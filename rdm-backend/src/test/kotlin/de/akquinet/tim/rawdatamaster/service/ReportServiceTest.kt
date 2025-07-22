/*
 * Copyright (C) 2023 akquinet GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package de.akquinet.tim.rawdatamaster.service

import de.akquinet.tim.rawdatamaster.model.RawDataOperation
import de.akquinet.tim.rawdatamaster.model.persistence.RawDataEntity
import de.akquinet.tim.rawdatamaster.model.persistence.ReportEntity
import de.akquinet.tim.rawdatamaster.repository.RawDataRepository
import de.akquinet.tim.rawdatamaster.repository.ReportRepository
import de.akquinet.tim.rawdatamaster.testUtils.shouldBeDelivered
import de.akquinet.tim.rawdatamaster.testUtils.shouldHaveFinish
import de.akquinet.tim.rawdatamaster.testUtils.shouldHaveStart
import de.akquinet.tim.rawdatamaster.testUtils.shouldNotBeDelivered
import de.akquinet.tim.rawdatamaster.util.minutes
import de.akquinet.tim.rawdatamaster.util.toTimestamp
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import org.springframework.scheduling.TaskScheduler
import java.io.ByteArrayOutputStream
import java.io.PrintWriter
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.time.Instant


class ReportServiceTest : DescribeSpec({
    describe("ReportServiceTest") {

        val rawDataRepositoryMock: RawDataRepository = mockk()
        val reportRepositoryMock: ReportRepository = mockk()
        val httpDeliveryServiceMock: HttpDeliveryService = mockk()
        val taskSchedulerMock: TaskScheduler = mockk()

        val service = ReportService(
            instanceId = "CI-999999",
            deliveryRateMinutes = 5,
            rawDataRepository = rawDataRepositoryMock,
            reportRepository = reportRepositoryMock,
            httpDeliveryService = httpDeliveryServiceMock,
            taskScheduler = taskSchedulerMock
        )

        afterEach {
            clearAllMocks()
        }

        context("convenience methods") {
            it("creates a report name from components") {
                val start = Instant.parse("2018-04-09T08:55:00Z")
                val end = Instant.parse("2018-04-09T09:00:00Z")

                val reportName = service.createReportName(start, end)
                reportName shouldBe "CI-999999_1523264100000_1523264400000_perf.log"
            }

            it("converts RawDataEntity to formatted String as specified") {
                val rawDataEntity = RawDataEntity(
                    start = Instant.parse("2018-04-09T08:59:51Z"),
                    durationInMs = 223_000,
                    operation = RawDataOperation.RS_LOGIN,
                    status = "status",
                    message = "message"
                )

                val outputStream = ByteArrayOutputStream()

                PrintWriter(outputStream).use {
                    service.formatRawDataEntity(rawDataEntity, it)
                }

                outputStream.toString(StandardCharsets.UTF_8) shouldBe
                        "1523264391000;223000;TIM.UC_10060_01;status;message\r\n"
            }

            it("converts multiple RawDataEntity to formatted String as specified") {
                val rawDataEntity1 = RawDataEntity(
                    start = Instant.parse("2018-04-09T08:59:51Z"),
                    durationInMs = 123_000,
                    operation = RawDataOperation.RS_LOGIN,
                    status = "status1",
                    message = "message1"
                )

                val rawDataEntity2 = RawDataEntity(
                    start = Instant.parse("2018-04-09T09:59:51Z"),
                    durationInMs = 223_000,
                    operation = RawDataOperation.RS_LOGIN,
                    status = "status2",
                    message = "message2"
                )

                val outputStream = ByteArrayOutputStream()

                service.formatReport(listOf(rawDataEntity1, rawDataEntity2), outputStream)

                val result = outputStream.toString(StandardCharsets.UTF_8)

                result shouldBe
                        "1523264391000;123000;TIM.UC_10060_01;status1;message1\r\n" +
                        "1523267991000;223000;TIM.UC_10060_01;status2;message2\r\n"
            }

            it("converts empty list to formatted String as specified") {
                val outputStream = ByteArrayOutputStream()
                service.formatReport(listOf(), outputStream)

                val result = outputStream.toString(StandardCharsets.UTF_8)
                result shouldBe "leer"
            }
        }

        context("scheduling reports") {
            it("automatically schedules reports") {
                every { taskSchedulerMock.scheduleAtFixedRate(any(), any(), any<Duration>()) } returns mockk()

                service.scheduleSendReportTask()

                verify(exactly = 1) {
                    taskSchedulerMock.scheduleAtFixedRate(any(), any(), 5.minutes())
                }
            }

            it("does not schedule reports when disabled") {
                val disabledService = ReportService(
                    instanceId = "CI-999999",
                    deliveryRateMinutes = 0, // disabled
                    rawDataRepository = rawDataRepositoryMock,
                    reportRepository = reportRepositoryMock,
                    httpDeliveryService = httpDeliveryServiceMock,
                    taskScheduler = taskSchedulerMock
                )

                disabledService.scheduleSendReportTask()

                verify(exactly = 0) {
                    taskSchedulerMock.scheduleAtFixedRate(any(), any(), any<Duration>())
                }
            }
        }

        context("sending reports") {
            it("successfully sends a report") {
                val rawDataEntity = RawDataEntity(
                    start = Instant.parse("2018-04-09T08:59:51Z"),
                    durationInMs = 223_000,
                    operation = RawDataOperation.RS_LOGIN,
                    status = "status",
                    message = "message"
                )

                val referenceStart = Instant.parse("2018-04-09T09:00:12Z")
                val reportPeriod = 5.minutes()
                val referenceEnd = referenceStart + reportPeriod

                val expectedStart = Instant.parse("2018-04-09T09:00:00Z")
                val expectedEnd = Instant.parse("2018-04-09T09:05:00Z")
                val expectedFileName = service.createReportName(expectedStart, expectedEnd)

                every {
                    rawDataRepositoryMock.findBetween(expectedStart.toTimestamp(), expectedEnd.toTimestamp())
                } returns listOf(rawDataEntity)

                every {
                    httpDeliveryServiceMock.sendRawData(expectedFileName, any(), more(0L))
                } returns true

                val savedReport = slot<ReportEntity>()

                every {
                    reportRepositoryMock.save(capture(savedReport))
                } returns mockk()

                service.sendReport(referenceStart, referenceEnd)

                savedReport.captured
                    .shouldHaveStart(expectedStart)
                    .shouldHaveFinish(expectedEnd)
                    .shouldBeDelivered()
            }

            it("logs a failed report") {
                val rawDataEntity = RawDataEntity(
                    start = Instant.parse("2018-04-09T08:59:51Z"),
                    durationInMs = 223_000,
                    operation = RawDataOperation.RS_LOGIN,
                    status = "status",
                    message = "message"
                )

                val referenceStart = Instant.parse("2018-04-09T09:00:12Z")
                val reportPeriod = 5.minutes()
                val referenceEnd = referenceStart + reportPeriod

                val expectedStart = Instant.parse("2018-04-09T09:00:00Z")
                val expectedEnd = Instant.parse("2018-04-09T09:05:00Z")
                val expectedFileName = service.createReportName(expectedStart, expectedEnd)

                every {
                    rawDataRepositoryMock.findBetween(expectedStart.toTimestamp(), expectedEnd.toTimestamp())
                } returns listOf(rawDataEntity)

                every {
                    httpDeliveryServiceMock.sendRawData(expectedFileName, any(), any())
                } returns false

                val savedReport = slot<ReportEntity>()

                every {
                    reportRepositoryMock.save(capture(savedReport))
                } returns mockk()

                service.sendReport(referenceStart, referenceEnd)

                savedReport.captured
                    .shouldHaveStart(expectedStart)
                    .shouldHaveFinish(expectedEnd)
                    .shouldNotBeDelivered()
            }

            it("successfully sends an empty report") {
                val referenceStart = Instant.parse("2018-04-09T09:00:12Z")
                val reportPeriod = 5.minutes()
                val referenceEnd = referenceStart + reportPeriod

                val expectedStart = Instant.parse("2018-04-09T09:00:00Z")
                val expectedEnd = Instant.parse("2018-04-09T09:05:00Z")
                val expectedFileName = service.createReportName(expectedStart, expectedEnd)

                every {
                    rawDataRepositoryMock.findBetween(expectedStart.toTimestamp(), expectedEnd.toTimestamp())
                } returns listOf() // empty

                every {
                    httpDeliveryServiceMock.sendRawData(expectedFileName, any(), more(0L))
                } returns true

                val savedReport = slot<ReportEntity>()

                every {
                    reportRepositoryMock.save(capture(savedReport))
                } returns mockk()

                service.sendReport(referenceStart, referenceEnd)

                savedReport.captured
                    .shouldHaveStart(expectedStart)
                    .shouldHaveFinish(expectedEnd)
                    .shouldBeDelivered()
            }
        }
    }
})
