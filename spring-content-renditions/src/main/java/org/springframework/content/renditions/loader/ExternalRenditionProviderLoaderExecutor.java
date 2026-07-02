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
package org.springframework.content.renditions.loader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Arrays;
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

    private final List<ExternalRenditionProviderLoader> loaders = new ArrayList<>();
    private final Boolean renditionsLoaderActive;

    @Autowired(required = false)
    public ExternalRenditionProviderLoaderExecutor(Boolean renditionsLoaderActive, ExternalRenditionProviderLoader... loaders) {

        this.renditionsLoaderActive = renditionsLoaderActive;
        this.loaders.addAll(Arrays.asList(loaders));
        loadExternalRenditionProviders();
    }

    public void loadExternalRenditionProviders() {

        if (Boolean.TRUE.equals(renditionsLoaderActive)) {
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
