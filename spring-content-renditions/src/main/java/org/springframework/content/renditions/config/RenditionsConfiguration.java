package org.springframework.content.renditions.config;

import internal.org.springframework.content.commons.renditions.RenditionServiceImpl;
import internal.org.springframework.content.renditions.loader.AlfrescoTransformCoreRenditionProviderLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.content.commons.renditions.RenditionProvider;
import org.springframework.content.commons.renditions.RenditionService;
import org.springframework.content.renditions.loader.ExternalRenditionProviderLoader;
import org.springframework.content.renditions.loader.ExternalRenditionProviderLoaderExecutor;
import org.springframework.content.renditions.renderers.PdfToJpegRenderer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.annotation.Order;

@Configuration
@ComponentScan(basePackageClasses = PdfToJpegRenderer.class)
@PropertySource("classpath:renditions.properties")
public class RenditionsConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(RenditionsConfiguration.class);

    @Bean
    @Order(1)
    @ConditionalOnProperty(name = "alfresco.transform.core.url")
    public ExternalRenditionProviderLoader alfrescoTransformCoreRenditionProviderLoader(
        @Value("${alfresco.transform.core.url}") String alfrescoTransformCoreUrl,
        @Value("${spring.content.renditions.loaders.maxRetries}") Integer maxRetries,
        @Value("${spring.content.renditions.loaders.timeoutSeconds}") Integer timeoutSeconds, DefaultListableBeanFactory registry) {

        LOGGER.debug("Registering AlfrescoTransformCoreRenditionProviderLoader...");
        return new AlfrescoTransformCoreRenditionProviderLoader(alfrescoTransformCoreUrl, "AlfrescoTransformCore", maxRetries, timeoutSeconds, registry);
    }

    @Bean
    @Order(2)
    public ExternalRenditionProviderLoaderExecutor externalRenditionProviderLoaderExecutor(
        @Value("${spring.content.renditions.loaders.active}") Boolean renditionsLoaderActive, ExternalRenditionProviderLoader... loaders) {

        LOGGER.debug("Registering ExternalRenditionProviderLoaderExecutor...");
        return new ExternalRenditionProviderLoaderExecutor(renditionsLoaderActive, loaders);
    }

    @Bean
    @DependsOn("externalRenditionProviderLoaderExecutor")
    public RenditionService renditionService(RenditionProvider... providers) {

        return new RenditionServiceImpl(providers);
    }
}
