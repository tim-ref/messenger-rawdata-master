/*
 * Copyright (C) 2023 akquinet GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package de.akquinet.tim.rawdatamaster.model.persistence

import de.akquinet.tim.rawdatamaster.model.RawDataOperation
import de.akquinet.tim.rawdatamaster.util.millis
import de.akquinet.tim.rawdatamaster.util.plus
import de.akquinet.tim.rawdatamaster.util.toTimestamp
import jakarta.persistence.*
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "raw_data",
    indexes = [Index(columnList = "finish")]
)
data class RawDataEntity(
    @Id
    @GeneratedValue
    @Column(name = "uuid", updatable = false)
    val uuid: UUID = UUID.randomUUID(),

    @Column(name = "start", updatable = false)
    val start: Timestamp,

    @Column(name = "duration_in_ms", updatable = false)
    val durationInMs: Int,

    @Column(name = "finish", updatable = false)
    val finish: Timestamp,

    @Column(name = "operation", updatable = false)
    @Enumerated(EnumType.STRING)
    val operation: RawDataOperation,

    // this contains a http status code
    @Column(name = "status", updatable = false)
    val status: String,

    // string contains JSON, see A_22940
    @Column(name = "message", updatable = false)
    val message: String
) {
    // JPA needs this
    constructor() : this(
        start = Instant.EPOCH,
        durationInMs = 0,
        operation = RawDataOperation.UNKNOWN,
        status = "",
        message = ""
    )

    constructor(
        start: Instant,
        durationInMs: Int,
        operation: RawDataOperation,
        status: String,
        message: String
    ) : this(
        start = start.toTimestamp(),
        durationInMs = durationInMs,
        finish = start.toTimestamp() + durationInMs.millis(),
        operation = operation,
        status = status,
        message = message
    )
}


