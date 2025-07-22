/*
 * Copyright (C) 2023 akquinet GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package de.akquinet.tim.rawdatamaster.config

import jakarta.annotation.Nonnull
import org.apache.hc.client5.http.config.ConnectionConfig
import org.apache.hc.client5.http.config.RequestConfig
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder
import org.apache.hc.core5.ssl.SSLContexts
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ResourceLoader
import org.springframework.http.client.ClientHttpRequestFactory
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.web.client.RestTemplate
import java.security.GeneralSecurityException
import java.util.concurrent.TimeUnit

@Configuration
open class RestServiceConfig(
    @Value("\${rawdata-master.fileUpload.connectTimeout:30}") val connectTimeout: Int,
    @Value("\${rawdata-master.fileUpload.connectRequestTimeout:600}") val connectRequestTimeout: Int,
    @Value("\${rawdata-master.fileUpload.readTimeout:30}") val readTimeout: Int,
    @Value("\${rawdata-master.fileUpload.skipSSLHostnameVerification:false}") val skipSSLHostnameVerification: Boolean,
    @Value("\${rawdata-master.fileUpload.trustStorePath:\"\"}") val trustStorePath: String,
    @Value("\${rawdata-master.fileUpload.trustStorePassword:\"\"}") var trustStorePassword: String,
    private val resourceLoader: ResourceLoader
) {
    companion object {
        private val log = LoggerFactory.getLogger(RestServiceConfig::class.java)
    }

    @Bean
    open fun produceRestTemplate(): RestTemplate =
        RestTemplate(createClientHttpRequestFactory(createSSLSocketFactory()))

    @Nonnull
    private fun createClientHttpRequestFactory(
        sslSocketFactory: SSLConnectionSocketFactory
    ): ClientHttpRequestFactory {
        val connConfig = ConnectionConfig.custom()
            .setConnectTimeout(connectTimeout.toLong(), TimeUnit.SECONDS)
            .setSocketTimeout(readTimeout, TimeUnit.SECONDS)
            .build()

        val requestConfig = RequestConfig.custom()
            .setConnectionRequestTimeout(connectRequestTimeout.toLong(), TimeUnit.SECONDS)
            .build()

        val connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
            .setSSLSocketFactory(sslSocketFactory)
            .setDefaultConnectionConfig(connConfig)
            .build()
        connectionManager.defaultMaxPerRoute = 20
        connectionManager.maxTotal = 100

        val clientBuilder = HttpClientBuilder.create()
            .disableCookieManagement()
            .disableAuthCaching()
            .setConnectionManager(connectionManager)
            .setDefaultRequestConfig(requestConfig)
            .build()

        val requestFactory = HttpComponentsClientHttpRequestFactory(clientBuilder)
        requestFactory.setBufferRequestBody(false)
        return requestFactory
    }

    private fun createSSLSocketFactory(): SSLConnectionSocketFactory {
        return try {
            var sslContextBuilder = SSLContexts.custom()

            try {
                if (trustStorePath.isNotEmpty() && trustStorePassword.isNotEmpty()) {
                    val truststoreFile = resourceLoader.getResource(trustStorePath).file

                    sslContextBuilder =
                            sslContextBuilder.loadTrustMaterial(truststoreFile, trustStorePassword.toCharArray())

                } else {
                    log.info("using default trust store")
                }
            } catch (ignored: Exception) {
                log.warn("Failed to load trust store file, falling back to default")
            }

            val sslBuilder = SSLConnectionSocketFactoryBuilder.create()
                .setSslContext(sslContextBuilder.build())
            if (skipSSLHostnameVerification) {
                // if we are running against our fake services, we don't want hostname verification
                sslBuilder.setHostnameVerifier(NoopHostnameVerifier.INSTANCE)
            }
            sslBuilder.build()
        } catch (e: GeneralSecurityException) {
            throw IllegalStateException(e)
        }
    }
}
