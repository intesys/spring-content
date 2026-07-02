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
package org.springframework.content.renditions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * <p>
 * Utility class providing methods to work with renditions and file resources.
 * This class is designed to provide functionality for converting input streams
 * into byte array resources with optional filename handling.
 * </p>
 * <p>
 * The class is non-instantiable and contains only static methods.
 * </p>
 *
 * @author marcobelligoli
 */
public class RenditionUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(RenditionUtils.class);

    private RenditionUtils() {

        throw new IllegalStateException("Utility class");
    }

    /**
     * Converts an {@link InputStream} into a {@link ByteArrayResource} with "default.dat"
     * as the default filename. This method is a utility for creating a resource object
     * encapsulating the content of the provided stream.
     *
     * @param inputStream the input stream to be converted into a {@link ByteArrayResource}.
     *                    It must not be null. If null, the method returns null.
     * @return a {@link ByteArrayResource} containing the byte data from the input stream
     * and a default filename of "default.dat". Returns null if the input stream is null.
     */
    public static ByteArrayResource getByteArrayResourceFromInputStream(InputStream inputStream) {

        return getByteArrayResource(inputStream, "default.dat");
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
            throw new RenditionException(e.getMessage());
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
