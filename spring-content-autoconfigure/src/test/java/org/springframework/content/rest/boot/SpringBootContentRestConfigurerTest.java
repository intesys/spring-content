package org.springframework.content.rest.boot;

import internal.org.springframework.content.rest.boot.autoconfigure.ContentRestAutoConfiguration.ContentRestProperties;
import internal.org.springframework.content.rest.boot.autoconfigure.ContentRestAutoConfiguration.ContentRestProperties.ShortcutRequestMappings;
import internal.org.springframework.content.rest.boot.autoconfigure.SpringBootContentRestConfigurer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.content.rest.config.RestConfiguration;
import org.springframework.http.MediaType;

import java.net.URI;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("SpringBootContentRestConfigurer")
public class SpringBootContentRestConfigurerTest {

    private SpringBootContentRestConfigurer configurer;
    private ContentRestProperties properties;

    // mocks
    private RestConfiguration restConfig;
    private RestConfiguration.Exclusions exclusions;

    @BeforeEach
    void setUp() {
        properties = new ContentRestProperties();
        restConfig = mock(RestConfiguration.class);
        exclusions = mock(RestConfiguration.Exclusions.class);
        when(restConfig.shortcutExclusions()).thenReturn(exclusions);
    }

    private void executeConfigure() {
        configurer = new SpringBootContentRestConfigurer(properties);
        configurer.configure(restConfig);
    }

    @Nested
    @DisplayName("#configure")
    class Configure {

        @Nested
        @DisplayName("given a base uri property")
        class BaseUriProperty {
            @BeforeEach
            void setUp() {
                properties.setBaseUri(URI.create("/test"));
                executeConfigure();
            }
            @Test
            @DisplayName("should set the property on the RestConfiguration")
            void shouldSetProperty() {
                verify(restConfig).setBaseUri(eq(properties.getBaseUri()));
            }
        }

        @Nested
        @DisplayName("given a fullyQualifiedLinks property setting")
        class FullyQualifiedLinks {
            @BeforeEach
            void setUp() {
                properties.setFullyQualifiedLinks(true);
                executeConfigure();
            }
            @Test
            @DisplayName("should set the property on the RestConfiguration")
            void shouldSetProperty() {
                verify(restConfig).setFullyQualifiedLinks(eq(true));
            }
        }

        @Nested
        @DisplayName("given disabled shortcut request mappings")
        class DisabledShortcutMappings {
            @BeforeEach
            void setUp() {
                ShortcutRequestMappings mappings = new ShortcutRequestMappings();
                mappings.setDisabled(true);
                properties.setShortcutRequestMappings(mappings);
                executeConfigure();
            }
            @Test
            @DisplayName("should diabled the shortcut links")
            void shouldDisableLinks() {
                verify(restConfig).setShortcutLinks(false);
            }
        }

        @Nested
        @DisplayName("given excluded shortcut request mappings")
        class ExcludedShortcutMappings {
            @BeforeEach
            void setUp() {
                ShortcutRequestMappings mappings = new ShortcutRequestMappings();
                mappings.setExcludes("GET=a/b,c/d:PUT=*/*");
                properties.setShortcutRequestMappings(mappings);
                executeConfigure();
            }
            @Test
            @DisplayName("should set the exclusions property on the RestConfiguration")
            void shouldSetExclusions() {
                verify(exclusions).exclude("GET", MediaType.parseMediaType("a/b"));
                verify(exclusions).exclude("GET", MediaType.parseMediaType("c/d"));
                verify(exclusions).exclude("PUT", MediaType.parseMediaType("*/*"));
            }
        }

        @Nested
        @DisplayName("given empty excluded shortcut request mapping")
        class EmptyExcludedMappings {
            @BeforeEach
            void setUp() {
                ShortcutRequestMappings mappings = new ShortcutRequestMappings();
                mappings.setExcludes("");
                properties.setShortcutRequestMappings(mappings);
                executeConfigure();
            }
            @Test
            @DisplayName("should not set the exclusions property on the RestConfiguration")
            void shouldNotSetExclusions() {
                verify(exclusions, never()).exclude(any(), any());
            }
        }

        @Nested
        @DisplayName("given empty excluded shortcut GET request mapping")
        class EmptyExcludedGetMappings {
            @BeforeEach
            void setUp() {
                ShortcutRequestMappings mappings = new ShortcutRequestMappings();
                mappings.setExcludes("GET=");
                properties.setShortcutRequestMappings(mappings);
                executeConfigure();
            }
            @Test
            @DisplayName("should not set the exclusions property on the RestConfiguration")
            void shouldNotSetExclusions() {
                verify(exclusions, never()).exclude(any(), any());
            }
        }

        @Nested
        @DisplayName("given invalid excluded shortcut request mapping")
        class InvalidExcludedMappings {
            @BeforeEach
            void setUp() {
                ShortcutRequestMappings mappings = new ShortcutRequestMappings();
                mappings.setExcludes("GET=/");
                properties.setShortcutRequestMappings(mappings);
                executeConfigure();
            }
            @Test
            @DisplayName("should not set the exclusions property on the RestConfiguration")
            void shouldNotSetExclusions() {
                verify(exclusions, never()).exclude(any(), any());
            }
        }

        @Nested
        @DisplayName("given a null base uri property")
        class NullBaseUri {
            @BeforeEach
            void setUp() {
                executeConfigure();
            }
            @Test
            @DisplayName("should not set the property on the RestConfiguration")
            void shouldNotSetProperty() {
                verify(restConfig, never()).setBaseUri(any());
            }
        }

        @Nested
        @DisplayName("given a null properties")
        class NullProperties {
            @BeforeEach
            void setUp() {
                properties = null;
                executeConfigure();
            }
            @Test
            @DisplayName("should not set the property on the RestConfiguration")
            void shouldNotSetProperty() {
                verify(restConfig, never()).setBaseUri(any());
                verify(restConfig, never()).setFullyQualifiedLinks(anyBoolean());
            }
        }
    }
}
