/*
 * Copyright (c) 2026 Intesys S.r.l. and the Spring Content contributors
 *
 * This file is part of Spring Content.
 *
 * Spring Content is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Spring Content is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Spring Content.  If not, see <https://www.gnu.org/licenses/>.
 */
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
