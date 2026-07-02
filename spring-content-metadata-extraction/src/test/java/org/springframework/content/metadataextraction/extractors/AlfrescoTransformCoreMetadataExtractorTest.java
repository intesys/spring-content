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
package org.springframework.content.metadataextraction.extractors;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.content.metadataextraction.MetadataExtractionException;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Test class for validating the functionality of the {@link AlfrescoTransformCoreMetadataExtractor}.
 * This test suite contains unit tests to ensure correct metadata extraction behavior,
 * proper handling of errors, and correct communication with the Alfresco Transform Core service.
 * <p>
 * It sets up a mock wire server to simulate the behavior of the Alfresco Transform Core service.
 * Additionally, it verifies both successful metadata extraction and error scenarios.
 * </p>
 */
class AlfrescoTransformCoreMetadataExtractorTest {

    private static final String ALFRESCO_TRANSFORM_CORE_HOST = "localhost";
    private static WireMockServer wireMockServer;
    private static AlfrescoTransformCoreMetadataExtractor metadataExtractor;

    @BeforeAll
    static void setup() {

        wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMockServer.start();
        var port = wireMockServer.port();
        WireMock.configureFor(ALFRESCO_TRANSFORM_CORE_HOST, port);
        var baseUrl = String.format("http://%s:%d", ALFRESCO_TRANSFORM_CORE_HOST, port);
        metadataExtractor = new AlfrescoTransformCoreMetadataExtractor(baseUrl);
    }

    @AfterAll
    static void tearDown() {

        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    private static void mockAlfrescoTransformCoreResponse(String response) {

        stubFor(post(urlEqualTo("/transform")).willReturn(aResponse().withHeader("Content-Type", "application/json").withBody(response)));
    }

    @Test
    void testExtractMetadata()
        throws URISyntaxException {

        File input = getFile();

        mockAlfrescoTransformCoreResponse(
            "{\"{http://www.alfresco.org/model/content/1.0}author\":\"John Doe\",\"{http://www.alfresco.org/model/content/1.0}created\":\"2024-04-16T07:39:22Z\",\"{http://www.alfresco.org/model/content/1.0}title\":null}");

        var result = metadataExtractor.extractMetadata(input);

        assertNotNull(result);
        assertEquals("John Doe", result.get("{http://www.alfresco.org/model/content/1.0}author"));
        assertEquals("2024-04-16T07:39:22Z", result.get("{http://www.alfresco.org/model/content/1.0}created"));
        assertNull(result.get("{http://www.alfresco.org/model/content/1.0}title"));
    }

    @Test
    void testExtractMetadataWithError()
        throws URISyntaxException {

        File input = getFile();

        mockAlfrescoTransformCoreResponse("{\"{http://www.alfresco.org/model/content/1.0}title:null}");

        assertThrows(MetadataExtractionException.class, () -> metadataExtractor.extractMetadata(input));
    }

    private static File getFile()
        throws URISyntaxException {

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL resource = classLoader.getResource("sample.jpeg");
        assert resource != null;
        return new File(resource.toURI());
    }
}
