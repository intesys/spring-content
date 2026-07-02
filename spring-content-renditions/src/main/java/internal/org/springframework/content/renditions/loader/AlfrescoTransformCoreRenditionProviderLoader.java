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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.content.renditions.loader.ExternalRenditionProviderLoader;
import org.springframework.content.renditions.renderers.AlfrescoTransformCoreRenditionProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.InvalidMimeTypeException;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AlfrescoTransformCoreRenditionProviderLoader implements ExternalRenditionProviderLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(AlfrescoTransformCoreRenditionProviderLoader.class);
    private static final String ALFRESCO_TRANSFORM_CORE_CFG_URL = "/transform/config?configVersion=9999";
    private static final String ALFRESCO_TRANSFORM_CORE_HEALTH = "/actuator/health";
    private final String renditionProviderPrefix;
    private final String transformerUrl;
    private final String name;
    private final RestTemplate restTemplate;
    private final Integer maxRetries;
    private final Integer timeoutSeconds;
    private final DefaultListableBeanFactory registry;

    public AlfrescoTransformCoreRenditionProviderLoader(String transformerUrl, String name, Integer maxRetries, Integer timeoutSeconds,
        DefaultListableBeanFactory registry) {

        this.transformerUrl = transformerUrl;
        this.name = name;
        this.renditionProviderPrefix = name + "RenditionProviderImpl_";
        this.maxRetries = maxRetries;
        this.timeoutSeconds = timeoutSeconds;
        this.registry = registry;
        this.restTemplate = new RestTemplate();
    }

    @Override
    public void loadBeans() {

        // 1) Check service Alfresco Transform Core
        if (!waitForAlfrescoTransformCoreHealth()) {
            throw new IllegalStateException("Cannot instantiate rendition provider: external service is not running!");
        }

        // 2) Reading Json configuration
        AlfrescoTransformCoreConfig alfrescoTransformCoreConfig = getAlfrescoTransformCoreConfig();

        // 3) Creating an instance of RenditionProvider for each transformer found, group by source with all targets
        var transformers = alfrescoTransformCoreConfig != null ? alfrescoTransformCoreConfig.getTransformers() : null;
        if (transformers != null) {
            var sourceAndTargetMap = buildSourceAndTargetMap(transformers);
            registerRenditionProviderBeans(sourceAndTargetMap);
        }
        LOGGER.info("Loader for {} successfully loaded beans", name);
    }

    @Override
    public String getLoaderName() {

        return name;
    }

    private AlfrescoTransformCoreConfig getAlfrescoTransformCoreConfig() {

        String alfrescoTransformCoreConfigUrl = transformerUrl + ALFRESCO_TRANSFORM_CORE_CFG_URL;
        AlfrescoTransformCoreConfig alfrescoTransformCoreConfig = restTemplate.getForObject(alfrescoTransformCoreConfigUrl, AlfrescoTransformCoreConfig.class);
        LOGGER.debug("Retrieved AlfrescoTransformCore config from {}", alfrescoTransformCoreConfigUrl);
        return alfrescoTransformCoreConfig;
    }

    private boolean waitForAlfrescoTransformCoreHealth() {

        // Alfresco Transform Core health check: if after N attempts the service isn't UP, an exception is thrown
        String url = transformerUrl + ALFRESCO_TRANSFORM_CORE_HEALTH;
        for (int i = 0; i < maxRetries; i++) {
            try {
                ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
                if (response.getStatusCode() == HttpStatus.OK) {
                    return true;
                }
            }
            catch (RestClientException e) {
                LOGGER.debug("Health check attempt {} failed: {}", i + 1, e.getMessage());
            }
            try {
                Thread.sleep(timeoutSeconds * 1000L);
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    private Map<String, Set<String>> buildSourceAndTargetMap(List<AlfrescoTransformCoreConfig.Transformer> transformers) {

        Map<String, Set<String>> sourceAndTargetMap = new HashMap<>();

        for (var transformer : transformers) {
            var supportedSourceAndTargetList = transformer.getSupportedSourceAndTargetList();
            if (supportedSourceAndTargetList == null) {
                continue;
            }

            for (var supportedSourceAndTarget : supportedSourceAndTargetList) {
                // check if MimeType are correct
                String source = supportedSourceAndTarget.getSourceMediaType();
                String target = supportedSourceAndTarget.getTargetMediaType();
                try {
                    var sourceMimeType = MimeTypeUtils.parseMimeType(source);
                    var targetMimeType = MimeTypeUtils.parseMimeType(target);

                    String beanName = renditionProviderPrefix + sourceMimeType;
                    sourceAndTargetMap.computeIfAbsent(beanName, k -> new HashSet<>()).add(targetMimeType.toString());
                }
                catch (InvalidMimeTypeException invalidMimeTypeException) {
                    LOGGER.debug("Skipping transformer {} because source or target mime type is not valid: {}", transformer.getTransformerName(),
                                 invalidMimeTypeException.getMessage());
                }
            }
        }

        return sourceAndTargetMap;
    }

    private void registerRenditionProviderBeans(Map<String, Set<String>> sourceAndTargetMap) {

        var beans = registry.getBeanDefinitionNames();
        // remove bean not present
        for (String instantiatedBean : beans) {
            if (instantiatedBean.startsWith(renditionProviderPrefix) && !sourceAndTargetMap.containsKey(instantiatedBean)) {
                LOGGER.debug("Removing bean {} of type AlfrescoTransformCoreRenditionProvider...", instantiatedBean);
                registry.removeBeanDefinition(instantiatedBean);
            }
        }

        for (var entry : sourceAndTargetMap.entrySet()) {
            GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
            beanDefinition.setBeanClass(AlfrescoTransformCoreRenditionProvider.class);
            beanDefinition.getConstructorArgumentValues().addIndexedArgumentValue(0, transformerUrl);
            beanDefinition.getConstructorArgumentValues().addIndexedArgumentValue(1, entry.getKey().replace(renditionProviderPrefix, ""));
            beanDefinition.getConstructorArgumentValues().addIndexedArgumentValue(2, entry.getValue());
            beanDefinition.getConstructorArgumentValues().addIndexedArgumentValue(3, restTemplate);

            String beanName = entry.getKey();
            if (!registry.containsBeanDefinition(beanName)) {
                LOGGER.debug("Registering bean {} of type AlfrescoTransformCoreRenditionProvider...", beanName);
                registry.registerBeanDefinition(beanName, beanDefinition);
            }
            else {
                LOGGER.debug("Bean with name {} already registered, skipping...", beanName);
            }
        }
    }
}
