/*
 * Copyright (C) 2023 akquinet GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package de.akquinet.tim.rawdatamaster.admin

import com.fasterxml.jackson.module.kotlin.readValue
import com.ninjasquad.springmockk.MockkBean
import de.akquinet.tim.rawdatamaster.model.persistence.RawDataEntity
import de.akquinet.tim.rawdatamaster.model.persistence.ReportEntity
import de.akquinet.tim.rawdatamaster.repository.RawDataRepository
import de.akquinet.tim.rawdatamaster.repository.ReportRepository
import de.akquinet.tim.rawdatamaster.service.StatisticsService
import de.akquinet.tim.rawdatamaster.testUtils.bodyShouldContain
import de.akquinet.tim.rawdatamaster.testUtils.performSecure
import de.akquinet.tim.rawdatamaster.testUtils.shouldHaveBody
import de.akquinet.tim.rawdatamaster.testUtils.statusShouldBeBadRequest
import de.akquinet.tim.rawdatamaster.testUtils.statusShouldBeOk
import de.akquinet.tim.rawdatamaster.util.Clock
import de.akquinet.tim.rawdatamaster.util.defaultJsonMapper
import de.akquinet.tim.rawdatamaster.util.minus
import de.akquinet.tim.rawdatamaster.util.minutes
import de.akquinet.tim.rawdatamaster.util.toTimestamp
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.verify
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import org.springframework.web.servlet.config.annotation.EnableWebMvc
import java.time.Instant

@RunWith(SpringRunner::class)
@SpringBootTest(classes = [AdminController::class])
@AutoConfigureMockMvc
@EnableWebMvc
class AdminControllerTest : DescribeSpec() {
    override fun extensions() = listOf(SpringExtension)

    @Autowired
    private lateinit var context: WebApplicationContext

    @Autowired
    private lateinit var mvc: MockMvc

    @MockkBean
    private lateinit var clock: Clock

    @MockkBean
    private lateinit var rawDataRepository: RawDataRepository

    @MockkBean
    private lateinit var reportRepository: ReportRepository

    @MockkBean
    private lateinit var statisticsService: StatisticsService

    init {
        val anInstant = Instant.parse("2018-04-09T08:59:51Z")

        beforeEach {
            mvc = MockMvcBuilders
                .webAppContextSetup(context)
                .build()

            every { clock.now() } returns anInstant
        }

        describe("AdminControllerTest") {
            context("GET /admin/raw-data") {
                it("uses default values if request parameters are missing") {
                    val rawDataEntities = listOf(RawDataEntity(), RawDataEntity())

                    every {
                        rawDataRepository.findBetween(
                            anInstant.toTimestamp() - 60.minutes(),
                            anInstant.toTimestamp()
                        )
                    } returns rawDataEntities

                    val response = mvc.performSecure(
                        MockMvcRequestBuilders.get("/admin/raw-data")
                    ).andReturn().response

                    response.statusShouldBeOk()

                    response.contentAsString.let {
                        defaultJsonMapper.readValue<List<RawDataEntity>>(it)
                    } shouldBe rawDataEntities
                }

                it("answers BAD_REQUEST if startInclusive > endExclusive") {
                    val aMinuteAgo = anInstant - 1.minutes()

                    val response = mvc.performSecure(
                        MockMvcRequestBuilders
                            .get("/admin/raw-data")
                            .param("from", "$anInstant")
                            .param("to", "$aMinuteAgo")
                    ).andReturn().response

                    response
                        .statusShouldBeBadRequest()
                        .bodyShouldContain("Start must be before end")
                }
            }

            context("GET /admin/reports") {
                it("uses default values if request parameters are missing") {
                    val reportEntities = listOf(ReportEntity(), ReportEntity())

                    every {
                        reportRepository.findBetween(
                            anInstant.toTimestamp() - 60.minutes(),
                            anInstant.toTimestamp()
                        )
                    } returns reportEntities

                    val response = mvc.performSecure(
                        MockMvcRequestBuilders.get("/admin/reports")
                    ).andReturn().response

                    response.statusShouldBeOk()

                    response.contentAsString.let {
                        defaultJsonMapper.readValue<List<ReportEntity>>(it)
                    } shouldBe reportEntities
                }

                it("answers BAD_REQUEST if start > end") {
                    val aMinuteAgo = anInstant - 1.minutes()

                    val response = mvc.performSecure(
                        MockMvcRequestBuilders
                            .get("/admin/reports")
                            .param("from", "$anInstant")
                            .param("to", "$aMinuteAgo")
                    ).andReturn().response

                    response
                        .statusShouldBeBadRequest()
                        .bodyShouldContain("Start must be before end")
                }
            }

            context("DELETE /admin/reports") {
                it("fails if request parameters are missing") {
                    val response = mvc.performSecure(
                        MockMvcRequestBuilders.delete("/admin/reports")
                    ).andReturn().response

                    response
                        .statusShouldBeBadRequest()
                        .shouldHaveBody("")
                }

                it("answers OK with number of deleted reports") {
                    val anHourAgo = anInstant - 60.minutes()

                    every {
                        reportRepository.deleteBetween(anHourAgo.toTimestamp(), anInstant.toTimestamp())
                    } returns 777

                    val response = mvc.performSecure(
                        MockMvcRequestBuilders
                            .delete("/admin/reports")
                            .param("from", "$anHourAgo")
                            .param("to", "$anInstant")
                    ).andReturn().response

                    response
                        .statusShouldBeOk()
                        .bodyShouldContain("deleted 777 reports")
                }

                it("answers BAD_REQUEST if start > end") {
                    val aMinuteAgo = anInstant - 1.minutes()

                    val response = mvc.performSecure(
                        MockMvcRequestBuilders
                            .delete("/admin/reports")
                            .param("from", "$anInstant")
                            .param("to", "$aMinuteAgo")
                    ).andReturn().response

                    response
                        .statusShouldBeBadRequest()
                        .bodyShouldContain("Start must be before end")
                }
            }

            context("POST /admin/statistics") {
                it("sends statistics") {
                    every { statisticsService.sendStatistics(anInstant) } returns Unit

                    val response = mvc.performSecure(
                        MockMvcRequestBuilders.post("/admin/statistics")
                    ).andReturn().response

                    response
                        .statusShouldBeOk()
                        .shouldHaveBody("")

                    verify {
                        statisticsService.sendStatistics(anInstant)
                    }
                }
            }
        }
    }
}
