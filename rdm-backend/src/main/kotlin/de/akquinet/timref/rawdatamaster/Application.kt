/*
 * Copyright (C) 2023 akquinet GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package de.akquinet.timref.rawdatamaster

import de.akquinet.timref.rawdatamaster.config.WebSecurityConfig
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.jsse.provider.BouncyCastleJsseProvider
import org.springframework.boot.Banner
import org.springframework.boot.WebApplicationType
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.FilterType
import org.springframework.core.env.Environment
import org.springframework.scheduling.annotation.EnableScheduling
import java.security.Security

@SpringBootApplication
// creating own configuration for parent application
// this is necessary because loading WebSecurityConfig by parent application would lead to error
// because it is started with WebApplicationType.NONE
@ComponentScan(
    basePackages =
    [
        "de.akquinet.timref.rawdatamaster.model",
        "de.akquinet.timref.rawdatamaster.repository",
        "de.akquinet.timref.rawdatamaster.service",
        "de.akquinet.timref.rawdatamaster.util",
        "de.akquinet.timref.rawdatamaster.config"
    ],
    excludeFilters = [ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = [WebSecurityConfig::class])]
)
@EnableScheduling
open class Application {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            // BC providers required for certificates and TLS cipher suites using brainpool curves
            Security.insertProviderAt(BouncyCastleProvider(), 1)
            Security.insertProviderAt(BouncyCastleJsseProvider(), 1)

            val parentBuilder = SpringApplicationBuilder(Application::class.java)
            parentBuilder.web(WebApplicationType.NONE).bannerMode(Banner.Mode.OFF)
            val context = parentBuilder.run(*args)
            val environment: Environment = context.environment
            val adminPort = environment.getProperty("rawdata-master.admin.port")
            val apiPort = environment.getProperty("rawdata-master.api.port")
            parentBuilder.child(ApiConfiguration::class.java).web(WebApplicationType.SERVLET)
                .properties("server.port:$apiPort").run(*args)
            parentBuilder.child(AdminConfiguration::class.java).web(WebApplicationType.SERVLET)
                .properties("server.port:$adminPort").run(*args)
        }

        @Configuration
        @ComponentScan(value = ["de.akquinet.timref.rawdatamaster.admin", "de.akquinet.timref.rawdatamaster.config"])
        @EnableAutoConfiguration
        internal open class AdminConfiguration

        @Configuration
        @ComponentScan(value = ["de.akquinet.timref.rawdatamaster.api", "de.akquinet.timref.rawdatamaster.config"])
        @EnableAutoConfiguration
        internal open class ApiConfiguration
    }
}
