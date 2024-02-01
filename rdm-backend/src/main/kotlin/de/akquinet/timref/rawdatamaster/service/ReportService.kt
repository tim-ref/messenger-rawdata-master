/*
 * Copyright (C) 2023 akquinet GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package de.akquinet.timref.rawdatamaster.service

import de.akquinet.timref.rawdatamaster.model.persistence.RawDataEntity
import de.akquinet.timref.rawdatamaster.model.persistence.ReportEntity
import de.akquinet.timref.rawdatamaster.repository.RawDataRepository
import de.akquinet.timref.rawdatamaster.repository.ReportRepository
import de.akquinet.timref.rawdatamaster.util.minutes
import de.akquinet.timref.rawdatamaster.util.roundUpToNextPeriodStartSinceEpoch
import de.akquinet.timref.rawdatamaster.util.toTimestamp
import de.akquinet.timref.rawdatamaster.util.truncatedToMinutes
import jakarta.annotation.PostConstruct
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.TaskScheduler
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.io.PrintWriter
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.*

@Service
class ReportService(
    @param:Value("\${InstanceID}")
    private val instanceId: String,
    @param:Value("\${rawdata-master.rawDataReport.deliveryRateMinutes}")
    private val deliveryRateMinutes: Int,
    private val rawDataRepository: RawDataRepository,
    private val reportRepository: ReportRepository,
    private val httpDeliveryService: HttpDeliveryService,
    private val taskScheduler: TaskScheduler
) {
    companion object {
        private const val REPORT_FILENAME_PATTERN = "%s_%d_%d_perf.log"
        private const val RAW_DATA_LINE_FORMAT = "%d;%d;%s;%s;%s\r\n"
        private const val RAW_DATA_EMPTY_REPORT = "leer"

        private val log: Logger = LoggerFactory.getLogger(this::class.java)
    }

    private val sendReportTask = Runnable {
        val now = Instant.now()
        val deliveryRate = deliveryRateMinutes.minutes()
        sendReport(now - deliveryRate.multipliedBy(2), now - deliveryRate)
    }

    @PostConstruct
    private fun postConstruct() = scheduleSendReportTask()

    internal fun scheduleSendReportTask() {
        if (deliveryRateMinutes > 0) {
            val now = Instant.now()
            val deliveryRate = deliveryRateMinutes.minutes()
            val nextPeriodStart = roundUpToNextPeriodStartSinceEpoch(now, deliveryRate)

            taskScheduler.scheduleAtFixedRate(sendReportTask, nextPeriodStart, deliveryRate)
        } else {
            log.info("Report scheduling disabled")
        }
    }

    internal fun sendReport(
        startInclusive: Instant,
        endExclusive: Instant,
        // possibility to pass a UUID in order to overwrite an existing entity (Nachlieferung)
        uuid: UUID = UUID.randomUUID()
    ) {
        log.debug("Sending report from {} to {}", startInclusive, endExclusive)

        val startWholeMinutes = startInclusive.truncatedToMinutes()
        val endWholeMinutes = endExclusive.truncatedToMinutes()

        // TODO Streaming
        val rawDataEntities = rawDataRepository.findBetween(
            startInclusive = startWholeMinutes.toTimestamp(),
            endExclusive = endWholeMinutes.toTimestamp()
        )

        val reportBytes = ByteArrayOutputStream().use {
            formatReport(rawDataEntities, it)
            it
        }.toByteArray()

        val deliverySuccess = httpDeliveryService.sendRawData(
            reportFileName = createReportName(startWholeMinutes, endWholeMinutes),
            inputStream = reportBytes.inputStream(),
            contentLength = reportBytes.size.toLong()
        )

        // TODO auch im Fehlerfall schreiben?
        ReportEntity(
            uuid = uuid,
            start = startWholeMinutes,
            finish = endWholeMinutes,
            delivered = deliverySuccess
        ).also {
            reportRepository.save(it)
        }
    }

    // convenience overload
    internal fun sendReport(reportEntity: ReportEntity) =
        reportEntity.also {
            sendReport(it.start.toInstant(), it.finish.toInstant(), it.uuid)
        }

    internal fun formatReport(entities: List<RawDataEntity>, outputStream: OutputStream) {
        PrintWriter(outputStream, false, StandardCharsets.UTF_8).use { out ->
            if (entities.isEmpty()) {
                out.print(RAW_DATA_EMPTY_REPORT)
            } else {
                entities.forEach {
                    formatRawDataEntity(it, out)
                }
            }
        }
    }

    internal fun formatRawDataEntity(rawDataEntity: RawDataEntity, printWriter: PrintWriter) =
        rawDataEntity.run {
            printWriter.format(
                RAW_DATA_LINE_FORMAT,
                start.toInstant().toEpochMilli(),
                durationInMs,
                operation.value,
                status,
                message
            )
        }

    internal fun createReportName(start: Instant, end: Instant) =
        REPORT_FILENAME_PATTERN.format(
            instanceId,
            start.toEpochMilli(),
            end.toEpochMilli()
        )
}
