/*
 * Copyright (C) 2023 akquinet GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package de.akquinet.tim.rawdatamaster.model.input

import de.akquinet.tim.rawdatamaster.validator.OperationConstraint
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import java.time.Instant

data class PerformanceData(
    @field:NotNull
    val start: Instant? = null,

    @field:NotNull
    val durationInMs: Int? = null,

    @field:NotNull
    @field:OperationConstraint
    val operation: String? = null,

    @field:NotNull
    @field:Pattern(regexp="^\\d{1,5}$", message="status code not in valid format")
    val status: String? = null,

    @field:NotNull
    val message: RawDataMessage? = null
)

