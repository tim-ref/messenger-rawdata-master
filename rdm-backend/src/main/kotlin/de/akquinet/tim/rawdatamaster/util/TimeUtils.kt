/*
 * Copyright (C) 2023 akquinet GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package de.akquinet.tim.rawdatamaster.util

import java.sql.Timestamp
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

/**
 * [Timestamp] for JPA
 */

internal fun Instant.toTimestamp(): Timestamp = Timestamp.from(this)

internal operator fun Timestamp.plus(duration: Duration) =
    this.toInstant().plus(duration).toTimestamp()

internal operator fun Timestamp.minus(duration: Duration) =
    this.toInstant().minus(duration).toTimestamp()

/**
 * some convenience functions for [Instant]
 */

internal fun Instant.toUtcDateTime() = ZonedDateTime.from(this.atZone(ZoneOffset.UTC))

internal fun Instant.previousMonth(): Instant =
    this.toUtcDateTime().minusMonths(1).toInstant()

internal fun Instant.truncatedToSeconds(): Instant = this.truncatedTo(ChronoUnit.SECONDS)

internal fun Instant.truncatedToMinutes(): Instant = this.truncatedTo(ChronoUnit.MINUTES)

internal fun Instant.truncatedToMonths(): Instant =
    this.toUtcDateTime().truncatedTo(ChronoUnit.DAYS).withDayOfMonth(1).toInstant()

internal fun min(a: Instant, b: Instant) = if (a.isBefore(b)) a else b

internal fun roundUpToNextPeriodStartSinceEpoch(now: Instant, period: Duration): Instant {
    val elapsed = Duration.between(Instant.EPOCH, now)
    val quotient = elapsed.dividedBy(period)
    val remainder = Duration.between(Instant.EPOCH + period.multipliedBy(quotient), now)
    val numberOfPeriodsRoundedUp = quotient + if (remainder.isZero) 0 else 1
    return Instant.EPOCH + period.multipliedBy(numberOfPeriodsRoundedUp)
}

internal fun roundDownToPreviousPeriodEndSinceEpoch(now: Instant, period: Duration): Instant {
    val elapsed = Duration.between(Instant.EPOCH, now)
    val quotient = elapsed.dividedBy(period)
    return Instant.EPOCH + period.multipliedBy(quotient)
}

/**
 * [Duration] from [Int] and [Long]
 */

internal fun Long.days(): Duration = Duration.ofDays(this)

internal fun Int.days(): Duration = this.toLong().days()

internal fun Long.minutes(): Duration = Duration.ofMinutes(this)

internal fun Int.minutes(): Duration = this.toLong().minutes()

internal fun Long.millis(): Duration = Duration.ofMillis(this)

internal fun Int.millis(): Duration = this.toLong().millis()
