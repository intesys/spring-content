package org.springframework.content.renditions.config;

import internal.org.springframework.content.commons.renditions.RenditionServiceImpl;
import internal.org.springframework.content.renditions.loader.AlfrescoTransformCoreRenditionProviderLoader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.content.commons.renditions.RenditionProvider;
import org.springframework.content.commons.renditions.RenditionService;
import org.springframework.content.renditions.loader.ExternalRenditionProviderLoader;
import org.springframework.content.renditions.renderers.PdfToJpegRenderer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Configuration
@ComponentScan(basePackageClasses = PdfToJpegRenderer.class)
public class RenditionsConfiguration {

    @Bean
    @Order(1)
    @ConditionalOnProperty(name = "alfresco.transform.core.url")
    public ExternalRenditionProviderLoader alfrescoTransformCoreRenditionProviderLoader(
        @Value("${alfresco.transform.core.url}") String alfrescoTransformCoreUrl) {

        return new AlfrescoTransformCoreRenditionProviderLoader(alfrescoTransformCoreUrl, "alfrescoTransformCore");
    }

    @Bean
    @Order(2)
    public RenditionService renditionService(RenditionProvider... providers) {

        return new RenditionServiceImpl(providers);
    }
}
