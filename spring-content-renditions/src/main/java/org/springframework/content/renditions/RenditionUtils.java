/* Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License. */
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
