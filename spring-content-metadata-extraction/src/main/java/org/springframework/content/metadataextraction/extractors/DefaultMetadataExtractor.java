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

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.content.commons.metadataextraction.MetadataExtractor;
import org.springframework.content.metadataextraction.MetadataExtractionException;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;

/**
 * A service implementation of the {@link MetadataExtractor} interface that extracts base metadata
 * from a given file and provides it as a map of key-value pairs.
 * <p>
 * This implementation retrieves detailed metadata using Java NIO's file attributes and utility
 * classes. Metadata extracted by this class includes:
 * </p>
 * <ul>
 *     <li><b>fileName</b></li>
 *     <li><b>fileExtension</b></li>
 *     <li><b>size</b></li>
 *     <li><b>mimeType</b></li>
 *     <li><b>creationTime</b></li>
 *     <li><b>lastModifiedTime</b></li>
 *     <li><b>lastAccessTime</b></li>
 * </ul>
 * <p>
 * If an error occurs during metadata extraction, a {@link MetadataExtractionException} is
 * thrown, encapsulating the root cause of the error. This class also logs the extraction process
 * for debugging purposes.
 * </p>
 *
 * @author marcobelligoli
 */
@Service
public class DefaultMetadataExtractor implements MetadataExtractor {

    /**
     * Constructs a new {@code DefaultMetadataExtractor}.
     */
    public DefaultMetadataExtractor() {
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultMetadataExtractor.class);

    @Override
    public Map<String, Object> extractMetadata(File file) {

        LOGGER.debug("Starting extractMetadata...");
        Map<String, Object> metadata = new HashMap<>();
        try {
            if (file != null) {
                BasicFileAttributes attr = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
                metadata.put("fileName", file.getName());
                metadata.put("fileExtension", FilenameUtils.getExtension(file.getName()));
                metadata.put("size", attr.size());
                metadata.put("mimeType", Files.probeContentType(file.toPath()));
                metadata.put("creationTime", attr.creationTime().toString());
                metadata.put("lastModifiedTime", attr.lastModifiedTime().toString());
                metadata.put("lastAccessTime", attr.lastAccessTime().toString());
            }
        }
        catch (IOException e) {
            throw new MetadataExtractionException(e);
        }
        LOGGER.debug("extractMetadata done");
        return metadata;
    }
}
