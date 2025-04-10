package org.springframework.content.renditions.config;

import internal.org.springframework.content.commons.renditions.RenditionServiceImpl;
import internal.org.springframework.content.renditions.loader.AlfrescoTransformCoreRenditionProviderLoader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.content.commons.renditions.RenditionProvider;
import org.springframework.content.commons.renditions.RenditionService;
import org.springframework.content.renditions.loader.ExternalRenditionProviderLoader;
import org.springframework.content.renditions.renderers.PdfToJpegRenderer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
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

    @Bean
    @Order(1)
    public List<ExternalRenditionProviderLoader> alfrescoTransformCoreRenditionProviderLoaders(Environment environment,
        @Value("${spring.content.renditions.loader.maxRetries:5}") Integer maxRetries,
        @Value("${spring.content.renditions.loader.timeoutSeconds:5}") Integer timeoutSeconds,
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

        return loaders;
    }

    @Bean
    @Order(2)
    public RenditionService renditionService(RenditionProvider... providers) {

        return new RenditionServiceImpl(providers);
    }
}
