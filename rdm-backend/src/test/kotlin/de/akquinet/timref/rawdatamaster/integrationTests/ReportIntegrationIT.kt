/*
 * Copyright (C) 2023 akquinet GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package de.akquinet.timref.rawdatamaster.integrationTests

import com.ninjasquad.springmockk.MockkBean
import de.akquinet.timref.rawdatamaster.config.BaseConfig
import de.akquinet.timref.rawdatamaster.model.RawDataOperation
import de.akquinet.timref.rawdatamaster.model.input.PerformanceData
import de.akquinet.timref.rawdatamaster.model.input.RawDataMessage
import de.akquinet.timref.rawdatamaster.repository.RawDataRepository
import de.akquinet.timref.rawdatamaster.repository.ReportRepository
import de.akquinet.timref.rawdatamaster.service.CollectionService
import de.akquinet.timref.rawdatamaster.service.HttpDeliveryService
import de.akquinet.timref.rawdatamaster.service.ReportService
import de.akquinet.timref.rawdatamaster.service.SupplementService
import de.akquinet.timref.rawdatamaster.util.minutes
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.zonky.test.db.AutoConfigureEmbeddedDatabase
import jakarta.annotation.PostConstruct
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.flywaydb.core.Flyway
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.core.io.InputStreamResource
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.web.client.RestTemplate
import java.time.Instant
import javax.sql.DataSource

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "rawdata-master.rawDataReport.autoSupplementOnStart=false",
        "rawdata-master.rawDataReport.deliveryRateMinutes=0"
    ]
)
@RunWith(SpringRunner::class)
@AutoConfigureEmbeddedDatabase
@ActiveProfiles("integration-tests")
@Import(BaseConfig::class)
class ReportIntegrationIT : DescribeSpec() {
    override fun extensions() = listOf(SpringExtension)

    @Value("\${rawdata-master.rawDataReport.url}")
    private lateinit var rawDataUrl: String

    @Autowired
    private lateinit var embeddedDatasource: DataSource

    @Autowired
    private lateinit var rawDataRepository: RawDataRepository

    @Autowired
    private lateinit var reportRepository: ReportRepository

    @Autowired
    private lateinit var collectionService: CollectionService

    @Autowired
    private lateinit var reportService: ReportService

    @Autowired
    private lateinit var supplementService: SupplementService

    @Autowired
    private lateinit var httpDeliveryService: HttpDeliveryService

    @MockkBean
    private lateinit var restTemplate: RestTemplate

    @PostConstruct
    fun migrateWithFlyway() {
        Flyway
            .configure()
            .dataSource(embeddedDatasource)
            .locations("db/migration", "db/testdata-dev")
            .cleanOnValidationError(true)
            .load()
            .migrate()
    }

    init {
        afterEach {
            withContext(Dispatchers.IO) {
                rawDataRepository.deleteAll()
                reportRepository.deleteAll()
            }
        }

        describe("ReportIntegrationTest") {
            it("sends performance report") {
                val anInstant = Instant.parse("2018-04-09T08:59:51Z")

                val somePerformanceData = PerformanceData(
                    start = anInstant,
                    durationInMs = 100,
                    operation = RawDataOperation.RS_LOGIN.name,
                    status = "200",
                    message = null
                )

                (0..4).forEach {
                    somePerformanceData.copy(
                        message = RawDataMessage("Instanz_ID".plus(it), "TestTypVersion", "TestVersion", "Ausprägung", "Plattform", "OS", "OS_Version", "client_id", "Matrix_Domain", 23, 45, "8234hs023", "doctor", "200")
                    ).also { performanceData ->
                        collectionService.addPerformanceData(performanceData)
                    }
                }

                val gematikReportCollector = mutableListOf<String>()

                every {
                    restTemplate.exchange(rawDataUrl, HttpMethod.POST, any(), String::class.java)
                } answers {
                    thirdArg<HttpEntity<InputStreamResource>>().also { httpEntity ->
                        httpEntity
                            .body!!
                            .inputStream
                            .bufferedReader()
                            .use { it.readText() }
                            .also { gematikReportCollector.add(it) }
                    }

                    ResponseEntity.ok().build()
                }

                reportService.sendReport(
                    startInclusive = anInstant,
                    endExclusive = anInstant + 5.minutes()
                )

                reportRepository.findAll() shouldHaveSize 1

                gematikReportCollector shouldHaveSize 1

                val reportString = gematikReportCollector
                    .first()
                    .replace("\r", "") // remove all carriage returns
                    .dropLast(1) // remove single linebreak at the end

                reportString shouldBe """
                    1523264391000;100;TIM.UC_10060_01;200;{"Inst-ID":"Instanz_ID0","UA-PTV":"TestTypVersion","UA-PV":"TestVersion","UA-A":"Ausprägung","UA-P":"Plattform","UA-OS":"OS","UA-OS-VERSION":"OS_Version","UA-cid":"client_id","M-Dom":"Matrix_Domain","sizeIn":23,"sizeOut":45,"tID":"8234hs023","profOID":"doctor","Res":"200"}
                    1523264391000;100;TIM.UC_10060_01;200;{"Inst-ID":"Instanz_ID1","UA-PTV":"TestTypVersion","UA-PV":"TestVersion","UA-A":"Ausprägung","UA-P":"Plattform","UA-OS":"OS","UA-OS-VERSION":"OS_Version","UA-cid":"client_id","M-Dom":"Matrix_Domain","sizeIn":23,"sizeOut":45,"tID":"8234hs023","profOID":"doctor","Res":"200"}
                    1523264391000;100;TIM.UC_10060_01;200;{"Inst-ID":"Instanz_ID2","UA-PTV":"TestTypVersion","UA-PV":"TestVersion","UA-A":"Ausprägung","UA-P":"Plattform","UA-OS":"OS","UA-OS-VERSION":"OS_Version","UA-cid":"client_id","M-Dom":"Matrix_Domain","sizeIn":23,"sizeOut":45,"tID":"8234hs023","profOID":"doctor","Res":"200"}
                    1523264391000;100;TIM.UC_10060_01;200;{"Inst-ID":"Instanz_ID3","UA-PTV":"TestTypVersion","UA-PV":"TestVersion","UA-A":"Ausprägung","UA-P":"Plattform","UA-OS":"OS","UA-OS-VERSION":"OS_Version","UA-cid":"client_id","M-Dom":"Matrix_Domain","sizeIn":23,"sizeOut":45,"tID":"8234hs023","profOID":"doctor","Res":"200"}
                    1523264391000;100;TIM.UC_10060_01;200;{"Inst-ID":"Instanz_ID4","UA-PTV":"TestTypVersion","UA-PV":"TestVersion","UA-A":"Ausprägung","UA-P":"Plattform","UA-OS":"OS","UA-OS-VERSION":"OS_Version","UA-cid":"client_id","M-Dom":"Matrix_Domain","sizeIn":23,"sizeOut":45,"tID":"8234hs023","profOID":"doctor","Res":"200"}
                """.trimIndent()
            }

            it("sends supplements") {
                val now = Instant.parse("2018-04-09T09:00:00Z")

                // We mock the gematik endpoint here.
                // For some report entities, the upload should succeed.
                // For other report entities, it should fail.
                // Calling the helpers below makes it easy to toggle the behaviour.

                fun reportUploadSucceeds() =
                    every {
                        restTemplate.exchange(rawDataUrl, HttpMethod.POST, any(), String::class.java)
                    } answers {
                        ResponseEntity.ok().build()
                    }

                fun reportUploadFails() =
                    every {
                        restTemplate.exchange(rawDataUrl, HttpMethod.POST, any(), String::class.java)
                    } answers {
                        ResponseEntity.internalServerError().build()
                    }

                reportUploadSucceeds()

                reportService.sendReport(
                    startInclusive = now,
                    endExclusive = now + 2.minutes()
                )

                reportService.sendReport(
                    startInclusive = now + 2.minutes(),
                    endExclusive = now + 4.minutes()
                )

                reportUploadFails()

                reportService.sendReport(
                    startInclusive = now + 4.minutes(),
                    endExclusive = now + 6.minutes()
                )

                reportUploadSucceeds()

                reportService.sendReport(
                    startInclusive = now + 9.minutes(),
                    endExclusive = now + 12.minutes()
                )

                reportUploadFails()

                reportService.sendReport(
                    startInclusive = now + 12.minutes(),
                    endExclusive = now + 15.minutes()
                )

                // initial state of the report repository

                val entitiesBefore = reportRepository.findAll()
                entitiesBefore shouldHaveSize 5
                entitiesBefore.map { it.delivered } shouldBe
                        listOf(true, true, false, true, false)

                // send supplements (gematik upload now always succeeds)

                reportUploadSucceeds()

                val supplementStart =  now - 2.minutes()
                val supplementEnd = now + 20.minutes()
                val period = 3.minutes()

                supplementService.enqueueSupplements(
                    start = supplementStart,
                    end = supplementEnd,
                    all = true,
                    reportPeriod = period
                )

                supplementService.sendAllSupplements()

                val entitiesAfter = reportRepository.findAll().sortedBy { it.start }
                entitiesAfter shouldHaveSize 5 + 1 + 1 + 2 // 9

                // all entities are now delivered
                entitiesAfter.all { it.delivered }.shouldBeTrue()

                // UUIDs of entities that were already there should be the same as before
                entitiesAfter.map { it.uuid } shouldContainAll
                        entitiesBefore.map { it.uuid }

                // correct interpolation at the boundary
                supplementStart shouldBe Instant.parse("2018-04-09T08:58:00Z")
                entitiesAfter.first().start.toInstant() shouldBe Instant.parse("2018-04-09T08:57:00Z")

                supplementEnd shouldBe Instant.parse("2018-04-09T09:20:00Z")
                entitiesAfter.last().finish.toInstant() shouldBe Instant.parse("2018-04-09T09:21:00Z")
            }
        }
    }
}
