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
package org.springframework.content.commons.metadataextraction;

import java.io.File;
import java.util.Map;

/**
 * A service that extracts metadata from a given file.
 * <p>
 * The extracted metadata will be returned as a map where the keys represent
 * the metadata property names and the values represent the respective metadata values.
 * </p>
 * <p>
 * This service retrieves all instances of {@link MetadataExtractor} present in the Spring context and,
 * for each of them, performs metadata extraction from the provided file.
 * </p>
 *
 * @author marcobelligoli
 */
public interface MetadataExtractionService {

    /**
     * Extracts metadata from the specified file.
     *
     * @param file the file from which metadata will be extracted
     * @return a map containing the extracted metadata, where keys represent
     * metadata property names, and values represent the respective metadata values
     */
    Map<String, Object> extractMetadata(File file);
}
