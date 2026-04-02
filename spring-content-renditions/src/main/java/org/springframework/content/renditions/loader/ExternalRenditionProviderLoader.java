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
