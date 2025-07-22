/*
 * Copyright (C) 2023 akquinet GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package de.akquinet.tim.rawdatamaster.admin

import com.ninjasquad.springmockk.MockkBean
import de.akquinet.tim.rawdatamaster.config.BaseConfig
import de.akquinet.tim.rawdatamaster.service.SupplementService
import de.akquinet.tim.rawdatamaster.testUtils.*
import de.akquinet.tim.rawdatamaster.util.Clock
import de.akquinet.tim.rawdatamaster.util.days
import de.akquinet.tim.rawdatamaster.util.minutes
import de.akquinet.tim.rawdatamaster.util.truncatedToMinutes
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.verify
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import org.springframework.web.servlet.config.annotation.EnableWebMvc
import java.time.Instant

@RunWith(SpringRunner::class)
@SpringBootTest(classes = [SupplementController::class])
@AutoConfigureMockMvc
@EnableWebMvc
@Import(BaseConfig::class)
class SupplementControllerTest : DescribeSpec() {
    override fun extensions() = listOf(SpringExtension)

    @Autowired
    private lateinit var context: WebApplicationContext

    @Autowired
    private lateinit var mvc: MockMvc

    @MockkBean
    private lateinit var supplementService: SupplementService

    @MockkBean
    private lateinit var clock: Clock

    private val deliveryRateMinutes: Int = 5

    init {
        val anInstant = Instant.parse("2018-04-09T08:59:51Z")

        beforeEach {
            mvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply<DefaultMockMvcBuilder>(SecurityMockMvcConfigurers.springSecurity())
                .build()

            every { clock.now() } returns anInstant

            every {
                supplementService.enqueueSupplements(any(), any(), any(), any())
            } returns listOf()

            every {
                supplementService.getDeliveryRateMinutes()
            } returns deliveryRateMinutes
        }

        describe("SupplementControllerTest") {
            context("POST on /admin/supplement succeeds") {
                it("uses default values if request parameters are missing") {
                    every { supplementService.getQueueSize() } returns 0

                    val response = mvc.performSecure(
                        MockMvcRequestBuilders.post("/admin/supplement")
                    ).andReturn().response

                    response.status shouldBe HttpStatus.OK.value()

                    val expectedStart = anInstant.truncatedToMinutes()
                    val expectedPeriod = deliveryRateMinutes.minutes()

                    verify {
                        supplementService.enqueueSupplements(
                            start = expectedStart - 1.days().dividedBy(2),
                            end = expectedStart - expectedPeriod.multipliedBy(2),
                            all = true,
                            reportPeriod = expectedPeriod
                        )
                    }
                }

                it("ensures that end is not after now minus 2 periods") {
                    every { supplementService.getQueueSize() } returns 0

                    val future = anInstant + 3.minutes()
                    val periodMinutes = 1
                    val period = periodMinutes.minutes()

                    val response = mvc.performSecure(
                        MockMvcRequestBuilders
                            .post("/admin/supplement")
                            .param("to", "$future")
                            .param("periodMinutes", "$periodMinutes")
                    ).andReturn().response

                    response.statusShouldBeOk()

                    val expectedEnd = anInstant.truncatedToMinutes() - period.multipliedBy(2)

                    verify {
                        supplementService.enqueueSupplements(
                            start = any(),
                            end = expectedEnd,
                            all = true,
                            reportPeriod = period
                        )
                    }
                }

                it("ensures report period of at least 1 minute") {
                    every { supplementService.getQueueSize() } returns 0

                    val periodMinutes = 0

                    val response = mvc.performSecure(
                        MockMvcRequestBuilders
                            .post("/admin/supplement")
                            .param("periodMinutes", "$periodMinutes")
                    ).andReturn().response

                    response.statusShouldBeOk()

                    verify {
                        supplementService.enqueueSupplements(
                            start = any(),
                            end = any(),
                            all = true,
                            reportPeriod = 1.minutes()
                        )
                    }
                }
            }

            context("POST on /admin/supplement fails") {
                it("answers SERVICE_UNAVAILABLE if supplements are disabled") {
                    every { supplementService.getDeliveryRateMinutes() } returns 0

                    val response = mvc.performSecure(
                        MockMvcRequestBuilders.post("/admin/supplement")
                    ).andReturn().response

                    response
                        .statusShouldBeServiceUnavailable()
                        .bodyShouldContain("disabled")

                    verify(exactly = 0) {
                        supplementService.enqueueSupplements(any(), any(), any(), any())
                    }
                }

                it("answers BAD_REQUEST if redelivery queue is not empty") {
                    every { supplementService.getQueueSize() } returns 777

                    val response = mvc.performSecure(
                        MockMvcRequestBuilders.post("/admin/supplement")
                    ).andReturn().response

                    response
                        .statusShouldBeBadRequest()
                        .bodyShouldContain("already supplements in queue")

                    verify(exactly = 0) {
                        supplementService.enqueueSupplements(any(), any(), any(), any())
                    }
                }

                it("answers BAD_REQUEST if from > end") {
                    every { supplementService.getQueueSize() } returns 0

                    val aMinuteAgo = anInstant - 1.minutes()

                    val response = mvc.performSecure(
                        MockMvcRequestBuilders
                            .post("/admin/supplement")
                            .param("from", "$anInstant")
                            .param("to", "$aMinuteAgo")
                    ).andReturn().response

                    response
                        .statusShouldBeBadRequest()
                        .bodyShouldContain("Start must be before end")
                }
            }

            context("GET on /admin/supplement/queue") {
                it("answers OK with queue size") {
                    every { supplementService.getQueueSize() } returns 777

                    val response = mvc.performSecure(
                        MockMvcRequestBuilders.get("/admin/supplement/queue")
                    ).andReturn().response

                    response
                        .statusShouldBeOk()
                        .shouldHaveBody("777")
                }
            }

            context("DELETE on /admin/supplement/queue") {
                it("answers OK") {
                    every { supplementService.getQueueSize() } returns 777
                    every { supplementService.clearQueue() } returns Unit

                    val response = mvc.performSecure(
                        MockMvcRequestBuilders.delete("/admin/supplement/queue")
                    ).andReturn().response

                    response
                        .statusShouldBeOk()
                        .bodyShouldContain("cleared")
                        .bodyShouldContain("777 items removed")

                    verify {
                        supplementService.clearQueue()
                    }
                }
            }
        }
    }
}
