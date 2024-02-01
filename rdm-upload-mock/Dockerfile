# Copyright (C) 2023 akquinet GmbH
#
# Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.

FROM eclipse-temurin:17-jre

LABEL maintainer="TIMREF Maintainers"

WORKDIR /app

ARG JAR_FILE

HEALTHCHECK --interval=30s --timeout=5s --retries=5 CMD wget --quiet --tries=1 --spider http://localhost:8192

USER 65534

COPY ${JAR_FILE} app.jar

ENTRYPOINT [ "java", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", \
    "app.jar" \
]
