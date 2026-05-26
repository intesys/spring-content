package org.springframework.content.renditions.renderers;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.poi.ooxml.POIXMLException;
import org.apache.poi.ooxml.POIXMLProperties;
import org.apache.poi.openxml4j.exceptions.NotOfficeXmlFileException;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.content.commons.renditions.RenditionProvider;
import org.springframework.content.renditions.RenditionException;
import org.springframework.renditions.poi.POIService;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItemInArray;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class WordToJpegRendererTest {

	@Nested
	@DisplayName("WordToJpegRenderer")
	class WordToJpegRendererTests {

		private POIService poi;
		private RenditionProvider renderer;

		@BeforeEach
		void init() {
			poi = mock(POIService.class);
			renderer = new WordToJpegRenderer(poi);
		}

		@Nested
		@DisplayName("#consumes")
		class Consumes {

			@Test
			@DisplayName("should return word ml mimetype")
			void shouldReturnWordMlMimetype() {
				assertThat(renderer.consumes(), is("application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
			}
		}

		@Nested
		@DisplayName("#produces")
		class Produces {

			@Test
			@DisplayName("should return jpeg mimetype")
			void shouldReturnJpegMimetype() {
				assertThat(renderer.produces(), hasItemInArray("image/jpg"));
			}
		}

		@Nested
		@DisplayName("#convert")
		class Convert {

			private XWPFDocument doc;
			POIXMLProperties props;
			private InputStream input;
			private String mimeType;
			private Exception e;

			private void whenConverting() {
				try {
					renderer.convert(input, mimeType);
				} catch (Exception ex) {
					this.e = ex;
				}
			}

			@Nested
			@DisplayName("given an input stream and a mimetype")
			class GivenInputStreamAndMimetype {

				@BeforeEach
				void init() throws Exception {
					doc = mock(XWPFDocument.class);
					when(poi.xwpfDocument(anyObject())).thenReturn(doc);
					props = mock(POIXMLProperties.class);
					when(doc.getProperties()).thenReturn(props);

					input = new ByteArrayInputStream("".getBytes());
				}

				@Test
				@DisplayName("should get the embedded thumbnail from the XWPFDocument's properties")
				void shouldGetEmbeddedThumbnail() throws Exception {
					whenConverting();
					verify(props).getThumbnailImage();
				}

				@Nested
				@DisplayName("when the input stream is not a valid word file")
				class WhenNotValidWordFile {

					@BeforeEach
					void init() throws Exception {
						doc = mock(XWPFDocument.class);
						doThrow(NotOfficeXmlFileException.class).when(poi).xwpfDocument(anyObject());
					}

					@Test
					@DisplayName("should throw a RenditionException")
					void shouldThrowRenditionException() throws Exception {
						whenConverting();
						assertThat(e, is(not(nullValue())));
						assertThat(e, is(instanceOf(RenditionException.class)));
					}
				}

				@Nested
				@DisplayName("when the word document fails to return properties")
				class WhenDocumentFailsToReturnProperties {

					@BeforeEach
					void init() throws Exception {
						doc = mock(XWPFDocument.class);
						when(poi.xwpfDocument(anyObject())).thenReturn(doc);
						props = mock(POIXMLProperties.class);
						doThrow(POIXMLException.class).when(doc).getProperties();
					}

					@Test
					@DisplayName("should throw a RenditionException")
					void shouldThrowRenditionException() throws Exception {
						whenConverting();
						assertThat(e, is(not(nullValue())));
						assertThat(e, is(instanceOf(RenditionException.class)));
					}
				}

				@Nested
				@DisplayName("when the word document fails to return a thumbnail")
				class WhenDocumentFailsToReturnThumbnail {

					@BeforeEach
					void init() throws Exception {
						doc = mock(XWPFDocument.class);
						when(poi.xwpfDocument(anyObject())).thenReturn(doc);
						props = mock(POIXMLProperties.class);
						when(doc.getProperties()).thenReturn(props);
						doThrow(IOException.class).when(props).getThumbnailImage();
					}

					@Test
					@DisplayName("should throw a RenditionException")
					void shouldThrowRenditionException() throws Exception {
						whenConverting();
						assertThat(e, is(not(nullValue())));
						assertThat(e, is(instanceOf(RenditionException.class)));
					}
				}
			}

			@Nested
			@DisplayName("given a null input stream")
			class GivenNullInputStream {

				@Test
				@DisplayName("should get the embedded thumbnail from the XWPFDocument's properties")
				void shouldGetEmbeddedThumbnail() throws Exception {
					whenConverting();
					assertThat(e, is(not(nullValue())));
				}
			}
		}
	}
}
