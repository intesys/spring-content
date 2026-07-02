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

/**
 * Represents a loader responsible for managing and loading external rendition providers
 * into the application's context. Implementations of this interface are expected to
 * support the dynamic discovery and registration of rendition providers.
 *
 * @author marcobelligoli
 */
public interface ExternalRenditionProviderLoader {

    /**
     * Loads external rendition provider beans into the application's context.
     * Implementations of this method are responsible for discovering and
     * registering external rendition providers dynamically to make them available
     * for use within the application.
     */
    void loadBeans();

    /**
     * Retrieves the name of the loader.
     *
     * @return a string representing the name of the loader
     */
    String getLoaderName();
}
