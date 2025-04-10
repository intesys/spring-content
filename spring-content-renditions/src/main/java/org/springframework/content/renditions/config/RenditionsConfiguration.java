package org.springframework.content.renditions.config;

import internal.org.springframework.content.commons.renditions.RenditionServiceImpl;
import internal.org.springframework.content.renditions.loader.AlfrescoTransformCoreRenditionProviderLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.content.commons.renditions.RenditionProvider;
import org.springframework.content.commons.renditions.RenditionService;
import org.springframework.content.renditions.loader.ExternalRenditionProviderLoader;
import org.springframework.content.renditions.loader.ExternalRenditionProviderLoaderExecutor;
import org.springframework.content.renditions.renderers.PdfToJpegRenderer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
@ComponentScan(basePackageClasses = PdfToJpegRenderer.class)
public class RenditionsConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(RenditionsConfiguration.class);

    @Bean
    @Order(1)
    public List<ExternalRenditionProviderLoader> alfrescoTransformCoreRenditionProviderLoaders(Environment environment,
        @Value("${spring.content.renditions.loaders.maxRetries:5}") Integer maxRetries,
        @Value("${spring.content.renditions.loaders.timeoutSeconds:5}") Integer timeoutSeconds,
        DefaultListableBeanFactory registry) {

        List<ExternalRenditionProviderLoader> loaders = new ArrayList<>();

        Map<String, Object> dynamicProperties = ((AbstractEnvironment) environment).getPropertySources().stream()
                                                                                   .filter(MapPropertySource.class::isInstance)
                                                                                   .map(ps -> ((MapPropertySource) ps).getSource())
                                                                                   .flatMap(map -> map.entrySet().stream())
                                                                                   .filter(entry -> entry.getKey().matches("^[^.]+\\.transform\\.core\\.url$"))
                                                                                   .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        if (dynamicProperties.isEmpty()) {
            return Collections.emptyList();
        }

        for (Map.Entry<String, Object> entry : dynamicProperties.entrySet()) {
            String key = entry.getKey();
            String url = entry.getValue().toString();
            String name = key.substring(0, key.indexOf(".transform.core.url"));

            AlfrescoTransformCoreRenditionProviderLoader loader = new AlfrescoTransformCoreRenditionProviderLoader(url, name + "TransformCore", maxRetries,
                                                                                                                   timeoutSeconds, registry);
            loaders.add(loader);
        }

        LOGGER.debug("Found {} alfresco transform core loaders", loaders.size());
        return loaders;
    }

    @Bean
    @DependsOn("alfrescoTransformCoreRenditionProviderLoaders")
    public ExternalRenditionProviderLoaderExecutor externalRenditionProviderLoaderExecutor(List<ExternalRenditionProviderLoader> loaders,
        @Value("${spring.content.renditions.loaders.active:true}") Boolean renditionsLoaderActive) {

        return new ExternalRenditionProviderLoaderExecutor(loaders, renditionsLoaderActive);
    }

    @Bean
    @DependsOn("externalRenditionProviderLoaderExecutor")
    public RenditionService renditionService(RenditionProvider... providers) {

        return new RenditionServiceImpl(providers);
    }
}
