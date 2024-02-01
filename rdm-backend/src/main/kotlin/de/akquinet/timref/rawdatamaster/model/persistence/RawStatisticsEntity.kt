/*
 * Copyright (C) 2023 akquinet GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package de.akquinet.timref.rawdatamaster.model.persistence

import de.akquinet.timref.rawdatamaster.util.toTimestamp
import jakarta.persistence.*
import java.sql.Timestamp
import java.time.Instant

@Entity
@Table(name = "raw_statistics")
data class RawStatisticsEntity(
    @Id
    @Column(name = "homeserver", updatable = false)
    val homeserver: String,

    @Column(name = "total_users")
    val totalUsers: Int,

    @Column(name = "monthly_active_users")
    val monthlyActiveUsers: Int,

    @Column(name = "timestamp")
    val timestamp: Timestamp
) {
    // JPA needs this
    constructor() : this(
        homeserver = "",
        totalUsers = 0,
        monthlyActiveUsers = 0,
        timestamp = Instant.EPOCH.toTimestamp()
    )
}
