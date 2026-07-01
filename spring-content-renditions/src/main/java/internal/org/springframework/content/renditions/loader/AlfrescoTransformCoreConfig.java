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
package internal.org.springframework.content.renditions.loader;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Configuration class for Alfresco Transform Core, encapsulating options and settings
 * related to transformers and their associated transform parameters.
 * The class defines the structure of transformer configurations for different use cases and media types.
 *
 * @author marcobelligoli
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class AlfrescoTransformCoreConfig {

    private TransformOption transformOptions;
    private List<Transformer> transformers;

    @Getter
    @Setter
    public static class Group {

        private boolean required;
        private List<TransformOption> transformOptions;
    }

    @Getter
    @Setter
    public static class ImageMagickOption {

        private Value value;
        private Group group;
    }

    @Getter
    @Setter
    public static class SupportedSourceAndTargetList {

        private String sourceMediaType;
        private String targetMediaType;
        private int maxSourceSizeBytes;
        private int priority;
    }

    @Getter
    @Setter
    public static class Transformer {

        private String transformerName;
        private String coreVersion;
        private List<String> transformOptions;
        private List<SupportedSourceAndTargetList> supportedSourceAndTargetList;
    }

    @Getter
    @Setter
    public static class TransformOption {

        private List<Value> pdfRendererOptions;
        private List<Value> archiveOptions;
        private List<ImageMagickOption> imageMagickOptions;
        private List<Value> imageToPdfOptions;
        private List<Value> metadataOptions;
        private List<Value> directAccessUrl;
        private List<Value> tikaOptions;
        private List<Value> pdfboxOptions;
        private List<Value> textToPdfOptions;
        private List<Value> stringOptions;
    }

    @Getter
    @Setter
    public static class Value {

        private boolean required;
        private String name;
    }
}
