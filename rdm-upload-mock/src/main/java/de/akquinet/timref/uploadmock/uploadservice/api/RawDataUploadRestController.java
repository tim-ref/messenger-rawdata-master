/*
 * Copyright (C) 2023 akquinet GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package de.akquinet.tim.uploadmock.uploadservice.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@RestController
public class RawDataUploadRestController {

    public static final Logger log = LoggerFactory.getLogger(RawDataUploadRestController.class);

    public static final String TEMP_DIR = System.getProperty("java.io.tmpdir");
    public static final String FILENAME_HEADER = "filename";
    public static final String ACCEPT_ENCODING_VALUE ="gzip, deflate-Encoding";

    @PostMapping(value = "/fileUpload", produces = {MediaType.TEXT_PLAIN_VALUE})
    public void fileUpload(HttpServletRequest request, HttpServletResponse response) throws IOException {
        log.trace("fileUpload()");
        long time = System.currentTimeMillis();

        log.debug("Header {}: {}", HttpHeaders.CONTENT_TYPE, request.getContentType());
        log.debug("Header {}: {}", HttpHeaders.CONTENT_LENGTH, request.getContentLengthLong());
        log.debug("Header {}: {}", HttpHeaders.ACCEPT_ENCODING, request.getHeader(HttpHeaders.ACCEPT_ENCODING));
        log.debug("Header {}: {}", FILENAME_HEADER, request.getHeader(FILENAME_HEADER));

        if (!MediaType.APPLICATION_OCTET_STREAM_VALUE.equals(request.getContentType())
            || request.getContentLengthLong() <= 0
            // || !ACCEPT_ENCODING_VALUE.equals(request.getHeader(HttpHeaders.ACCEPT_ENCODING))
            || request.getHeader(FILENAME_HEADER) == null) {
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            log.info("Upload rejected for file '{}' with length: {}", request.getHeader(FILENAME_HEADER), request.getContentLengthLong());
            return;
        }

        File file = new File(TEMP_DIR, request.getHeader(FILENAME_HEADER) + ".uploaded");
        if (!file.getCanonicalPath().startsWith(TEMP_DIR)) {
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            log.info("Invalid file path '{}'", request.getHeader(FILENAME_HEADER));
            return;
        }

        try (InputStream inputStream = new InputStream() {
                private long bytesRead;
                private final InputStream is = request.getInputStream();

                @Override
                public int read() throws IOException {
                    int c = this.is.read();
                    if (c != -1) {
                        this.bytesRead++;
                        if (this.bytesRead % 1000 == 0) {
                            log.debug("Reading stream, bytesRead: {}", this.bytesRead);
                        }
                    } else {
                        log.debug("End of stream, bytes read: {}", this.bytesRead);
                    }
                    return c;
                }

            @Override
                public void close() {
                    log.debug("Stream closed, bytes read: {}", this.bytesRead);
                }
            }) {
            try (OutputStream outputStream = new FileOutputStream(file, false)) {
                inputStream.transferTo(outputStream);
            }
        } catch (Exception e) {
            log.error("Upload failed for file {}", file, e);
            throw e;
        }

        log.info("Uploaded file {} in {} ms, length: {}", file, System.currentTimeMillis() - time, file.length());
    }
}
