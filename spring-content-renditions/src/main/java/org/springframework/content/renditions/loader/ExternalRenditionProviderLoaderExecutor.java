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
package org.springframework.content.renditions.loader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * <b>
 * The ExternalRenditionProviderLoaderExecutor class is responsible for managing
 * the initialization and execution of multiple {@link ExternalRenditionProviderLoader}
 * instances. It ensures that the external rendition provider beans are dynamically
 * loaded into the application's context based on the active configuration and
 * provided loaders.
 * </b>
 * <b>
 * The list of loaders and the active flag are supplied during instantiation of
 * the class, making it configurable and adaptable to various application contexts.
 * </b>
 */
public class ExternalRenditionProviderLoaderExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExternalRenditionProviderLoaderExecutor.class);

    private final List<ExternalRenditionProviderLoader> loaders;
    private final Boolean renditionsLoaderActive;

    public ExternalRenditionProviderLoaderExecutor(List<ExternalRenditionProviderLoader> loaders, Boolean renditionsLoaderActive) {

        this.loaders = loaders;
        this.renditionsLoaderActive = renditionsLoaderActive;
        loadExternalRenditionProviders();
    }

    public void loadExternalRenditionProviders() {

        if (Boolean.TRUE.equals(renditionsLoaderActive) && loaders != null) {
            for (var loader : loaders) {
                try {
                    LOGGER.debug("Loading beans for loader {}...", loader.getLoaderName());
                    loader.loadBeans();
                }
                catch (Exception ex) {
                    LOGGER.warn("Error loading beans for loader {} -> [{}]", loader.getLoaderName(), ex.getMessage());
                }
            }
        }
    }
}
