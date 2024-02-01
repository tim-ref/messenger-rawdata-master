/*
 * Copyright (C) 2023 akquinet GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package de.akquinet.timref.rawdatamaster.admin

import de.akquinet.timref.rawdatamaster.repository.RawDataRepository
import de.akquinet.timref.rawdatamaster.repository.ReportRepository
import de.akquinet.timref.rawdatamaster.service.StatisticsService
import de.akquinet.timref.rawdatamaster.util.Clock
import de.akquinet.timref.rawdatamaster.util.defaultJsonMapper
import de.akquinet.timref.rawdatamaster.util.minutes
import de.akquinet.timref.rawdatamaster.util.toTimestamp
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.Instant

@RestController
@RequestMapping("/admin")
class AdminController(
    private val clock: Clock,
    private val rawDataRepository: RawDataRepository,
    private val reportRepository: ReportRepository,
    private val statisticsService: StatisticsService
) {
    companion object {
        private const val START_BEFORE_END =
            "Start must be before end."
        private const val DELETED_REPORTS =
            "Successfully deleted %d reports from %s to %s."
    }

    @GetMapping("/raw-data", produces = [MediaType.APPLICATION_JSON_VALUE])
    internal fun findRawData(
        @RequestParam(required = false) from: Instant?,
        @RequestParam(required = false) to: Instant?
    ): ResponseEntity<String> {
        val endExclusive = to ?: clock.now()
        val startInclusive = from ?: (endExclusive - 60.minutes())

        if (!startInclusive.isBefore(endExclusive))
            return ResponseEntity.badRequest().body(START_BEFORE_END)

        return rawDataRepository
            .findBetween(startInclusive.toTimestamp(), endExclusive.toTimestamp())
            .let {
                ResponseEntity.ok(defaultJsonMapper.writeValueAsString(it))
            }
    }

    @GetMapping("/reports", produces = [MediaType.APPLICATION_JSON_VALUE])
    internal fun findReports(
        @RequestParam(required = false) from: Instant?,
        @RequestParam(required = false) to: Instant?
    ): ResponseEntity<String> {
        val end = to ?: clock.now()
        val start = from ?: (end - 60.minutes())

        if (!start.isBefore(end))
            return ResponseEntity.badRequest().body(START_BEFORE_END)

        return reportRepository
            .findBetween(start.toTimestamp(), end.toTimestamp())
            .let {
                ResponseEntity.ok(defaultJsonMapper.writeValueAsString(it))
            }
    }

    @DeleteMapping("/reports")
    internal fun deleteReports(
        @RequestParam(required = true) from: Instant,
        @RequestParam(required = true) to: Instant
    ): ResponseEntity<String> {
        if (!from.isBefore(to))
            return ResponseEntity.badRequest().body(START_BEFORE_END)

        return reportRepository
            .deleteBetween(from.toTimestamp(), to.toTimestamp())
            .let { deleted ->
                ResponseEntity.ok(DELETED_REPORTS.format(deleted, from, to))
            }
    }

    @PostMapping("/statistics")
    internal fun sendStatistics() {
        statisticsService.sendStatistics(clock.now())
    }
}
