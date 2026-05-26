package org.springframework.content.renditions.renderers;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.content.commons.renditions.RenditionProvider;
import org.springframework.content.renditions.RenditionException;
import org.springframework.renditions.poi.PDFService;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItemInArray;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PdfToJpegRendererTest {

	@Nested
	@DisplayName("WordToJpegRenderer")
	class WordToJpegRendererTests {

		private PDFService pdf;
		private RenditionProvider renderer;

		@BeforeEach
		void init() {
			pdf = mock(PDFService.class);
			renderer = new PdfToJpegRenderer(pdf);
		}

		@Nested
		@DisplayName("#consumes")
		class Consumes {

			@Test
			@DisplayName("should return word ml mimetype")
			void shouldReturnWordMlMimetype() {
				assertThat(renderer.consumes(), is("application/pdf"));
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

			private PDDocument doc;
			private PDFRenderer pdfRenderer;
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
					doc = mock(PDDocument.class);
					when(pdf.load(anyObject())).thenReturn(doc);
					pdfRenderer = mock(PDFRenderer.class);
					when(pdf.renderer(doc)).thenReturn(pdfRenderer);

					input = new ByteArrayInputStream("".getBytes());
				}

				@Nested
				@DisplayName("when the pdf has more than one page")
				class WhenPdfHasMoreThanOnePage {

					@BeforeEach
					void init() {
						when(doc.getNumberOfPages()).thenReturn(1);
					}

					@Test
					@DisplayName("should get the embedded thumbnail from the XWPFDocument's properties")
					void shouldGetEmbeddedThumbnail() throws Exception {
						whenConverting();
						verify(pdfRenderer).renderImageWithDPI(0, 300, ImageType.RGB);
					}

					@Test
					@DisplayName("should output the rendered image")
					void shouldOutputRenderedImage() throws Exception {
						whenConverting();
						verify(pdf).writeImage(anyObject(), eq("jpeg"), isA(OutputStream.class));
					}

					@Nested
					@DisplayName("when the pdf document fails to return a thumbnail")
					class WhenPdfFailsToReturnThumbnail {

						@BeforeEach
						void init() throws Exception {
							doThrow(IOException.class).when(pdfRenderer).renderImageWithDPI(0, 300, ImageType.RGB);
						}

						@Test
						@DisplayName("should throw a RenditionException")
						void shouldThrowRenditionException() throws Exception {
							whenConverting();
							assertThat(e, is(not(nullValue())));
							assertThat(e, is(instanceOf(RenditionException.class)));
						}

						@Test
						@DisplayName("should close the document")
						void shouldCloseDocument() throws Exception {
							whenConverting();
							verify(doc).close();
						}
					}
				}

				@Nested
				@DisplayName("when the input stream is not a valid pdf file")
				class WhenNotValidPdfFile {

					@BeforeEach
					void init() throws Exception {
						doThrow(IOException.class).when(pdf).load(anyObject());
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
