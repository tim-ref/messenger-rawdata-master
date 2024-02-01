/*
 * Copyright (C) 2023 akquinet GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package de.akquinet.timref.rawdatamaster.util

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import java.time.Duration
import java.time.Instant

class TimeUtilsTest : DescribeSpec({
    describe("TimeUtilsTest") {
        context("truncation of Instant to ChronoUnit") {
            it("truncates Instant to whole seconds") {
                val anInstant = Instant.parse("2018-04-09T08:59:51.999Z")
                val anInstantWholeSeconds = Instant.parse("2018-04-09T08:59:51.000Z")

                anInstant.truncatedToSeconds() shouldBe anInstantWholeSeconds
            }

            it("truncates Instant to whole minutes") {
                val anInstant = Instant.parse("2018-04-09T08:59:51Z")
                val anInstantWholeMinutes = Instant.parse("2018-04-09T08:59:00Z")

                anInstant.truncatedToMinutes() shouldBe anInstantWholeMinutes
            }

            it("truncates Instant to whole months") {
                val anInstant = Instant.parse("2018-04-09T08:59:51Z")
                val anInstantWholeMonths = Instant.parse("2018-04-01T00:00:00Z")

                anInstant.truncatedToMonths() shouldBe anInstantWholeMonths
            }
        }


        context("previous month of Instant") {
            it("calculates previous month of regular date") {
                val anInstant = Instant.parse("2018-04-09T08:59:51Z")
                val oneMonthAgo = Instant.parse("2018-03-09T08:59:51Z")

                anInstant.previousMonth() shouldBe oneMonthAgo
            }

            it("calculates previous month of month with 31 days") {
                val anInstant = Instant.parse("2023-07-31T00:00:00Z")
                val oneMonthAgo = Instant.parse("2023-06-30T00:00:00Z")

                anInstant.previousMonth() shouldBe oneMonthAgo
            }

            it("calculates previous month of March in leap year") {
                val anInstant = Instant.parse("2024-03-31T00:00:00Z")
                val oneMonthAgo = Instant.parse("2024-02-29T00:00:00Z")

                anInstant.previousMonth() shouldBe oneMonthAgo
            }
        }

        context("round to interval") {
            it("rounds nothing if elapsed time is integer multiple of period") {
                val now = Instant.parse("2019-09-23T09:00:00Z")
                val period = 5.minutes()

                roundUpToNextPeriodStartSinceEpoch(now, period) shouldBe now
                roundDownToPreviousPeriodEndSinceEpoch(now, period) shouldBe now
            }

            it("rounds to start (end) of next (previous) period") {
                val now = Instant.parse("2019-09-23T09:00:11Z")

                withClue("1 minute") {
                    val period = 1.minutes()

                    val nextStart = Instant.parse("2019-09-23T09:01:00Z")
                    roundUpToNextPeriodStartSinceEpoch(now, period) shouldBe nextStart

                    val previousEnd = Instant.parse("2019-09-23T09:00:00Z")
                    roundDownToPreviousPeriodEndSinceEpoch(now, period) shouldBe previousEnd
                }

                withClue("5 minutes") {
                    val period = 5.minutes()

                    val nextStart = Instant.parse("2019-09-23T09:05:00Z")
                    roundUpToNextPeriodStartSinceEpoch(now, period) shouldBe nextStart

                    val previousEnd = Instant.parse("2019-09-23T09:00:00Z")
                    roundDownToPreviousPeriodEndSinceEpoch(now, period) shouldBe previousEnd
                }

                withClue("10 minutes") {
                    val period = 10.minutes()

                    val nextStart = Instant.parse("2019-09-23T09:10:00Z")
                    roundUpToNextPeriodStartSinceEpoch(now, period) shouldBe nextStart

                    val previousEnd = Instant.parse("2019-09-23T09:00:00Z")
                    roundDownToPreviousPeriodEndSinceEpoch(now, period) shouldBe previousEnd
                }

                withClue("1 day") {
                    val period = 1.days()

                    val nextStart = Instant.parse("2019-09-24T00:00:00Z")
                    roundUpToNextPeriodStartSinceEpoch(now, period) shouldBe nextStart

                    val previousEnd = Instant.parse("2019-09-23T00:00:00Z")
                    roundDownToPreviousPeriodEndSinceEpoch(now, period) shouldBe previousEnd
                }
            }
        }

        it("calculates minimum of two Instants") {
            val anInstant = Instant.parse("2018-04-09T08:59:51Z")
            val later = anInstant + 1.days()

            min(anInstant, anInstant) shouldBe anInstant
            min(anInstant, later) shouldBe anInstant
        }

        it("creates Durations from Ints") {
            val oneDay = Duration.ofDays(1)

            oneDay shouldBe 1.days()
            oneDay shouldBe 1_440.minutes()
            oneDay shouldBe 86_400_000.millis()
        }
    }
})
