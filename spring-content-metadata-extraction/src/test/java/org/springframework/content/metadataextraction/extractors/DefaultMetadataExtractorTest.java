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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.content.metadataextraction.MetadataExtractionException;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit test class for the {@link DefaultMetadataExtractor}.
 *
 * <p>This class contains test cases to validate the functionality of the
 * {@link DefaultMetadataExtractor#extractMetadata(File)} method.</p>
 *
 * <p>The following test scenarios are covered:</p>
 * <ul>
 *   <li>Test the extraction of metadata from a valid file.</li>
 *   <li>Test the behavior when a null file is provided.</li>
 *   <li>Test the handling of {@link IOException} during metadata extraction.</li>
 * </ul>
 *
 * @author marcobelligoli
 */
@DisplayName("DefaultMetadataExtractor")
public class DefaultMetadataExtractorTest {

    private final DefaultMetadataExtractor metadataExtractor = new DefaultMetadataExtractor();

    @Test
    @DisplayName("should extract metadata from a valid file")
    public void testExtractMetadataWithValidFile()
        throws URISyntaxException {

        File input = getFile();

        var result = metadataExtractor.extractMetadata(input);

        assertNotNull(result);
        assertNotNull(result.get("fileName"));
        assertNotNull(result.get("lastModifiedTime"));
        assertNotNull(result.get("lastAccessTime"));
        assertNotNull(result.get("size"));
        assertNotNull(result.get("mimeType"));
        assertNotNull(result.get("creationTime"));
        assertNotNull(result.get("fileExtension"));
    }

    @Test
    @DisplayName("should return empty metadata when file is null")
    public void testExtractMetadataWithNullFile() {

        Map<String, Object> metadata = metadataExtractor.extractMetadata(null);
        assertTrue(metadata.isEmpty());
    }

    @Test
    @DisplayName("should throw MetadataExtractionException when an IOException occurs")
    public void testExtractMetadataWithIOException() {

        File nonExistentFile = new File("/path/to/nonexistent/file");

        assertThrows(MetadataExtractionException.class, () -> metadataExtractor.extractMetadata(nonExistentFile));
    }

    private static File getFile()
        throws URISyntaxException {

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL resource = classLoader.getResource("sample.jpeg");
        assert resource != null;
        return new File(resource.toURI());
    }
}
