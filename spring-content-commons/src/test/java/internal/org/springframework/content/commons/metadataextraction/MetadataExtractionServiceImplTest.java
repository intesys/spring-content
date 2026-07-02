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
package internal.org.springframework.content.commons.metadataextraction;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.content.commons.metadataextraction.MetadataExtractor;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * This class tests the `extractMetadata` method in the `MetadataExtractionServiceImpl` class.
 * The method is responsible for iterating over multiple `MetadataExtractor` instances and aggregating
 * the metadata they extract into a single map.
 *
 * @author marcobelligoli
 */
public class MetadataExtractionServiceImplTest {

    @Test
    public void testExtractMetadataWithSingleExtractor() {

        File mockFile = Mockito.mock(File.class);
        MetadataExtractor mockExtractor = Mockito.mock(MetadataExtractor.class);
        MetadataExtractionServiceImpl service = new MetadataExtractionServiceImpl(mockExtractor);

        Map<String, Object> mockMetadata = new HashMap<>();
        mockMetadata.put("author", "John Doe");
        Mockito.when(mockExtractor.extractMetadata(mockFile)).thenReturn(mockMetadata);

        Map<String, Object> result = service.extractMetadata(mockFile);

        Assert.assertEquals(1, result.size());
        Assert.assertEquals("John Doe", result.get("author"));
        Mockito.verify(mockExtractor).extractMetadata(mockFile);
    }

    @Test
    public void testExtractMetadataWithMultipleExtractors() {

        File mockFile = Mockito.mock(File.class);
        MetadataExtractor mockExtractor1 = Mockito.mock(MetadataExtractor.class);
        MetadataExtractor mockExtractor2 = Mockito.mock(MetadataExtractor.class);
        MetadataExtractionServiceImpl service = new MetadataExtractionServiceImpl(mockExtractor1, mockExtractor2);

        Map<String, Object> mockMetadata1 = new HashMap<>();
        mockMetadata1.put("author", "John Doe");
        Mockito.when(mockExtractor1.extractMetadata(mockFile)).thenReturn(mockMetadata1);

        Map<String, Object> mockMetadata2 = new HashMap<>();
        mockMetadata2.put("title", "Sample Document");
        Mockito.when(mockExtractor2.extractMetadata(mockFile)).thenReturn(mockMetadata2);

        Map<String, Object> result = service.extractMetadata(mockFile);

        Assert.assertEquals(2, result.size());
        Assert.assertEquals("John Doe", result.get("author"));
        Assert.assertEquals("Sample Document", result.get("title"));
        Mockito.verify(mockExtractor1).extractMetadata(mockFile);
        Mockito.verify(mockExtractor2).extractMetadata(mockFile);
    }

    @Test
    public void testExtractMetadataWithConflictingKeys() {

        File mockFile = Mockito.mock(File.class);
        MetadataExtractor mockExtractor1 = Mockito.mock(MetadataExtractor.class);
        MetadataExtractor mockExtractor2 = Mockito.mock(MetadataExtractor.class);
        MetadataExtractionServiceImpl service = new MetadataExtractionServiceImpl(mockExtractor1, mockExtractor2);

        Map<String, Object> mockMetadata1 = new HashMap<>();
        mockMetadata1.put("key", "value1");
        Mockito.when(mockExtractor1.extractMetadata(mockFile)).thenReturn(mockMetadata1);

        Map<String, Object> mockMetadata2 = new HashMap<>();
        mockMetadata2.put("key", "value2");
        Mockito.when(mockExtractor2.extractMetadata(mockFile)).thenReturn(mockMetadata2);

        Map<String, Object> result = service.extractMetadata(mockFile);

        Assert.assertEquals(1, result.size());
        Assert.assertEquals("value2", result.get("key")); // Last extractor's value overrides
        Mockito.verify(mockExtractor1).extractMetadata(mockFile);
        Mockito.verify(mockExtractor2).extractMetadata(mockFile);
    }

    @Test
    public void testExtractMetadataWithNoExtractors() {

        File mockFile = Mockito.mock(File.class);
        MetadataExtractionServiceImpl service = new MetadataExtractionServiceImpl();

        Map<String, Object> result = service.extractMetadata(mockFile);

        Assert.assertTrue(result.isEmpty());
    }

    @Test
    public void testExtractMetadataWithEmptyMetadataFromExtractor() {

        File mockFile = Mockito.mock(File.class);
        MetadataExtractor mockExtractor = Mockito.mock(MetadataExtractor.class);
        MetadataExtractionServiceImpl service = new MetadataExtractionServiceImpl(mockExtractor);

        Mockito.when(mockExtractor.extractMetadata(mockFile)).thenReturn(new HashMap<>());

        Map<String, Object> result = service.extractMetadata(mockFile);

        Assert.assertTrue(result.isEmpty());
        Mockito.verify(mockExtractor).extractMetadata(mockFile);
    }
}