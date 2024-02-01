/*
 * Copyright (C) 2023 akquinet GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

CREATE TABLE IF NOT EXISTS raw_data(
    "uuid" uuid NOT NULL,
    "start" timestamp NOT NULL,
    duration_in_ms INT NOT NULL,
    "finish" timestamp NOT NULL,
    operation VARCHAR(255) NOT NULL,
    status VARCHAR(5) NOT NULL,
    message text NOT NULL,
    PRIMARY KEY ("uuid")
);

CREATE TABLE IF NOT EXISTS raw_data_report(
    "uuid" uuid NOT NULL,
    "start" timestamp NOT NULL,
    "finish" timestamp NOT NULL,
    "delivered" boolean NOT NULL,
    PRIMARY KEY ("uuid")
);

CREATE TABLE IF NOT EXISTS statistics(
    reference_date timestamp NOT NULL,
    messenger_services INT NOT NULL,
    registered_users INT NOT NULL,
    active_users INT NOT NULL,
    PRIMARY KEY (reference_date)
);
