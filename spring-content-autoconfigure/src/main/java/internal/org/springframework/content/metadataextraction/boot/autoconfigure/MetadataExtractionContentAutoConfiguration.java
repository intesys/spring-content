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
package internal.org.springframework.content.metadataextraction.boot.autoconfigure;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.content.metadataextraction.config.MetadataExtractionConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Autoconfiguration class for enabling metadata extraction functionality in a Spring Boot application.
 * <p>
 * This configuration class is activated when the {@link MetadataExtractionConfiguration} class is present in the classpath.
 * </p>
 * <p>
 * By including this class, metadata extraction features are automatically configured without requiring explicit registration
 * of the necessary components, simplifying integration into the application context.
 * </p>
 *
 * @author marcobelligoli
 */
@Configuration
@ConditionalOnClass(MetadataExtractionConfiguration.class)
@Import(MetadataExtractionConfiguration.class)
public class MetadataExtractionContentAutoConfiguration {

}
