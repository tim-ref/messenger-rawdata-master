/*
 * Copyright (C) 2023 akquinet GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package de.akquinet.tim.rawdatamaster.repository

import de.akquinet.tim.rawdatamaster.model.persistence.ReportEntity
import jakarta.transaction.Transactional
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.sql.Timestamp
import java.util.*

@Repository
@Transactional
interface ReportRepository : JpaRepository<ReportEntity, UUID> {
    @Query("SELECT * FROM raw_data_report WHERE finish >= :start AND start <= :end ORDER BY start ASC", nativeQuery = true)
    fun findBetween(start: Timestamp, end: Timestamp): List<ReportEntity>

    @Modifying
    @Query("DELETE FROM raw_data_report WHERE finish >= :start AND start <= :end", nativeQuery = true)
    fun deleteBetween(start: Timestamp, end: Timestamp): Int

    @Modifying
    @Query("DELETE FROM raw_data_report WHERE finish < :before", nativeQuery = true)
    fun deleteBefore(before: Timestamp): Int
}
