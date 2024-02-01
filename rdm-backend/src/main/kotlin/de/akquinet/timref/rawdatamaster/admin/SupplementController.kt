/*
 * Copyright (C) 2023 akquinet GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package de.akquinet.timref.rawdatamaster.admin

import de.akquinet.timref.rawdatamaster.service.SupplementService
import de.akquinet.timref.rawdatamaster.util.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.lang.Integer.max
import java.time.Instant

@RestController
@RequestMapping("/admin/supplement")
class SupplementController(
    private val supplementService: SupplementService,
    private val clock: Clock
) {
    companion object {
        private val log: Logger = LoggerFactory.getLogger(this::class.java)

        private const val SUPPLEMENTS_DISABLED =
            "Sending supplements is disabled on this instance."
        private const val REQUESTING_SUPPLEMENTS_LOG =
            "Requesting {} reports from {} to {} with period of {} minutes."
        private const val REQUESTING_SUPPLEMENTS =
            "Requesting %s reports from %s to %s with period of %d minutes."
        private const val SUPPLEMENTS_IN_QUEUE =
            "There are already supplements in queue. " +
                    "The ongoing redelivery has to be canceled before requesting another one."
        private const val QUEUE_CLEARED =
            "Redelivery queue successfully cleared (%d items removed)."
        private const val START_BEFORE_END =
            "Start must be before end."
    }

    @GetMapping("/queue")
    internal fun getQueueSize(): ResponseEntity<String> =
        ResponseEntity.ok("${supplementService.getQueueSize()}")

    @DeleteMapping("/queue")
    internal fun clearQueue(): ResponseEntity<String> {
        val size = supplementService.getQueueSize()
        supplementService.clearQueue()
        return ResponseEntity.ok(QUEUE_CLEARED.format(size))
    }

    @PostMapping
    internal fun requestSupplements(
        @RequestParam(required = false) from: Instant?,
        @RequestParam(required = false) to: Instant?,
        @RequestParam(required = false) all: Boolean?,
        @RequestParam(required = false) periodMinutes: Int?
    ): ResponseEntity<String> {
        val deliveryRateMinutes = supplementService.getDeliveryRateMinutes()
        if (deliveryRateMinutes <= 0)
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(SUPPLEMENTS_DISABLED)

        if (supplementService.getQueueSize() > 0)
            return ResponseEntity.badRequest().body(SUPPLEMENTS_IN_QUEUE)

        // report duration should be at least one minute
        // defaults to configuration
        val reportPeriodMinutes =
            if (periodMinutes != null) max(1, periodMinutes) else deliveryRateMinutes
        val reportPeriod = reportPeriodMinutes.minutes()

        val now = clock.now()
        val twoReportPeriodsAgo = now - reportPeriod.multipliedBy(2)
        val halfADayAgo = now - 1.days().dividedBy(2)

        val start = (from ?: halfADayAgo).truncatedToMinutes()
        val end = (if (to != null) min(to, twoReportPeriodsAgo) else twoReportPeriodsAgo).truncatedToMinutes()

        val everything = all ?: true
        val everythingString = if (everything) "all" else "missing"

        if(!start.isBefore(end))
            return ResponseEntity.badRequest().body(START_BEFORE_END)

        log.debug(REQUESTING_SUPPLEMENTS_LOG, everythingString, start, end, reportPeriodMinutes)

        supplementService.enqueueSupplements(start, end, everything, reportPeriod)

        return ResponseEntity.ok(REQUESTING_SUPPLEMENTS.format(everythingString, start, end, reportPeriodMinutes))
    }
}
