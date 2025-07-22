/*
 * Copyright (C) 2023 akquinet GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package de.akquinet.tim.rawdatamaster.service

import de.akquinet.tim.rawdatamaster.model.input.StatisticsData
import de.akquinet.tim.rawdatamaster.model.output.EnrichedStatistics
import de.akquinet.tim.rawdatamaster.repository.RawStatisticsRepository
import de.akquinet.tim.rawdatamaster.util.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@Service
class StatisticsService(
    @param:Value("\${InstanceID}")
    private val instanceId: String,
    private val collectionService: CollectionService,
    private val rawStatisticsRepository: RawStatisticsRepository,
    private val httpDeliveryService: HttpDeliveryService
) {
    companion object {
        private const val STATISTICS_FILENAME_PATTERN = "%s_%d_%d_stats.json"
    }

    @Scheduled(cron = "\${rawdata-master.statistics.cron}", zone = "Europe/Berlin")
    private fun scheduleStatistics() {
        sendStatistics(Instant.now())
    }

    internal fun sendStatistics(now: Instant) {
        val nowWholeSeconds = now.truncatedToSeconds()
        val nowWholeMonths = now.truncatedToMonths()

        val statisticsData = aggregateRawStatistics() ?: return

        // add new entry to know which statistics were send
        collectionService.addStatisticsData(statisticsData)

        httpDeliveryService.sendStatistics(
            statisticsJson = getStatisticsJson(statisticsData, nowWholeSeconds),
            reportFileName = createReportName(nowWholeMonths.previousMonth(), nowWholeMonths)
        )
    }

    internal fun getStatisticsJson(
        statisticsData: StatisticsData,
        requestTime: Instant
    ): String =
        convertToEnrichedStatistics(statisticsData, requestTime).let {
            defaultJsonMapper.writeValueAsString(it)
        }

    internal fun convertToEnrichedStatistics(
        statisticsData: StatisticsData,
        requestTime: Instant
    ): EnrichedStatistics =
        EnrichedStatistics(
            requestTime = requestTime.toUtcDateTime(),
            instanceId = instanceId,
            messengerServices = statisticsData.messengerServices,
            registeredUsers = statisticsData.registeredUsers,
            activeUsers = statisticsData.activeUsers
        )

    internal fun createReportName(start: Instant, end: Instant) =
        STATISTICS_FILENAME_PATTERN.format(
            instanceId,
            start.toEpochMilli(),
            end.toEpochMilli()
        )

    private fun aggregateRawStatistics(): StatisticsData? {
        // use LocalDateTime here because Instant.toTimestamp() in comparison to Timestamp.valueOf(LocalDateTime)
        // produces 02:00 instead of 00:00
        val yesterday = LocalDateTime.now().minusDays(1).truncatedTo(ChronoUnit.DAYS)

        // only the entries from yesterday are aggregated and then send to gematik
        // this method is executed on the first of a month at 00:00 (timezone Europe/Berlin), there should be no raw statistics in the db for a time after this moment
        // resulting from this just looking for raw data after yesterday 00:00 is sufficient
        val rawStatistics = rawStatisticsRepository.findByTimestampAfter(Timestamp.valueOf(yesterday)).also {
            if (it.isEmpty()) {
                return null
            }
        }

        val statisticsData = StatisticsData(0, 0, 0)

        rawStatistics.forEach {
            statisticsData.messengerServices += 1
            statisticsData.activeUsers += it.monthlyActiveUsers
            statisticsData.registeredUsers += it.totalUsers
        }

        return statisticsData
    }
}
