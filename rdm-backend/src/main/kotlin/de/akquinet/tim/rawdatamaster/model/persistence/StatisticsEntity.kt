/*
 * Copyright (C) 2023 akquinet GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package de.akquinet.tim.rawdatamaster.model.persistence

import de.akquinet.tim.rawdatamaster.util.toTimestamp
import jakarta.persistence.*
import java.sql.Timestamp
import java.time.Instant

@Entity
@Table(name = "statistics")
data class StatisticsEntity(
    @Id
    @Column(name = "reference_date", updatable = false)
    val referenceDate: Timestamp = Instant.now().toTimestamp(),

    @Column(name = "messenger_services", updatable = false)
    val messengerServices: Int,

    @Column(name = "registered_users", updatable = false)
    val registeredUsers: Int,

    @Column(name = "active_users", updatable = false)
    val activeUsers: Int
) {
    // JPA needs this
    constructor() : this(
        referenceDate = Instant.EPOCH.toTimestamp(),
        messengerServices = 0,
        registeredUsers = 0,
        activeUsers = 0
    )
}
