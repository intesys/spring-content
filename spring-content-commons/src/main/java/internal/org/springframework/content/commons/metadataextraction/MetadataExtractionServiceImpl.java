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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.metadataextraction.MetadataExtractionService;
import org.springframework.content.commons.metadataextraction.MetadataExtractor;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of the {@link MetadataExtractionService} interface.
 *
 * @author marcobelligoli
 */
public class MetadataExtractionServiceImpl implements MetadataExtractionService {

    private final List<MetadataExtractor> metadataExtractorList = new ArrayList<>();

    @Autowired(required = false)
    public MetadataExtractionServiceImpl(MetadataExtractor... metadataExtractors) {

        Collections.addAll(this.metadataExtractorList, metadataExtractors);
    }

    @Override
    public Map<String, Object> extractMetadata(File file) {

        Map<String, Object> fullMetadataMap = new HashMap<>();
        for (var metadataExtractor : metadataExtractorList) {
            fullMetadataMap.putAll(metadataExtractor.extractMetadata(file));
        }
        return fullMetadataMap;
    }
}
