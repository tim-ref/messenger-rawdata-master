/*
 * Copyright (C) 2023 akquinet GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package de.akquinet.timref.rawdatamaster.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.ninjasquad.springmockk.MockkBean
import de.akquinet.timref.rawdatamaster.model.RawDataOperation
import de.akquinet.timref.rawdatamaster.model.input.RawDataMessage
import de.akquinet.timref.rawdatamaster.service.CollectionService
import de.akquinet.timref.rawdatamaster.testUtils.bodyShouldContain
import de.akquinet.timref.rawdatamaster.testUtils.performSecure
import de.akquinet.timref.rawdatamaster.testUtils.statusShouldBeBadRequest
import de.akquinet.timref.rawdatamaster.util.defaultJsonMapper
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.extensions.spring.SpringExtension
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
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
@SpringBootTest(classes = [CollectionController::class, ObjectMapper::class])
@AutoConfigureMockMvc
@EnableWebMvc
class CollectionControllerTest : DescribeSpec() {
    override fun extensions() = listOf(SpringExtension)

    @Autowired
    private lateinit var context: WebApplicationContext

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var mvc: MockMvc

    // this one is needed although it seems to be unused
    @MockkBean
    private lateinit var collectionService: CollectionService

    init {
        beforeEach {
            objectMapper.registerModule(JavaTimeModule())
            mvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply<DefaultMockMvcBuilder>(SecurityMockMvcConfigurers.springSecurity())
                .build()
        }

        describe("CollectionControllerTest") {
            context("POST /add-performance-data") {
                fun performanceDataString(
                    start: Instant?,
                    durationInMs: Int?,
                    operationName: String?,
                    status: String?,
                    message: RawDataMessage?
                ): String = """
                    {
                      "start": ${start?.let { "${it.epochSecond}.${it.nano}" }},
                      "durationInMs": $durationInMs,
                      "operation": ${operationName?.let { "\"$it\"" }},
                      "status": ${status?.let { "\"$it\"" }},
                      "message": ${message?.let { defaultJsonMapper.writeValueAsString(it) }}
                    }""".trimIndent()

                context("controller answers BAD_REQUEST if PerformanceData is invalid") {
                    val anInstant = Instant.now()
                    val exampleRawDataMessage = RawDataMessage("Instanz_ID", "TestTypVersion", "TestVersion", "Ausprägung", "Plattform", "OS", "OS_Version", "client_id", "Matrix_Domain", 23, 45, "8234hs023", "doctor", "200")
                    it("fails if start is null") {
                        val invalidPerformanceDataString = performanceDataString(
                            start = null,
                            durationInMs = 1,
                            operationName = RawDataOperation.RS_LOGIN.name,
                            status = "200",
                            message = exampleRawDataMessage
                        )

                        val result = mvc.performSecure(
                            MockMvcRequestBuilders
                                .post("/add-performance-data")
                                .content(invalidPerformanceDataString)
                                .contentType(MediaType.APPLICATION_JSON)
                        ).andReturn().response

                        result
                            .statusShouldBeBadRequest()
                            .bodyShouldContain("Validation failed")
                    }

                    it("fails if status code is not in the correct range") {
                        val invalidPerformanceDataString = performanceDataString(
                            start = anInstant,
                            durationInMs = 1,
                            operationName = RawDataOperation.RS_LOGIN.name,
                            status = "7777777",
                            message = exampleRawDataMessage
                        )

                        val result = mvc.performSecure(
                            MockMvcRequestBuilders
                                .post("/add-performance-data")
                                .content(invalidPerformanceDataString)
                                .contentType(MediaType.APPLICATION_JSON)
                        ).andReturn().response

                        result
                            .statusShouldBeBadRequest()
                            .bodyShouldContain("Validation failed")
                            .bodyShouldContain("status code not in valid format")
                    }

                    it("fails if operation is invalid") {
                        val invalidPerformanceDataString = performanceDataString(
                            start = anInstant,
                            durationInMs = 1,
                            operationName = "TOTALLY_NOT_AN_OPERATION",
                            status = "200",
                            message = exampleRawDataMessage
                        )

                        val result = mvc.performSecure(
                            MockMvcRequestBuilders
                                .post("/add-performance-data")
                                .content(invalidPerformanceDataString)
                                .contentType(MediaType.APPLICATION_JSON)
                        ).andReturn().response

                        result
                            .statusShouldBeBadRequest()
                            .bodyShouldContain("Validation failed")
                            .bodyShouldContain("not a valid operation")
                    }
                }
            }

            context("PUT /add-statistics-data") {
                fun statisticsDataString(
                    homeserver: String?,
                    totalUsers: Int?,
                    activeUsers: Int?,
                    timstamp: Long?
                ): String = """
                    {
                      "homeserver": $homeserver, 
                      "total_users": $totalUsers,
                      "monthly_active_users": $activeUsers,
                      "timestamp": $timstamp
                    }""".trimIndent()

                context("controller answers BAD_REQUEST if StatisticsData is invalid") {
                    it("fails if field is missing") {
                        val invalidStatisticsDataString = statisticsDataString(
                            homeserver = null,
                            totalUsers = 100,
                            activeUsers = 50,
                            timstamp = 1688528700000L
                        )

                        val result = mvc.performSecure(
                            MockMvcRequestBuilders
                                .put("/add-statistics-data")
                                .content(invalidStatisticsDataString)
                                .contentType(MediaType.APPLICATION_JSON)
                        ).andReturn().response

                        result
                            .statusShouldBeBadRequest()
                            .bodyShouldContain("Validation failed")
                    }
                }
            }
        }
    }
}
