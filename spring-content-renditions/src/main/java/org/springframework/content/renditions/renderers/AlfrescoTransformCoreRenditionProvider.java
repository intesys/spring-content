/* Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License. */
package org.springframework.content.renditions.renderers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.content.commons.renditions.RenditionProvider;
import org.springframework.content.renditions.RenditionUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;

/**
 * <p>
 * This class is an implementation of the {@link RenditionProvider} interface
 * and provides functionality for converting one type of content to another
 * using the Alfresco Transform Core service.
 * </p>
 * <p>
 * The transformation process involves sending input content to the Alfresco
 * Transform Core service and retrieving the converted content as an
 * {@link InputStream}.
 * </p>
 * <p>
 * The transformation supports a specific source MIME type and a set of target
 * MIME types as defined during the instantiation of the class.
 * </p>
 * Thread safety:
 * Instances of this class are immutable and thread-safe provided the
 * dependencies passed to the constructor are thread-safe.
 * <p>
 * Constructor Details:
 * - Accepts the URL of the Alfresco Transform Core service, the source MIME
 * type, a list of target MIME types, and a Spring RestTemplate instance
 * for HTTP communication.
 * </p>
 *
 * @author marcobelligoli
 */
public final class AlfrescoTransformCoreRenditionProvider implements RenditionProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(AlfrescoTransformCoreRenditionProvider.class);
    private static final String ALFRESCO_TRANSFORM_CORE_TRANSFORM_URL = "/transform";
    private final String alfrescoTransformCoreUrl;
    private final String source;
    private final List<String> targets;
    private final RestTemplate restTemplate;

    public AlfrescoTransformCoreRenditionProvider(String alfrescoTransformCoreUrl, String source, List<String> targets, RestTemplate restTemplate) {

        this.alfrescoTransformCoreUrl = alfrescoTransformCoreUrl;
        this.source = source;
        this.targets = targets;
        this.restTemplate = restTemplate;
    }

    @Override
    public String consumes() {

        return source;
    }

    @Override
    public String[] produces() {

        return targets.toArray(String[]::new);
    }

    @Override
    public InputStream convert(InputStream inputStream, String toMimeType) {

        String url = alfrescoTransformCoreUrl + ALFRESCO_TRANSFORM_CORE_TRANSFORM_URL;

        MultiValueMap<String, Object> requestParams = new LinkedMultiValueMap<>();
        requestParams.put("file", Collections.singletonList(RenditionUtils.getByteArrayResourceFromInputStream(inputStream)));
        requestParams.put("sourceMimetype", Collections.singletonList(source));
        requestParams.put("targetMimetype", Collections.singletonList(toMimeType));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.MULTIPART_FORM_DATA);

        LOGGER.debug("Calling POST {} with params {}...", url, requestParams);
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(requestParams, headers);
        ResponseEntity<InputStream> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, InputStream.class);
        LOGGER.debug("POST to {} done", url);

        return response.getBody();
    }
}
