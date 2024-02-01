/*
 * Copyright (C) 2023 akquinet GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package de.akquinet.timref.rawdatamaster.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.InputStreamResource
import org.springframework.http.*
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.io.InputStream

@Service
class HttpDeliveryService(
    @param:Value("\${rawdata-master.rawDataReport.url}")
    private val rawDataUrl: String,
    @param:Value("\${rawdata-master.selfDisclosure.url}")
    private val selfDisclosureUrl: String,
    @param:Value("\${rawdata-master.statistics.url}")
    private val statisticsUrl: String,
    private val restTemplate: RestTemplate
) {
    companion object {
        private const val FILENAME_HEADER = "filename"
        private const val ACCEPT_ENCODING_VALUE = "gzip, deflate-Encoding"

        private val log: Logger = LoggerFactory.getLogger(this::class.java)
    }

    // see I_OpsData_Update::fileUpload according to gemSpec_SST_LD_BD#A_17733-01
    private fun sendToGematik(
        url: String,
        reportFileName: String,
        inputStream: InputStream,
        contentLength: Long
    ): Boolean = if (url.isEmpty()) { false } else try {
        val time = System.currentTimeMillis()

        val headers = HttpHeaders().apply {
            this.contentType = MediaType.APPLICATION_OCTET_STREAM
            this.contentLength = contentLength
            add(FILENAME_HEADER, reportFileName)
            add(HttpHeaders.ACCEPT_ENCODING, ACCEPT_ENCODING_VALUE)
        }

        val httpEntity = HttpEntity(InputStreamResource(inputStream), headers)

        val response = restTemplate.exchange(
            url, HttpMethod.POST, httpEntity, String::class.java
        )

        val statusCode = response.statusCode

        if (statusCode == HttpStatus.OK) {
            log.info(
                "Report {} successfully sent in {} ms, length: {}",
                reportFileName,
                System.currentTimeMillis() - time,
                contentLength
            )
            true
        } else {
            log.error("Error sending report {}: {}", reportFileName, response.statusCode)
            false
        }
    } catch (throwable: Throwable) {
        log.error("Sending report {} failed", reportFileName, throwable)
        false
    }

    internal fun sendRawData(
        reportFileName: String,
        inputStream: InputStream,
        contentLength: Long
    ): Boolean = sendToGematik(
        url = rawDataUrl,
        reportFileName = reportFileName,
        inputStream = inputStream,
        contentLength = contentLength
    )

    internal fun sendSelfDisclosure(
        selfDisclosureXml: String,
        reportFileName: String
    ): Boolean = selfDisclosureXml.toByteArray().let {
        sendToGematik(
            url = selfDisclosureUrl,
            reportFileName = reportFileName,
            inputStream = it.inputStream(),
            contentLength = it.size.toLong()
        )
    }

    internal fun sendStatistics(
        statisticsJson: String,
        reportFileName: String
    ): Boolean = statisticsJson.toByteArray().let {
        sendToGematik(
            url = statisticsUrl,
            reportFileName = reportFileName,
            inputStream = it.inputStream(),
            contentLength = it.size.toLong()
        )
    }
}
