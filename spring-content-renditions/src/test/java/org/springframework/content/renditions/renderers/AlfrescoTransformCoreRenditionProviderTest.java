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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlfrescoTransformCoreRenditionProviderTest {

    private static final String TEST_URL = "http://test-alfresco.com";
    private static final String SOURCE_MIMETYPE = "application/pdf";
    private static final String TARGET_MIMETYPE = "image/png";

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private AlfrescoTransformCoreRenditionProvider renditionProvider;

    @BeforeEach
    void setUp() {

        renditionProvider = new AlfrescoTransformCoreRenditionProvider(TEST_URL, SOURCE_MIMETYPE, List.of(TARGET_MIMETYPE), restTemplate);
    }

    @Test
    void testConsumes() {

        assertEquals(SOURCE_MIMETYPE, renditionProvider.consumes());
    }

    @Test
    void testProduces() {

        assertArrayEquals(new String[] { TARGET_MIMETYPE }, renditionProvider.produces());
    }

    @Test
    void testConvertSuccess() {

        InputStream mockInputStream = new ByteArrayInputStream("test data".getBytes());
        InputStream mockResponseStream = new ByteArrayInputStream("converted data".getBytes());

        String expectedUrl = TEST_URL + "/transform";

        ResponseEntity<InputStream> responseEntity = new ResponseEntity<>(mockResponseStream, HttpStatus.OK);

        when(restTemplate.exchange(eq(expectedUrl), eq(HttpMethod.POST), any(HttpEntity.class), eq(InputStream.class))).thenReturn(responseEntity);

        InputStream resultStream = renditionProvider.convert(mockInputStream, TARGET_MIMETYPE);
        assertNotNull(resultStream);
    }

    @Test
    void testConvertFailure() {

        InputStream mockInputStream = new ByteArrayInputStream("test data".getBytes());
        String expectedUrl = TEST_URL + "/transform";

        when(restTemplate.exchange(eq(expectedUrl), eq(HttpMethod.POST), any(HttpEntity.class), eq(InputStream.class))).thenThrow(
            new RuntimeException("Transformation failed"));

        assertThrows(RuntimeException.class, () -> renditionProvider.convert(mockInputStream, TARGET_MIMETYPE));
    }
}
