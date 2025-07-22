/*
 * Copyright (C) 2023 akquinet GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package de.akquinet.tim.rawdatamaster.config

import org.slf4j.MDC
import org.springframework.web.filter.OncePerRequestFilter
import java.io.IOException
import java.util.*
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

class MDCHttpRequestFilter : OncePerRequestFilter() {
    @Throws(ServletException::class, IOException::class)
    override fun doFilterInternal(
        httpServletRequest: HttpServletRequest,
        httpServletResponse: HttpServletResponse,
        filterChain: FilterChain
    ) {
        MDC.put(EVENT_ID, newEventId())
        val method = httpServletRequest.method
        if (method != null && method.isNotEmpty()) {
            MDC.put(METHOD, method)
        }
        val requestURL = httpServletRequest.requestURL
        if (requestURL != null) {
            MDC.put(URL, requestURL.toString())
        }
        val userAgent = httpServletRequest.getHeader("User-Agent")
        if (userAgent != null && userAgent.isNotEmpty()) {
            MDC.put(USER_AGENT, userAgent)
        }
        try {
            filterChain.doFilter(httpServletRequest, httpServletResponse)
        } finally {
            MDC.clear()
        }
    }

    private fun newEventId(): String = UUID.randomUUID().toString()

    companion object {
        const val EVENT_ID = "eventId"
        const val METHOD = "method"
        const val URL = "url"
        const val USER_AGENT = "userAgent"
    }
}
