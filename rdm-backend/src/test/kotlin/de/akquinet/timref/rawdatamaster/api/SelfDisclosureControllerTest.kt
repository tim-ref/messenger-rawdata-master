/*
 * Copyright (C) 2023 akquinet GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package de.akquinet.timref.rawdatamaster.api

import com.fasterxml.jackson.module.kotlin.readValue
import com.ninjasquad.springmockk.MockkBean
import de.akquinet.timref.rawdatamaster.config.BaseConfig
import de.akquinet.timref.rawdatamaster.config.ProductInfoConfiguration
import de.akquinet.timref.rawdatamaster.model.output.ProductInformation
import de.akquinet.timref.rawdatamaster.service.HttpDeliveryService
import de.akquinet.timref.rawdatamaster.service.SelfDisclosureService
import de.akquinet.timref.rawdatamaster.testUtils.performSecure
import de.akquinet.timref.rawdatamaster.testUtils.statusShouldBeOk
import de.akquinet.timref.rawdatamaster.util.defaultXmlMapper
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import org.springframework.web.servlet.config.annotation.EnableWebMvc

@RunWith(SpringRunner::class)
@SpringBootTest(classes = [SelfDisclosureController::class, SelfDisclosureService::class])
@AutoConfigureMockMvc
@EnableWebMvc
@EnableConfigurationProperties(ProductInfoConfiguration::class)
@Import(BaseConfig::class)
class SelfDisclosureControllerTest : DescribeSpec() {
    override fun extensions() = listOf(SpringExtension)

    @Autowired
    private lateinit var context: WebApplicationContext

    @Autowired
    private lateinit var mvc: MockMvc

    @Autowired
    private lateinit var productInfoConfig: ProductInfoConfiguration

    @MockkBean
    private lateinit var httpDeliveryService: HttpDeliveryService

    init {
        beforeEach {
            mvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply<DefaultMockMvcBuilder>(SecurityMockMvcConfigurers.springSecurity())
                .build()
        }

        describe("SelfDisclosureControllerTest") {
            it("answers OK with configured ProductInformation on GET /self-disclosure") {
                val response = mvc.performSecure(
                    MockMvcRequestBuilders.get("/self-disclosure")
                ).andReturn().response

                response.statusShouldBeOk()

                val productInfo = defaultXmlMapper.readValue<ProductInformation>(
                    response.contentAsString
                )

                val expectedProductInfo =
                    productInfoConfig.toProductInformation(productInfo.informationDate.toInstant())

                productInfo shouldBe expectedProductInfo
            }
        }
    }
}
