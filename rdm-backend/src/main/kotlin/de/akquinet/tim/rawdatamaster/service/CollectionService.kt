/*
 * Copyright (C) 2023 akquinet GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package de.akquinet.tim.rawdatamaster.service

import com.fasterxml.jackson.core.JsonProcessingException
import de.akquinet.tim.rawdatamaster.model.RawDataOperation
import de.akquinet.tim.rawdatamaster.model.input.MatrixStatisticsData
import de.akquinet.tim.rawdatamaster.model.input.PerformanceData
import de.akquinet.tim.rawdatamaster.model.input.StatisticsData
import de.akquinet.tim.rawdatamaster.model.persistence.RawDataEntity
import de.akquinet.tim.rawdatamaster.model.persistence.RawStatisticsEntity
import de.akquinet.tim.rawdatamaster.model.persistence.StatisticsEntity
import de.akquinet.tim.rawdatamaster.repository.RawDataRepository
import de.akquinet.tim.rawdatamaster.repository.RawStatisticsRepository
import de.akquinet.tim.rawdatamaster.repository.StatisticsRepository
import de.akquinet.tim.rawdatamaster.util.Clock
import de.akquinet.tim.rawdatamaster.util.defaultJsonMapper
import de.akquinet.tim.rawdatamaster.util.toTimestamp
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class CollectionService(
    private val rawDataRepository: RawDataRepository,
    private val statisticsRepository: StatisticsRepository,
    private val rawStatisticsRepository: RawStatisticsRepository,
    private val clock: Clock
) {
    companion object {
        private val log: Logger = LoggerFactory.getLogger(this::class.java)
    }

    // Rohdaten

    private fun sanitizeRawDataMessage(message: String) =
        message
            .replace(";", "")
            .replace("\n", "")
            .trim() // remove leading and trailing whitespace

    internal fun convertToRawDataEntity(performanceData: PerformanceData): RawDataEntity =
        performanceData.run {
            RawDataEntity(
                start = start!!,
                durationInMs = durationInMs!!,
                operation = RawDataOperation.valueOf(operation!!),
                status = status!!,
                message = sanitizeRawDataMessage(Json.encodeToString(message))
            )
        }

    internal fun addPerformanceData(performanceData: PerformanceData) {
        val rawDataEntity = convertToRawDataEntity(performanceData)
        rawDataRepository.save(rawDataEntity)
        if (log.isDebugEnabled) {
            try {
                log.debug("Raw data inserted: {}", defaultJsonMapper.writeValueAsString(rawDataEntity))
            } catch (e: JsonProcessingException) {
                log.error("", e)
            }
        }
    }

    // Bestandsdaten

    internal fun convertToStatisticsEntity(
        statisticsData: StatisticsData,
        referenceDate: Instant
    ): StatisticsEntity =
        statisticsData.run {
            StatisticsEntity(
                referenceDate = referenceDate.toTimestamp(),
                messengerServices = messengerServices,
                registeredUsers = registeredUsers,
                activeUsers = activeUsers
            )
        }

    internal fun addStatisticsData(statisticsData: StatisticsData) {
        val statisticsEntity = convertToStatisticsEntity(statisticsData, clock.now())
        statisticsRepository.save(statisticsEntity)
        if (log.isDebugEnabled) {
            try {
                log.debug("Statistics data inserted: {}", defaultJsonMapper.writeValueAsString(statisticsEntity))
            } catch (e: JsonProcessingException) {
                log.error("", e)
            }
        }
    }

    internal fun convertToRawStatisticsEntity(
        matrixStatisticsData: MatrixStatisticsData
    ): RawStatisticsEntity =
        matrixStatisticsData.run {
            RawStatisticsEntity(
                homeserver = homeserver!!,
                totalUsers = totalUsers,
                monthlyActiveUsers = monthlyActiveUsers,
                timestamp = Instant.ofEpochSecond(timestamp).toTimestamp()
            )
        }

    internal fun addRawStatistics(matrixStatisticsData: MatrixStatisticsData) {
        val rawStatisticsEntity = convertToRawStatisticsEntity(matrixStatisticsData)
        // this inserts a new line in db if homeserver name (primary key) is not present in db yet
        // otherwise the existing entry is overridden
        rawStatisticsRepository.save(rawStatisticsEntity)
        if (log.isDebugEnabled) {
            try {
                log.debug("Raw statistics data inserted: {}", defaultJsonMapper.writeValueAsString(rawStatisticsEntity))
            } catch (e: JsonProcessingException) {
                log.error("", e)
            }
        }
    }
}
