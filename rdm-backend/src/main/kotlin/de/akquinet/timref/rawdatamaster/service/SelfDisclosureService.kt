/*
 * Copyright (C) 2023 akquinet GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package de.akquinet.timref.rawdatamaster.service

import de.akquinet.timref.rawdatamaster.config.ProductInfoConfiguration
import de.akquinet.timref.rawdatamaster.util.defaultXmlMapper
import de.akquinet.timref.rawdatamaster.util.minutes
import de.akquinet.timref.rawdatamaster.util.roundUpToNextPeriodStartSinceEpoch
import de.akquinet.timref.rawdatamaster.util.truncatedToMinutes
import jakarta.annotation.PostConstruct
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.scheduling.TaskScheduler
import org.springframework.stereotype.Service
import java.time.Instant

@Service
@EnableConfigurationProperties(ProductInfoConfiguration::class)
class SelfDisclosureService(
    @param:Value("\${InstanceID}")
    private val instanceId: String,
    @param:Value("\${rawdata-master.selfDisclosure.deliveryRateMinutes}")
    private val deliveryRateMinutes: Int,
    private val productInfoConfig: ProductInfoConfiguration,
    private val httpDeliveryService: HttpDeliveryService,
    private val taskScheduler: TaskScheduler
) {
    companion object {
        private const val SELF_DISCLOSURE_FILENAME_PATTERN = "%s_%d_%d_inf.xml"

        private val log: Logger = LoggerFactory.getLogger(this::class.java)
    }

    private val sendSelfDisclosureTask = Runnable {
        val now = Instant.now()
        val before = now - deliveryRateMinutes.minutes()
        sendSelfDisclosure(before, now)
    }

    @PostConstruct
    private fun postConstruct() = scheduleSelfDisclosure()

    internal fun scheduleSelfDisclosure() {
        if (deliveryRateMinutes > 0) {
            val now = Instant.now()
            val deliveryRate = deliveryRateMinutes.minutes()
            val nextPeriodStart = roundUpToNextPeriodStartSinceEpoch(now, deliveryRate)

            taskScheduler.scheduleAtFixedRate(sendSelfDisclosureTask, nextPeriodStart, deliveryRate)
        } else {
            log.info("Self-disclosure scheduling disabled")
        }
    }

    internal fun sendSelfDisclosure(start: Instant, end: Instant) {
        val startWholeMinutes = start.truncatedToMinutes()
        val endWholeMinutes = end.truncatedToMinutes()

        httpDeliveryService.sendSelfDisclosure(
            getProductInfoXml(endWholeMinutes),
            createReportName(startWholeMinutes, endWholeMinutes)
        )
    }

    internal fun getProductInfoXml(now: Instant): String =
        productInfoConfig.toProductInformation(now).let {
            defaultXmlMapper.writeValueAsString(it)
        }

    internal fun createReportName(start: Instant, end: Instant) =
        SELF_DISCLOSURE_FILENAME_PATTERN.format(
            instanceId,
            start.toEpochMilli(),
            end.toEpochMilli()
        )
}
