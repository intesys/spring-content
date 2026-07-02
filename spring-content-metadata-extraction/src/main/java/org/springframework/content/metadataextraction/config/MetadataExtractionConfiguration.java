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
package org.springframework.content.metadataextraction.config;

import internal.org.springframework.content.commons.metadataextraction.MetadataExtractionServiceImpl;
import org.springframework.content.commons.metadataextraction.MetadataExtractionService;
import org.springframework.content.commons.metadataextraction.MetadataExtractor;
import org.springframework.content.metadataextraction.extractors.DefaultMetadataExtractor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration class for metadata extraction services.
 * <p>
 * This configuration enables component scanning to detect and register beans related to
 * metadata extraction, including implementations of {@link MetadataExtractor}.
 * It also defines a bean for the {@link MetadataExtractionService}, which aggregates
 * various {@link MetadataExtractor} implementations to perform metadata extraction
 * on a given file.
 * </p>
 *
 * @author marcobelligoli
 */
@Configuration
@ComponentScan(basePackageClasses = DefaultMetadataExtractor.class)
public class MetadataExtractionConfiguration {

    /**
     * Constructs a new {@code MetadataExtractionConfiguration}.
     */
    public MetadataExtractionConfiguration() {
    }

    /**
     * Creates a {@link MetadataExtractionService} bean that uses the provided
     * {@link MetadataExtractor} implementations.
     *
     * @param metadataExtractors the extractors to be used by the service.
     * @return the configured {@link MetadataExtractionService}.
     */
    @Bean
    public MetadataExtractionService metadataExtractionService(MetadataExtractor... metadataExtractors) {

        return new MetadataExtractionServiceImpl(metadataExtractors);
    }
}
