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
