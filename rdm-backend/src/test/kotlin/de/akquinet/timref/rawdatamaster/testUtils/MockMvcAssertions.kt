/*
 * Copyright (C) 2023 akquinet GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package de.akquinet.timref.rawdatamaster.testUtils

import io.kotest.matchers.Matcher
import io.kotest.matchers.MatcherResult
import io.kotest.matchers.should
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockHttpServletResponse

private fun haveStatus(status: HttpStatus) =
    Matcher<MockHttpServletResponse> { subject ->
        val expectedStatusCode = status.value()
        val actualStatusCode = subject.status
        MatcherResult(
            actualStatusCode == expectedStatusCode,
            { "expected status code $expectedStatusCode but got $actualStatusCode" },
            { "did not expect status code $expectedStatusCode" },
        )
    }

internal fun MockHttpServletResponse.shouldHaveStatus(status: HttpStatus): MockHttpServletResponse =
    apply { this should haveStatus(status) }

internal fun MockHttpServletResponse.statusShouldBeOk(): MockHttpServletResponse =
    shouldHaveStatus(HttpStatus.OK)

internal fun MockHttpServletResponse.statusShouldBeBadRequest(): MockHttpServletResponse =
    shouldHaveStatus(HttpStatus.BAD_REQUEST)

internal fun MockHttpServletResponse.statusShouldBeServiceUnavailable(): MockHttpServletResponse =
    shouldHaveStatus(HttpStatus.SERVICE_UNAVAILABLE)

private fun haveBodyThatContains(text: String) =
    Matcher<MockHttpServletResponse> { subject ->
        val body = subject.contentAsString
        MatcherResult(
            body.contains(text),
            { "response body <\"$body\"> should contain substring <\"$text\">" },
            { "response body should not contain substring <\"$text\">" },
        )
    }

internal fun MockHttpServletResponse.bodyShouldContain(text: String): MockHttpServletResponse =
    apply { this should haveBodyThatContains(text) }

private fun haveBody(text: String) =
    Matcher<MockHttpServletResponse> { subject ->
        val body = subject.contentAsString
        MatcherResult(
            body == text,
            { "response body should be <\"$text\"> but was <\"$body\">" },
            { "response body should not be <\"$text\">" },
        )
    }

internal fun MockHttpServletResponse.shouldHaveBody(text: String): MockHttpServletResponse =
    apply { this should haveBody(text) }
