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
package internal.org.springframework.content.renditions.loader;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlfrescoTransformCoreRenditionProviderLoaderTest {

    private static final int MAX_RETRIES = 2;
    private static final String TEST_URL = "http://test-alfresco.com";
    private static final String LOADER_NAME = "TestLoader";
    private static final int TIMEOUT_SECONDS = 1;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private DefaultListableBeanFactory registry;

    private AlfrescoTransformCoreRenditionProviderLoader loader;

    @BeforeEach
    void setUp() {

        loader = new AlfrescoTransformCoreRenditionProviderLoader(TEST_URL, LOADER_NAME, MAX_RETRIES, TIMEOUT_SECONDS, registry);
        ReflectionTestUtils.setField(loader, "restTemplate", restTemplate);
    }

    @Test
    void loadBeansSuccessful() {

        AlfrescoTransformCoreConfig mockConfig = mock(AlfrescoTransformCoreConfig.class);
        when(restTemplate.getForObject(anyString(), eq(AlfrescoTransformCoreConfig.class))).thenReturn(mockConfig);
        when(mockConfig.getTransformers()).thenReturn(getTransformers());
        when(restTemplate.getForEntity(anyString(), eq(String.class))).thenReturn(new ResponseEntity<>("OK", HttpStatus.OK));
        when(registry.getBeanDefinitionNames()).thenReturn(new String[0]);

        assertDoesNotThrow(() -> loader.loadBeans());
    }

    @Test
    void loadBeansFailsWhenServiceUnavailable() {

        when(restTemplate.getForEntity(anyString(), eq(String.class))).thenReturn(new ResponseEntity<>("Service Down", HttpStatus.SERVICE_UNAVAILABLE));

        assertThrows(IllegalStateException.class, () -> loader.loadBeans());
    }

    @Test
    void getLoaderNameReturnsCorrectName() {

        assertEquals(LOADER_NAME, loader.getLoaderName());
    }

    private List<AlfrescoTransformCoreConfig.Transformer> getTransformers() {

        List<AlfrescoTransformCoreConfig.Transformer> res = new ArrayList<>();

        var transf1 = new AlfrescoTransformCoreConfig.Transformer();
        transf1.setTransformerName("t1");
        var st1 = new AlfrescoTransformCoreConfig.SupportedSourceAndTargetList();
        st1.setSourceMediaType("text/html");
        st1.setTargetMediaType("application/pdf");
        transf1.setSupportedSourceAndTargetList(List.of(st1));

        var transf2 = new AlfrescoTransformCoreConfig.Transformer();
        transf2.setTransformerName("t2");
        var st2 = new AlfrescoTransformCoreConfig.SupportedSourceAndTargetList();
        st1.setSourceMediaType("text/plain");
        st1.setTargetMediaType("application/pdf");
        transf2.setSupportedSourceAndTargetList(List.of(st2));

        res.add(transf1);
        res.add(transf2);
        return res;
    }
}
