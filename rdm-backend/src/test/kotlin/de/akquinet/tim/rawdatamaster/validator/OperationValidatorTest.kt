/*
 * Copyright (C) 2023 akquinet GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package de.akquinet.tim.rawdatamaster.validator

import de.akquinet.tim.rawdatamaster.model.RawDataOperation
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.mockk.mockk
import jakarta.validation.ConstraintValidatorContext

class OperationValidatorTest : DescribeSpec({
    describe("OperationValidatorTest") {
        val operationValidator = OperationValidator()
        val context: ConstraintValidatorContext = mockk()

        context("isValid") {
            it("returns true if value is a RawDataOperation") {
                val operationToValidate = RawDataOperation.RS_LOGIN.name
                operationValidator.isValid(operationToValidate, context).shouldBeTrue()
            }

            it("returns false if value is not a RawDataOperation") {
                val operationToValidate = "DEFINITELY_NOT_AN_OPERATION"
                operationValidator.isValid(operationToValidate, context).shouldBeFalse()
            }
        }
    }
})
