/*
 * Copyright (C) 2023 akquinet GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package de.akquinet.timref.rawdatamaster.config

import de.akquinet.timref.rawdatamaster.model.output.*
import de.akquinet.timref.rawdatamaster.util.toUtcDateTime
import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Instant

@ConfigurationProperties(prefix = "rawdata-master.product-info")
data class ProductInfoConfiguration(
    val productType: String,
    val productTypeVersion: String,
    val productVendorId: String,
    val productCode: String,
    val productVersionLocal: String,
    val productVersionCentral: String,
    val productVendorName: String,
    val productName: String
) {
    internal fun toProductInformation(now: Instant) =
        ProductInformation(
            informationDate = now.toUtcDateTime(),
            productTypeInformation = ProductTypeInformation(
                productType = productType,
                productTypeVersion = productTypeVersion
            ),
            productIdentification = ProductIdentification(
                productVendorId = productVendorId,
                productCode = productCode,
                productVersion = ProductVersion(
                    productVersionLocal,
                    productVersionCentral
                )
            ),
            productMiscellaneous = ProductMiscellaneous(
                productVendorName = productVendorName,
                productName = productName
            )
        )
}
