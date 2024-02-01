/*
 * Copyright (C) 2023 akquinet GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package de.akquinet.timref.rawdatamaster.service

import de.akquinet.timref.rawdatamaster.repository.RawDataRepository
import de.akquinet.timref.rawdatamaster.repository.RawStatisticsRepository
import de.akquinet.timref.rawdatamaster.repository.ReportRepository
import de.akquinet.timref.rawdatamaster.util.days
import de.akquinet.timref.rawdatamaster.util.minus
import de.akquinet.timref.rawdatamaster.util.toTimestamp
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class CleaningService(
    @param:Value("\${rawdata-master.cleanup.daysToLive}")
    private val daysToLive: Int,
    private val rawDataRepository: RawDataRepository,
    private val reportRepository: ReportRepository,
    private val rawStatisticsRepository: RawStatisticsRepository
) {
    companion object {
        private val log: Logger = LoggerFactory.getLogger(this::class.java)
    }

    @Scheduled(cron = "\${rawdata-master.cleanup.cron}", zone = "Europe/Berlin")
    private fun cleanRepositories() {
        val daysToLiveAgo = Instant.now().toTimestamp() - daysToLive.days()

        val deletedRawData = rawDataRepository.deleteBefore(daysToLiveAgo)
        val deletedReports = reportRepository.deleteBefore(daysToLiveAgo)
        val deletedRawStatistics = rawStatisticsRepository.deleteBefore(daysToLiveAgo)

        log.info(
            "Deleted all rows older than {} days from raw_data ({}), raw_data_report ({}), raw_statistics ({})",
            daysToLive,
            deletedRawData,
            deletedReports,
            deletedRawStatistics
        )
    }
}
