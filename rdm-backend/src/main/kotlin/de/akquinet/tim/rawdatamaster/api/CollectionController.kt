/*
 * Copyright (C) 2023 akquinet GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package de.akquinet.tim.rawdatamaster.api

import de.akquinet.tim.rawdatamaster.model.input.MatrixStatisticsData
import de.akquinet.tim.rawdatamaster.model.input.PerformanceData
import de.akquinet.tim.rawdatamaster.service.CollectionService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.*

@RestController
class CollectionController(private val collectionService: CollectionService) {
    @PostMapping("/add-performance-data")
    fun addPerformanceData(@Valid @RequestBody performanceData: PerformanceData) {
        collectionService.addPerformanceData(performanceData)
    }

    @PutMapping("/add-statistics-data")
    fun addStatisticsData(@Valid @RequestBody matrixStatisticsData: MatrixStatisticsData): ResponseEntity<String> {
        collectionService.addRawStatistics(matrixStatisticsData)
        return ResponseEntity.ok().body("{}")
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleMethodArgumentNotValidException(e: MethodArgumentNotValidException): ResponseEntity<String> =
        ResponseEntity
            .badRequest()
            .body("Validation failed: ${e.message}")
}
