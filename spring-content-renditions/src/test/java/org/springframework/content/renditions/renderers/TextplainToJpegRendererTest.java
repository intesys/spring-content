package org.springframework.content.renditions.renderers;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.content.commons.renditions.RenditionProvider;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class TextplainToJpegRendererTest {

	@Nested
	@DisplayName("TextplainToJpegRenderer")
	class TextplainToJpegRendererTests {

		private boolean wrapText = false;
		private RenditionProvider renderer;

		@BeforeEach
		void init() {
			renderer = new TextplainToJpegRenderer(wrapText);
		}

		@Nested
		@DisplayName("#consumes")
		class Consumes {

			@Test
			@DisplayName("should return text/plain")
			void shouldReturnTextPlain() {
				assertThat(renderer.consumes(), is("text/plain"));
			}
		}

		@Nested
		@DisplayName("#produces")
		class Produces {

			@Test
			@DisplayName("should return jpeg mimetype")
			void shouldReturnJpegMimetype() {
				assertThat(renderer.produces(), hasItemInArray("image/jpg"));
				assertThat(renderer.produces(), hasItemInArray("image/jpeg"));
			}
		}

		@Nested
		@DisplayName("#convert")
		class Convert {

			private InputStream input, result;
			private String mimeType;
			private Exception e;

			private void whenConverting() {
				try {
					result = renderer.convert(input, mimeType);
				} catch (Exception ex) {
					this.e = ex;
				}
			}

			@Nested
			@DisplayName("given a plain/text input")
			class GivenPlainTextInput {

				@Nested
				@DisplayName("given a single-line input")
				class GivenSingleLineInput {

					@BeforeEach
					void init() {
						input = new ByteArrayInputStream("Hello Spring Content World!".getBytes());
					}

					@Test
					@DisplayName("should produce the correct image")
					void shouldProduceCorrectImage() {
						whenConverting();
						InputStream expected = TextplainToJpegRendererTest.this.getClass().getResourceAsStream("/textplaintorenderer/single-line.jpeg");
						assertThat(expected, is(not(nullValue())));
						assertThat(result, is(not(nullValue())));
					}
				}

				@Nested
				@DisplayName("given a multi-line input")
				class GivenMultiLineInput {

					@BeforeEach
					void init() {
						input = new ByteArrayInputStream("Hello\nSpring\n\nContent\n\n\nWorld!".getBytes());
					}

					@Test
					@DisplayName("should produce the correct image")
					void shouldProduceCorrectImage() {
						whenConverting();
						assertThat(result, is(not(nullValue())));
					}
				}

				@Nested
				@DisplayName("given a long line and wrapping")
				class GivenLongLineAndWrapping {

					@BeforeEach
					void init() {
						wrapText = true;
						input = new ByteArrayInputStream("Hello Spring Content World!  This is a really long line that we expect to wrap".getBytes());
					}

					@Test
					@DisplayName("should produce the correct image")
					void shouldProduceCorrectImage() {
						whenConverting();
						assertThat(result, is(not(nullValue())));
					}
				}

				@Nested
				@DisplayName("given a long line and no wrapping")
				class GivenLongLineAndNoWrapping {

					@BeforeEach
					void init() {
						input = new ByteArrayInputStream("Hello Spring Content World!  This is a really long line that we expect to wrap".getBytes());
					}

					@Test
					@DisplayName("should produce the correct image")
					void shouldProduceCorrectImage() {
						whenConverting();
						assertThat(result, is(not(nullValue())));
					}
				}

				@Nested
				@DisplayName("given a line file will overflow the image size")
				class GivenLineFileWillOverflowImageSize {

					@BeforeEach
					void init() {
						input = new ByteArrayInputStream("Hello\n\nSpring\n\nContent\n\nWorld!\n\n\nThis\n\nis\n\na\n\nreally\n\nreally\n\nreally\n\nreally\n\nreally\n\nlong\n\nfile\n\nthat\n\nwill\n\noverflow\n\nthe\n\nimage".getBytes());
					}

					@Test
					@DisplayName("should produce the correct image")
					void shouldProduceCorrectImage() {
						whenConverting();
						assertThat(result, is(not(nullValue())));
					}
				}
			}

			@Nested
			@DisplayName("when the input stream is not a valid word file")
			class WhenNotValidWordFile {

				@BeforeEach
				void init() {
					input = TextplainToJpegRendererTest.this.getClass().getResourceAsStream("/sample-docx.docx");
				}

				@Test
				@DisplayName("should not error")
				void shouldNotError() {
					whenConverting();
					assertThat(e, is(nullValue()));
				}
			}

			@Nested
			@DisplayName("given a null input stream")
			class GivenNullInputStream {

				@Test
				@DisplayName("should return an error")
				void shouldReturnError() {
					whenConverting();
					assertThat(e, is(not(nullValue())));
				}
			}
		}
	}
}
