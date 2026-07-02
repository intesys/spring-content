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
package org.springframework.content.metadataextraction;

import org.springframework.content.commons.metadataextraction.MetadataExtractor;

/**
 * Signals an error during metadata extraction.
 * <p>
 * This exception is typically thrown by components that implement
 * the {@link MetadataExtractor}
 * interface when an error occurs while attempting to extract metadata from a file.
 * Encapsulates the root cause of the error.
 * </p>
 *
 * @author marcobelligoli
 */
public class MetadataExtractionException extends RuntimeException {

    /**
     * Constructs a {@code MetadataExtractionException} with the specified cause.
     *
     * @param cause the cause of the exception.
     */
    public MetadataExtractionException(Throwable cause) {

        super(cause);
    }
}
