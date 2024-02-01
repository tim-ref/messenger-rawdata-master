/*
 * Copyright (C) 2023 akquinet GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package de.akquinet.timref.rawdatamaster.model.output

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.ZonedDateTime

data class EnrichedStatistics(
    @JsonProperty("Abfragezeitpunkt")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    val requestTime: ZonedDateTime,

    @JsonProperty("CI_ID")
    val instanceId: String,

    @JsonProperty("TIM-FD_Anzahl_Messenger-Service")
    val messengerServices: Int,

    @JsonProperty("TIM-FD_Anzahl_Nutzer")
    val registeredUsers: Int,

    @JsonProperty("TIM-FD_Anzahl_aktNutzer")
    val activeUsers: Int
)
