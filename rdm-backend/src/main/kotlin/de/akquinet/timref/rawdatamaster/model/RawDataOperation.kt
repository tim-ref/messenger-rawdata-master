/*
 * Copyright (C) 2023 akquinet GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package de.akquinet.timref.rawdatamaster.model

enum class RawDataOperation(val value: String) {
    UNKNOWN("UNKNOWN"),
    RS_LOGIN("TIM.UC_10060_01"),
    RS_CREATE_MESSENGER_SERVICE("TIM.UC_10060_02"),
    RS_ADD_MESSENGER_SERVICE_TO_FEDERATION("TIM.UC_10060_03"),
    MP_CLIENT_LOGIN_SUPPORTED_LOGIN_TYPES("TIM.UC_10057_01"),
    MP_CLIENT_LOGIN_REQUEST_ACCESS_TOKEN("TIM.UC_10057_02"),
    MP_CLIENT_LOGIN_REQUEST_OPENID_TOKEN("TIM.UC_10057_03"),
    MP_INVITE_WITHIN_ORGANISATION_SEARCH("TIM.UC_10104_01"),
    MP_INVITE_WITHIN_ORGANISATION_INVITE("TIM.UC_10104_02"),
    MP_EXCHANGE_EVENT_WITHIN_ORGANISATION("TIM.UC_10063_01"),
    MP_INVITE_OUTSIDE_ORGANISATION_ADD_TO_CONTACT_MANAGEMENT_LIST("TIM.UC_10061_01"),
    MP_INVITE_OUTSIDE_ORGANISATION_INVITE_SENDER("TIM.UC_10061_02"),
    MP_INVITE_OUTSIDE_ORGANISATION_INVITE_RECEIVER("TIM.UC_10061_03"),
    MP_EXCHANGE_EVENT_OUTSIDE_ORGANISATION_SENDER("TIM.UC_10062_01"),
    MP_EXCHANGE_EVENT_OUTSIDE_ORGANISATION_RECEIVER("TIM.UC_10062_02");
}
