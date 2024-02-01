/*
 * Copyright (C) 2023 akquinet GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package de.akquinet.timref.rawdatamaster.service

import de.akquinet.timref.rawdatamaster.config.ProductInfoConfiguration
import de.akquinet.timref.rawdatamaster.util.minutes
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.scheduling.TaskScheduler
import java.time.Duration
import java.time.Instant

class SelfDisclosureServiceTest : DescribeSpec({
    describe("SelfDisclosureServiceTest") {
        val productInfoConfig = ProductInfoConfiguration(
            productType = "productType",
            productTypeVersion = "productTypeVersion",
            productVendorId = "productVendorId",
            productCode = "productCode",
            productVersionLocal = "productVersionLocal",
            productVersionCentral = "productVersionCentral",
            productVendorName = "productVendorName",
            productName = "productName"
        )

        val httpDeliveryServiceMock: HttpDeliveryService = mockk()
        val taskSchedulerMock: TaskScheduler = mockk()

        val service = SelfDisclosureService(
            instanceId = "CI-999999",
            deliveryRateMinutes = 60,
            productInfoConfig = productInfoConfig,
            httpDeliveryService = httpDeliveryServiceMock,
            taskScheduler = taskSchedulerMock
        )

        afterEach {
            clearAllMocks()
        }

        it("gets product information as XML") {
            val anInstant = Instant.parse("2018-04-09T08:59:51Z")

            val expectedXml = """
                <ProductInformation>
                  <informationDate>2018-04-09T08:59:51.000Z</informationDate>
                  <productTypeInformation>
                    <productType>productType</productType>
                    <productTypeVersion>productTypeVersion</productTypeVersion>
                  </productTypeInformation>
                  <productIdentification>
                    <productVendorId>productVendorId</productVendorId>
                    <productCode>productCode</productCode>
                    <productVersion>
                      <local>productVersionLocal</local>
                      <central>productVersionCentral</central>
                    </productVersion>
                  </productIdentification>
                  <productMiscellaneous>
                    <productVendorName>productVendorName</productVendorName>
                    <productName>productName</productName>
                  </productMiscellaneous>
                </ProductInformation>
            """.trimIndent()

            service.getProductInfoXml(anInstant).trim() shouldBe expectedXml
        }

        it("creates a report name from components") {
            val start = Instant.parse("2018-04-09T08:00:00Z")
            val end = Instant.parse("2018-04-09T09:00:00Z")

            val reportName = service.createReportName(start, end)
            reportName shouldBe "CI-999999_1523260800000_1523264400000_inf.xml"
        }

        it("sends product information via http") {
            val expectedStart = Instant.parse("2018-04-09T09:00:00Z")
            val expectedEnd = Instant.parse("2018-04-09T09:05:00Z")
            val expectedFileName = service.createReportName(expectedStart, expectedEnd)

            every {
                httpDeliveryServiceMock.sendSelfDisclosure(any(), expectedFileName)
            } returns true

            service.sendSelfDisclosure(expectedStart, expectedEnd)
        }

        context("scheduling reports") {
            it("automatically schedules reports") {
                every { taskSchedulerMock.scheduleAtFixedRate(any(), any(), any<Duration>()) } returns mockk()

                service.scheduleSelfDisclosure()

                verify(exactly = 1) {
                    taskSchedulerMock.scheduleAtFixedRate(any(), any(), 60.minutes())
                }
            }

            it("does not schedule reports when disabled") {
                val disabledService = SelfDisclosureService(
                    instanceId = "CI-999999",
                    deliveryRateMinutes = 0, // disabled
                    productInfoConfig = productInfoConfig,
                    httpDeliveryService = mockk(),
                    taskScheduler = taskSchedulerMock
                )

                disabledService.scheduleSelfDisclosure()

                verify(exactly = 0) {
                    taskSchedulerMock.scheduleAtFixedRate(any(), any(), any<Duration>())
                }
            }
        }
    }
})
