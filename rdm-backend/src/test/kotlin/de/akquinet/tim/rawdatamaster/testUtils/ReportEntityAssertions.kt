/*
 * Copyright (C) 2023 akquinet GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package de.akquinet.tim.rawdatamaster.testUtils

import de.akquinet.tim.rawdatamaster.model.persistence.ReportEntity
import de.akquinet.tim.rawdatamaster.util.toTimestamp
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import java.sql.Timestamp
import java.time.Duration
import java.time.Instant

internal fun ReportEntity.shouldBeDelivered(): ReportEntity =
    apply { delivered.shouldBeTrue() }

internal fun ReportEntity.shouldNotBeDelivered(): ReportEntity =
    apply { delivered.shouldBeFalse() }

internal fun ReportEntity.shouldHaveStart(start: Timestamp): ReportEntity =
    apply { this.start shouldBe start }

internal fun ReportEntity.shouldHaveStart(start: Instant): ReportEntity =
    shouldHaveStart(start.toTimestamp())

internal fun ReportEntity.shouldHaveFinish(finish: Timestamp): ReportEntity =
    apply { this.finish shouldBe finish }

internal fun ReportEntity.shouldHaveFinish(finish: Instant): ReportEntity =
    shouldHaveFinish(finish.toTimestamp())

internal fun ReportEntity.shouldSpan(duration: Duration): ReportEntity =
    apply { Duration.between(start.toInstant(), finish.toInstant()) shouldBe duration }
