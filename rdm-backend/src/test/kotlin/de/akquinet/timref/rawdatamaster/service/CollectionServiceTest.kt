/*
 * Copyright (C) 2023 akquinet GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package de.akquinet.timref.rawdatamaster.service

import de.akquinet.timref.rawdatamaster.model.RawDataOperation
import de.akquinet.timref.rawdatamaster.model.input.MatrixStatisticsData
import de.akquinet.timref.rawdatamaster.model.input.PerformanceData
import de.akquinet.timref.rawdatamaster.model.input.RawDataMessage
import de.akquinet.timref.rawdatamaster.model.input.StatisticsData
import de.akquinet.timref.rawdatamaster.model.persistence.RawDataEntity
import de.akquinet.timref.rawdatamaster.model.persistence.RawStatisticsEntity
import de.akquinet.timref.rawdatamaster.model.persistence.StatisticsEntity
import de.akquinet.timref.rawdatamaster.util.toTimestamp
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant

class CollectionServiceTest : DescribeSpec({
    describe("CollectionServiceTest") {
        val service = CollectionService(
            rawDataRepository = mockk(),
            statisticsRepository = mockk(),
            rawStatisticsRepository = mockk(),
            clock = mockk()
        )

        val anInstant = Instant.parse("2018-04-09T08:59:51Z")

        it("converts PerformanceData to RawDataEntity") {
            val inputMessage = RawDataMessage("Instanz_ID", "TestTypVersion", "TestVersion", "Ausprägung", "Plattform", "OS", "OS_Version", "client_id", "Matrix_Domain", 23, 45, "8234hs023", "doctor", "200")
            val somePerformanceData = PerformanceData(
                start = anInstant,
                durationInMs = 223_000,
                operation = RawDataOperation.RS_LOGIN.name,
                status = "status",
                message = inputMessage)

            val result = service.convertToRawDataEntity(somePerformanceData)

            val expectedRawDataNessage = Json.decodeFromString<RawDataMessage>("{  " +
                    "\"Inst-ID\" : \"Instanz_ID\", " +
                    "\"UA-PTV\" : \"TestTypVersion\", " +
                    "\"UA-PV\" : \"TestVersion\", " +
                    "\"UA-A\" : \"Ausprägung\", " +
                    "\"UA-P\" : \"Plattform\", " +
                    "\"UA-OS\" : \"OS\", " +
                    "\"UA-OS-VERSION\" : \"OS_Version\", " +
                    "\"UA-cid\" : \"client_id\", " +
                    "\"M-Dom\" : \"Matrix_Domain\", " +
                    "\"sizeIn\" : 23, " +
                    "\"sizeOut\" : 45, " +
                    "\"tID\" : \"8234hs023\", " +
                    "\"profOID\" : \"doctor\", " +
                    "\"Res\" : \"200\"" +
                "}")
            result shouldBe RawDataEntity(
                start = anInstant,
                durationInMs = 223_000,
                operation = RawDataOperation.RS_LOGIN,
                status = "status",
                message = Json.encodeToString(expectedRawDataNessage)
            ).copy(uuid = result.uuid)
        }

        it("converts StatisticsData to StatisticsEntity") {
            val someStatisticsData = StatisticsData(
                messengerServices = 2,
                registeredUsers = 100,
                activeUsers = 50
            )

            val result = service.convertToStatisticsEntity(
                statisticsData = someStatisticsData,
                referenceDate = anInstant
            )

            result shouldBe StatisticsEntity(
                referenceDate = anInstant.toTimestamp(),
                messengerServices = 2,
                registeredUsers = 100,
                activeUsers = 50
            )
        }

        it("converts StatisticsData to StatisticsEntity") {
            val someMatrixStatisticsData = MatrixStatisticsData(
                homeserver = "testserver.de",
                totalUsers = 100,
                monthlyActiveUsers = 50,
                timestamp = 1688528700000L
            )

            val result = service.convertToRawStatisticsEntity(
                matrixStatisticsData = someMatrixStatisticsData
            )

            result shouldBe RawStatisticsEntity(
                homeserver = "testserver.de",
                totalUsers = 100,
                monthlyActiveUsers = 50,
                timestamp = Instant.ofEpochSecond(1688528700000L).toTimestamp()
            )
        }
    }
})
