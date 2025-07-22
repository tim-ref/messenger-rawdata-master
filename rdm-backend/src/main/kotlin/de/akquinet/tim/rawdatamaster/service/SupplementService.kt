/*
 * Copyright (C) 2023 akquinet GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package de.akquinet.tim.rawdatamaster.service

import de.akquinet.tim.rawdatamaster.model.persistence.ReportEntity
import de.akquinet.tim.rawdatamaster.repository.ReportRepository
import de.akquinet.tim.rawdatamaster.util.*
import jakarta.annotation.PostConstruct
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.TaskScheduler
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant

@Service
class SupplementService(
    @param:Value("\${rawdata-master.rawDataReport.deliveryRateMinutes}") private val deliveryRateMinutes: Int,
    @param:Value("\${rawdata-master.rawDataReport.autoSupplementOnStart}") private val autoSupplementOnStart: Boolean,
    private val reportService: ReportService,
    private val reportRepository: ReportRepository,
    private val taskScheduler: TaskScheduler
) {
    companion object {
        // According to A_22005, supplements have to be sent separately (not all at once).
        // Unfortunately, this requires the service to be stateful.
        private var redeliveryQueue = mutableListOf<ReportEntity>()

        private val log: Logger = LoggerFactory.getLogger(this::class.java)
    }

    private val sendSupplementTask = Runnable {
        log.debug("Current size of redelivery queue: {}", redeliveryQueue.size)

        if (redeliveryQueue.isEmpty()) return@Runnable

        redeliveryQueue.removeFirst().also {
            reportService.sendReport(it)
        }
    }

    @PostConstruct
    private fun postConstruct() = scheduleSendSupplementTask()

    internal fun scheduleSendSupplementTask() {
        if (deliveryRateMinutes > 0) {
            val now = Instant.now()
            val deliveryRate = deliveryRateMinutes.minutes()
            val nextPeriodStart = roundUpToNextPeriodStartSinceEpoch(now, deliveryRate)

            if (autoSupplementOnStart) {
                enqueueSupplements(
                    start = nextPeriodStart - 1.days().dividedBy(2),
                    end = nextPeriodStart - deliveryRate.multipliedBy(2),
                    all = false,
                    reportPeriod = deliveryRate
                )
            }

            // supplements have to be delivered delayed by half the delivery period
            taskScheduler.scheduleAtFixedRate(
                sendSupplementTask,
                nextPeriodStart + deliveryRate.dividedBy(2),
                deliveryRate
            )
        } else {
            log.info("Supplement scheduling disabled")
        }
    }

    internal fun getDeliveryRateMinutes() = deliveryRateMinutes

    internal fun getQueueSize() = redeliveryQueue.size

    internal fun clearQueue() {
        redeliveryQueue.clear()
    }

    // for testing purposes
    internal fun sendAllSupplements() {
        redeliveryQueue.run {
            forEach { reportService.sendReport(it) }
            clear()
        }
    }

    internal fun enqueueSupplements(
        start: Instant,
        end: Instant,
        all: Boolean,
        reportPeriod: Duration = deliveryRateMinutes.minutes()
    ): List<ReportEntity> {
        val startRounded = roundDownToPreviousPeriodEndSinceEpoch(start, reportPeriod)
        val endRounded = roundUpToNextPeriodStartSinceEpoch(end, reportPeriod)

        return reportRepository
            .findBetween(startRounded.toTimestamp(), endRounded.toTimestamp())
            .let {
                interpolateReportEntities(it, startRounded, endRounded, reportPeriod)
            }
            .filter { entity -> all || !entity.delivered }
            .also {
                redeliveryQueue = it.toMutableList()
            }
    }

    internal fun interpolateReportEntities(
        entities: List<ReportEntity>,
        start: Instant,
        end: Instant,
        period: Duration
    ): List<ReportEntity> {
        val interpolatedEntities = mutableListOf<ReportEntity>()

        var last = start

        entities.forEach { entity ->
            if (last.isBefore(entity.start.toInstant())) {
                interpolatedEntities.addAll(fillGap(last, entity.start.toInstant(), period))
            }
            interpolatedEntities.add(entity)
            last = entity.finish.toInstant()
        }

        if (last.isBefore(end)) {
            interpolatedEntities.addAll(fillGap(last, end, period))
        }

        return interpolatedEntities.toList()
    }

    internal fun fillGap(
        start: Instant,
        end: Instant,
        period: Duration
    ): List<ReportEntity> {
        val quotient = Duration.between(start, end).dividedBy(period)
        val remainder = Duration.between(start + period.multipliedBy(quotient), end)

        return (0 until quotient).map {
            ReportEntity(
                start = start.toTimestamp() + period.multipliedBy(it),
                finish = start.toTimestamp() + period.multipliedBy(it + 1)
            )
        } + if (remainder.isZero) {
            listOf()
        } else {
            listOf(
                ReportEntity(
                    start = start.toTimestamp() + period.multipliedBy(quotient),
                    finish = end.toTimestamp()
                )
            )
        }
    }
}
