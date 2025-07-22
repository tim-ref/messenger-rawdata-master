/*
 * Copyright (C) 2023 akquinet GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package de.akquinet.tim.rawdatamaster.service

import de.akquinet.tim.rawdatamaster.model.persistence.ReportEntity
import de.akquinet.tim.rawdatamaster.repository.ReportRepository
import de.akquinet.tim.rawdatamaster.testUtils.shouldHaveFinish
import de.akquinet.tim.rawdatamaster.testUtils.shouldHaveStart
import de.akquinet.tim.rawdatamaster.testUtils.shouldNotBeDelivered
import de.akquinet.tim.rawdatamaster.testUtils.shouldSpan
import de.akquinet.tim.rawdatamaster.util.minutes
import de.akquinet.tim.rawdatamaster.util.roundDownToPreviousPeriodEndSinceEpoch
import de.akquinet.tim.rawdatamaster.util.toTimestamp
import de.akquinet.tim.rawdatamaster.util.truncatedToMinutes
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.scheduling.TaskScheduler
import java.time.Duration
import java.time.Instant
import java.util.*

private fun reportEntity(start: Instant, end: Instant, success: Boolean = true) =
    ReportEntity(UUID.randomUUID(), start, end, success)

class SupplementServiceTest : DescribeSpec({
    describe("SupplementServiceTest") {
        val now = Instant.now()

        context("scheduling supplements") {
            it("automatically schedules reports") {
                val taskSchedulerMock: TaskScheduler = mockk {
                    every { scheduleAtFixedRate(any(), any(), any<Duration>()) } returns mockk()
                }

                val service = SupplementService(
                    deliveryRateMinutes = 5,
                    autoSupplementOnStart = false,
                    reportService = mockk(),
                    reportRepository = mockk(),
                    taskScheduler = taskSchedulerMock
                )

                service.scheduleSendSupplementTask()

                verify(exactly = 1) {
                    taskSchedulerMock.scheduleAtFixedRate(any(), any(), 5.minutes())
                }
            }

            it("does not schedule reports when disabled") {
                val taskSchedulerMock: TaskScheduler = mockk()

                val service = SupplementService(
                    deliveryRateMinutes = 0, // disabled
                    autoSupplementOnStart = false,
                    reportService = mockk(),
                    reportRepository = mockk(),
                    taskScheduler = taskSchedulerMock
                )

                service.scheduleSendSupplementTask()

                verify(exactly = 0) {
                    taskSchedulerMock.scheduleAtFixedRate(any(), any(), any<Duration>())
                }
            }
        }

        context("enqueue supplements") {
            val period = 5.minutes()
            val lookupStart = roundDownToPreviousPeriodEndSinceEpoch(now, period)
            val lookupEnd = lookupStart + 20.minutes()

            val reportRepositoryMock: ReportRepository = mockk {
                every {
                    findBetween(lookupStart.toTimestamp(), lookupEnd.toTimestamp())
                } returns listOf(
                    reportEntity(lookupStart, lookupStart + 5.minutes(), true),
                    reportEntity(lookupStart + 5.minutes(), lookupStart + 10.minutes(), false),
                    reportEntity(lookupStart + 15.minutes(), lookupStart + 20.minutes(), true)
                )
            }

            val service = SupplementService(
                deliveryRateMinutes = 5,
                autoSupplementOnStart = false,
                reportService = mockk(),
                reportRepository = reportRepositoryMock,
                taskScheduler = mockk()
            )

            it("enqueues missing and non-delivered supplements only") {
                val result = service.enqueueSupplements(
                    start = lookupStart,
                    end = lookupEnd,
                    all = false,
                    reportPeriod = period
                )

                result shouldHaveSize 2
            }

            it("enqueues ALL supplements if requested") {
                val result = service.enqueueSupplements(
                    start = lookupStart,
                    end = lookupEnd,
                    all = true,
                    reportPeriod = period
                )

                result shouldHaveSize 4
            }
        }

        context("interpolation and filling gaps") {
            val service = SupplementService(
                deliveryRateMinutes = 5,
                autoSupplementOnStart = false,
                reportService = mockk(),
                reportRepository = mockk(),
                taskScheduler = mockk()
            )

            context("interpolation of missing ReportEntities") {
                it("interpolates nothing if lookup interval is covered perfectly without gaps") {
                    val lookupStart = now.truncatedToMinutes()
                    val lookupEnd = lookupStart + 2.minutes()
                    val period = 1.minutes()

                    val entities = listOf(
                        reportEntity(lookupStart, lookupStart + period),
                        reportEntity(lookupStart + period, lookupEnd)
                    )

                    val result = service.interpolateReportEntities(entities, lookupStart, lookupEnd, period)

                    result shouldBe entities
                }

                it("does not interpolate at the beginning if lookup start lies within the first entity") {
                    val lookupStart = now.truncatedToMinutes()
                    val lookupEnd = lookupStart + 1.minutes()
                    val period = 2.minutes()

                    val entities = listOf(
                        reportEntity(lookupStart - 1.minutes(), lookupStart + 1.minutes())
                    )

                    val result = service.interpolateReportEntities(entities, lookupStart, lookupEnd, period)

                    result shouldBe entities
                }

                it("does not interpolate at the end if lookup end lies within the last entity") {
                    val lookupStart = now.truncatedToMinutes()
                    val lookupEnd = lookupStart + 1.minutes()
                    val period = 2.minutes()

                    val entities = listOf(
                        reportEntity(lookupStart, lookupStart + 2.minutes())
                    )

                    val result = service.interpolateReportEntities(entities, lookupStart, lookupEnd, period)

                    result shouldBe entities
                }

                it("interpolates gap at the beginning") {
                    val lookupStart = now.truncatedToMinutes()
                    val lookupEnd = lookupStart + 5.minutes()
                    val period = 2.minutes()

                    val entity = reportEntity(lookupStart + 3.minutes(), lookupEnd)
                    val entities = listOf(entity)

                    val result = service.interpolateReportEntities(entities, lookupStart, lookupEnd, period)

                    result shouldHaveSize 3

                    result[0]
                        .shouldNotBeDelivered()
                        .shouldHaveStart(lookupStart)
                        .shouldSpan(period)

                    result[1]
                        .shouldNotBeDelivered()
                        .shouldHaveStart(result[0].finish)
                        .shouldSpan(1.minutes())

                    result[2] shouldBe entity
                }

                it("interpolates gap at the end") {
                    val lookupStart = now.truncatedToMinutes()
                    val lookupEnd = lookupStart + 5.minutes()
                    val period = 2.minutes()

                    val entity = reportEntity(lookupStart, lookupStart + 2.minutes())
                    val entities = listOf(entity)

                    val result = service.interpolateReportEntities(entities, lookupStart, lookupEnd, period)

                    result shouldHaveSize 3

                    result[0] shouldBe entity

                    result[1]
                        .shouldNotBeDelivered()
                        .shouldHaveStart(entity.finish)
                        .shouldSpan(period)

                    result[2]
                        .shouldNotBeDelivered()
                        .shouldHaveStart(result[1].finish)
                        .shouldHaveFinish(lookupEnd)
                        .shouldSpan(1.minutes())
                }

                it("interpolates gap between entities") {
                    val lookupStart = now.truncatedToMinutes()
                    val lookupEnd = lookupStart + 7.minutes()
                    val period = 2.minutes()

                    val entity1 = reportEntity(lookupStart, lookupStart + 2.minutes())
                    val entity2 = reportEntity(lookupEnd - 2.minutes(), lookupEnd)
                    val entities = listOf(entity1, entity2)

                    val result = service.interpolateReportEntities(entities, lookupStart, lookupEnd, period)

                    result shouldHaveSize 4

                    result[0] shouldBe entity1

                    result[1]
                        .shouldHaveStart(entity1.finish)
                        .shouldSpan(period)
                        .shouldNotBeDelivered()

                    result[2]
                        .shouldHaveStart(result[1].finish)
                        .shouldSpan(1.minutes())
                        .shouldNotBeDelivered()

                    result[3] shouldBe entity2
                }

                it("interpolates EVERYTHING") {
                    val lookupStart = now.truncatedToMinutes()
                    val lookupEnd = lookupStart + 15.minutes()
                    val period = 2.minutes()

                    val entities = listOf(
                        reportEntity(lookupStart + 3.minutes(), lookupStart + 4.minutes()),
                        reportEntity(lookupStart + 4.minutes(), lookupStart + 5.minutes()),
                        reportEntity(lookupStart + 8.minutes(), lookupStart + 10.minutes()),
                        reportEntity(lookupStart + 10.minutes(), lookupStart + 12.minutes()),
                    )

                    val result = service.interpolateReportEntities(entities, lookupStart, lookupEnd, period)

                    result shouldHaveSize entities.size + 2 + 2 + 2 // = 10

                    result[0]
                        .shouldNotBeDelivered()
                        .shouldHaveStart(lookupStart)
                        .shouldSpan(period)

                    result[1]
                        .shouldNotBeDelivered()
                        .shouldHaveStart(result[0].finish)
                        .shouldSpan(1.minutes())

                    result[2] shouldBe entities[0]
                    result[3] shouldBe entities[1]

                    result[4]
                        .shouldNotBeDelivered()
                        .shouldHaveStart(result[3].finish)
                        .shouldSpan(period)

                    result[5]
                        .shouldNotBeDelivered()
                        .shouldHaveStart(result[4].finish)
                        .shouldSpan(1.minutes())

                    result[6] shouldBe entities[2]
                    result[7] shouldBe entities[3]

                    result[8]
                        .shouldNotBeDelivered()
                        .shouldHaveStart(result[7].finish)
                        .shouldSpan(period)

                    result[9]
                        .shouldNotBeDelivered()
                        .shouldHaveStart(result[8].finish)
                        .shouldSpan(1.minutes())
                }

                it("interpolates NOTHING (no entities in lookup interval)") {
                    val lookupStart = now.truncatedToMinutes()
                    val lookupEnd = lookupStart + 5.minutes()
                    val period = 2.minutes()

                    val entities = listOf<ReportEntity>()

                    val result = service.interpolateReportEntities(entities, lookupStart, lookupEnd, period)

                    result shouldHaveSize 3

                    result[0]
                        .shouldNotBeDelivered()
                        .shouldHaveStart(lookupStart)
                        .shouldSpan(period)

                    result[1]
                        .shouldNotBeDelivered()
                        .shouldHaveStart(result[0].finish)
                        .shouldSpan(period)

                    result[2]
                        .shouldNotBeDelivered()
                        .shouldHaveStart(result[1].finish)
                        .shouldHaveFinish(lookupEnd)
                        .shouldSpan(1.minutes())
                }
            }

            context("filling all the gaps") {
                it("does not fill a gap of zero width") {
                    val start = now.truncatedToMinutes()
                    val period = 1.minutes()

                    service.fillGap(start, start, period).shouldBeEmpty()
                }

                it("fills gap longer than period, no remainder") {
                    val start = now.truncatedToMinutes()
                    val end = start + 3.minutes()
                    val period = 1.minutes()

                    val result = service.fillGap(start, end, period)

                    result shouldHaveSize 3

                    result[0]
                        .shouldNotBeDelivered()
                        .shouldHaveStart(start)
                        .shouldSpan(period)

                    result[1]
                        .shouldNotBeDelivered()
                        .shouldHaveStart(result[0].finish)
                        .shouldSpan(period)

                    result[2]
                        .shouldNotBeDelivered()
                        .shouldHaveStart(result[1].finish)
                        .shouldHaveFinish(end)
                        .shouldSpan(period)
                }

                it("fills gap longer than period, with remainder") {
                    val start = now.truncatedToMinutes()
                    val end = start + 5.minutes()
                    val period = 2.minutes()

                    val result = service.fillGap(start, end, period)

                    result shouldHaveSize 3

                    result[0]
                        .shouldNotBeDelivered()
                        .shouldHaveStart(start)
                        .shouldSpan(period)

                    result[1]
                        .shouldNotBeDelivered()
                        .shouldHaveStart(result[0].finish)
                        .shouldSpan(period)

                    result[2]
                        .shouldNotBeDelivered()
                        .shouldHaveStart(result[1].finish)
                        .shouldHaveFinish(end)
                        .shouldSpan(1.minutes())
                }

                it("fills gap shorter than period") {
                    val start = now.truncatedToMinutes()
                    val end = start + 1.minutes()
                    val period = 2.minutes()

                    val result = service.fillGap(start, end, period)

                    result shouldHaveSize 1

                    result.single()
                        .shouldNotBeDelivered()
                        .shouldHaveStart(start)
                        .shouldHaveFinish(end)
                        .shouldSpan(1.minutes())
                }
            }
        }
    }
})
