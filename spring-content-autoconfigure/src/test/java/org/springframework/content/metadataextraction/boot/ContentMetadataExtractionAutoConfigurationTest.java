package org.springframework.content.metadataextraction.boot;

import internal.org.springframework.content.s3.boot.autoconfigure.S3ContentAutoConfiguration;
import internal.org.springframework.content.solr.boot.autoconfigure.SolrAutoConfiguration;
import internal.org.springframework.content.solr.boot.autoconfigure.SolrExtensionAutoConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.content.commons.metadataextraction.MetadataExtractionService;
import org.springframework.content.commons.renditions.Renderable;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.metadataextraction.extractors.DefaultMetadataExtractor;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableMBeanExport;
import org.springframework.jmx.support.RegistrationPolicy;
import org.springframework.support.TestEntity;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

@DisplayName("ContentMetadataExtractionAutoConfiguration")
public class ContentMetadataExtractionAutoConfigurationTest {

    @Test
    @DisplayName("given a default configuration should load the all metadata extractors")
    void shouldLoadAllExtractors() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.register(TestConfig.class);
        context.refresh();

        assertThat(context.getBean(MetadataExtractionService.class), is(not(nullValue())));
        assertThat(context.getBean(DefaultMetadataExtractor.class), is(not(nullValue())));

        context.close();
    }

    @Configuration
    @AutoConfigurationPackage
    @EnableAutoConfiguration(exclude = { SolrAutoConfiguration.class, SolrExtensionAutoConfiguration.class, S3ContentAutoConfiguration.class })
    @EnableMBeanExport(registration = RegistrationPolicy.IGNORE_EXISTING)
    public static class TestConfig {

    }

    public interface TestEntityContentStore extends ContentStore<TestEntity, String>, Renderable<TestEntity> {

    }
}
