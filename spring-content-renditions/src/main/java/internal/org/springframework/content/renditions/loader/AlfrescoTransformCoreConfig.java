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
