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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Utility class for metadata extraction operations.
 * This class provides methods for converting files or input streams
 * into {@link ByteArrayResource} objects.
 * <p>
 * It is designed to be used as a utility class and therefore cannot be instantiated.
 * </p>
 */
public class MetadataExtractionUtils {

    private MetadataExtractionUtils() {

        throw new IllegalStateException("Utility class");
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(MetadataExtractionUtils.class);

    /**
     * Converts the contents of the given file into a {@link ByteArrayResource}.
     * The method reads all data from the provided file and packages it
     * into a {@link ByteArrayResource} for further use.
     *
     * @param file The file to be converted into a {@link ByteArrayResource}.
     *             Must not be null. The method will attempt to read the file's contents.
     * @return A {@link ByteArrayResource} containing the file's byte data.
     * Throws a {@link MetadataExtractionException} if the file cannot be read.
     * @throws MetadataExtractionException If an I/O error occurs while accessing the file.
     */
    public static ByteArrayResource getByteArrayResourceFromFile(File file) {

        try (InputStream inputStream = new FileInputStream(file)) {
            return getByteArrayResource(inputStream, file.getName());
        }
        catch (IOException e) {
            throw new MetadataExtractionException(e);
        }
    }

    private static ByteArrayResource getByteArrayResource(InputStream inputStream, String fileName) {

        if (inputStream == null) {
            return null;
        }

        LOGGER.debug("Converting input stream in byte array resource...");

        if (inputStream instanceof FileInputStream fileInputStream) {
            File file;
            try {
                file = new File(fileInputStream.getFD().toString());
                fileName = file.getName();
            }
            catch (IOException e) {
                LOGGER.warn(e.getMessage());
            }
        }

        byte[] fileBytes;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
            fileBytes = baos.toByteArray();
        }
        catch (IOException e) {
            throw new MetadataExtractionException(e);
        }

        String finalFileName = fileName;
        var result = new ByteArrayResource(fileBytes) {

            @Override
            public String getFilename() {

                return finalFileName;
            }
        };

        LOGGER.debug("Conversion done");
        return result;
    }
}
