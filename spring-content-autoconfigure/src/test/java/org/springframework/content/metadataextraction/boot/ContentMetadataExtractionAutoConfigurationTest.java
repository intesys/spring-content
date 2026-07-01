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
package org.springframework.content.metadataextraction.boot;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jConfiguration;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;
import internal.org.springframework.content.s3.boot.autoconfigure.S3ContentAutoConfiguration;
import internal.org.springframework.content.solr.boot.autoconfigure.SolrAutoConfiguration;
import internal.org.springframework.content.solr.boot.autoconfigure.SolrExtensionAutoConfiguration;
import org.junit.runner.RunWith;
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

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Test class for verifying the functionality of the ContentMetadataExtractionAutoConfiguration.
 *
 * @author marcobelligoli
 */
@RunWith(Ginkgo4jRunner.class)
@Ginkgo4jConfiguration(threads = 1)
public class ContentMetadataExtractionAutoConfigurationTest {

    {
        Describe("ContentMetadataExtractionAutoConfiguration",
                 () -> Context("given a default configuration", () -> It("should load the all metadata extractors", () -> {

                     AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
                     context.register(TestConfig.class);
                     context.refresh();

                     assertThat(context.getBean(MetadataExtractionService.class), is(not(nullValue())));
                     assertThat(context.getBean(DefaultMetadataExtractor.class), is(not(nullValue())));

                     context.close();
                 })));
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
