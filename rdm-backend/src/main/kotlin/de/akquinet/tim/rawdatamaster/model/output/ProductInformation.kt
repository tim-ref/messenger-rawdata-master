/*
 * Copyright (C) 2023 akquinet GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package de.akquinet.tim.rawdatamaster.model.output

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.ZonedDateTime

data class ProductInformation(
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    val informationDate: ZonedDateTime,
    val productTypeInformation: ProductTypeInformation,
    val productIdentification: ProductIdentification,
    val productMiscellaneous: ProductMiscellaneous
)

data class ProductTypeInformation(
    val productType: String,
    val productTypeVersion: String
)

data class ProductIdentification(
    val productVendorId: String,
    val productCode: String,
    val productVersion: ProductVersion
)

data class ProductVersion(
    val local: String,
    val central: String
)

data class ProductMiscellaneous(
    val productVendorName: String,
    val productName: String
)
