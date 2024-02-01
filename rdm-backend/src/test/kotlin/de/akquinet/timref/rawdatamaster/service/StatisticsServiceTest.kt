/*
 * Copyright (C) 2023 akquinet GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package de.akquinet.timref.rawdatamaster.service

import de.akquinet.timref.rawdatamaster.model.input.StatisticsData
import de.akquinet.timref.rawdatamaster.model.output.EnrichedStatistics
import de.akquinet.timref.rawdatamaster.model.persistence.RawStatisticsEntity
import de.akquinet.timref.rawdatamaster.repository.RawStatisticsRepository
import de.akquinet.timref.rawdatamaster.util.toUtcDateTime
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import java.time.Instant

class StatisticsServiceTest : DescribeSpec({
    describe("StatisticsServiceTest") {
        context("send statistics to gematik") {
            val now = Instant.parse("2018-04-09T08:59:51Z")

            it("does not send anything if there is no latest StatisticsEntity") {
                val rawStatisticsRepository: RawStatisticsRepository = mockk {
                    every { findByTimestampAfter(any()) } returns emptyList()
                }

                val httpDeliveryServiceMock: HttpDeliveryService = mockk {
                    every { sendStatistics(any(), any()) } returns true
                }

                val service = StatisticsService(
                    instanceId = "CI-999999",
                    collectionService = mockk(),
                    rawStatisticsRepository = rawStatisticsRepository,
                    httpDeliveryService = httpDeliveryServiceMock
                )

                service.sendStatistics(now)

                verify(exactly = 0) {
                    httpDeliveryServiceMock.sendStatistics(any(), any())
                }
            }

            it("sends latest statistics") {
                val someStatisticsEntity = RawStatisticsEntity()

                val rawStatisticsRepository: RawStatisticsRepository = mockk {
                    every { findByTimestampAfter(any()) } returns listOf(someStatisticsEntity)
                }

                val httpDeliveryServiceMock: HttpDeliveryService = mockk {
                    every { sendStatistics(any(), any()) } returns true
                }

                val collectionService: CollectionService = mockk {
                    justRun { addStatisticsData(any()) }
                }

                val statisticsData = StatisticsData(1, 0, 0)

                val service = StatisticsService(
                    instanceId = "CI-999999",
                    collectionService = collectionService,
                    rawStatisticsRepository = rawStatisticsRepository,
                    httpDeliveryService = httpDeliveryServiceMock
                )

                val nowWholeSeconds = Instant.parse("2018-04-09T08:59:51Z")
                val nowWholeMonths = Instant.parse("2018-04-01T00:00:00Z")
                val previousMonth = Instant.parse("2018-03-01T00:00:00Z")

                val statisticsJson = service.getStatisticsJson(statisticsData, nowWholeSeconds)
                val reportFileName = service.createReportName(previousMonth, nowWholeMonths)

                service.sendStatistics(now)

                verify {
                    httpDeliveryServiceMock.sendStatistics(
                        statisticsJson = statisticsJson,
                        reportFileName = reportFileName
                    )
                }
            }
        }

        context("convenience methods") {
            val service = StatisticsService(
                instanceId = "CI-999999",
                collectionService = mockk(),
                rawStatisticsRepository = mockk(),
                httpDeliveryService = mockk()
            )

            it("creates a report name from components") {
                val start = Instant.parse("2018-04-01T00:00:00Z")
                val end = Instant.parse("2018-05-01T00:00:00Z")

                val reportName = service.createReportName(start, end)
                reportName shouldBe "CI-999999_1522540800000_1525132800000_stats.json"
            }

            it("converts StatisticsEntity to EnrichedStatistics with given request time") {
                val someStatisticsData = StatisticsData(
                    messengerServices = 12,
                    registeredUsers = 3456,
                    activeUsers = 789
                )

                val someRequestTime = Instant.parse("2018-04-09T08:59:51Z")

                val result = service.convertToEnrichedStatistics(someStatisticsData, someRequestTime)

                result shouldBe EnrichedStatistics(
                    requestTime = someRequestTime.toUtcDateTime(),
                    instanceId = "CI-999999",
                    messengerServices = 12,
                    registeredUsers = 3456,
                    activeUsers = 789
                )
            }

            it("writes StatisticsEntity as JSON") {
                val someStatisticsData = StatisticsData(
                    messengerServices = 12,
                    registeredUsers = 3456,
                    activeUsers = 789
                )

                val someRequestTime = Instant.parse("2018-04-09T08:59:51Z")

                val result = service.getStatisticsJson(someStatisticsData, someRequestTime)

                result shouldBe """
                    {
                      "Abfragezeitpunkt" : "2018-04-09T08:59:51.000Z",
                      "CI_ID" : "CI-999999",
                      "TIM-FD_Anzahl_Messenger-Service" : 12,
                      "TIM-FD_Anzahl_Nutzer" : 3456,
                      "TIM-FD_Anzahl_aktNutzer" : 789
                    }
                """.trimIndent()
            }
        }
    }
})
