package internal.org.springframework.renditions.poi;

import org.apache.poi.openxml4j.exceptions.NotOfficeXmlFileException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.content.commons.renditions.RenditionProvider;
import org.springframework.content.renditions.renderers.WordToJpegRenderer;
import org.springframework.renditions.poi.POIService;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItemInArray;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class POIServiceTest {

	@Nested
	@DisplayName("POIService")
	class POIServiceTests {

		private POIService poi;

		@BeforeEach
		void init() {
			poi = new POIServiceImpl();
		}

		@Nested
		@DisplayName("#xwpfDocument")
		class XwpfDocument {

			@Nested
			@DisplayName("given an input stream")
			class GivenInputStream {

				private InputStream stream;

				@BeforeEach
				void init() {
					stream = POIServiceTest.this.getClass().getResourceAsStream("/sample-docx.docx");
				}

				@Test
				@DisplayName("should return an instance of an XPWFDocument")
				void shouldReturnXWPFDocument() throws Exception {
					assertThat(poi.xwpfDocument(stream), is(not(nullValue())));
				}
			}

			@Nested
			@DisplayName("given a null inputstream")
			class GivenNullInputStream {

				private InputStream stream;

				@Test
				@DisplayName("should throw an exception")
				void shouldThrowException() {
					assertThrows(IllegalArgumentException.class, () -> {
						try {
							poi.xwpfDocument(stream);
						} catch (IOException e) {
							throw new RuntimeException(e);
						}
					});
				}
			}

			@Nested
			@DisplayName("given an invalid inputstream")
			class GivenInvalidInputStream {

				private InputStream stream;

				@BeforeEach
				void init() {
					stream = new ByteArrayInputStream("asdhg".getBytes());
				}

				@Test
				@DisplayName("should throw an exception")
				void shouldThrowException() {
					assertThrows(NotOfficeXmlFileException.class, () -> {
						try {
							poi.xwpfDocument(stream);
						} catch (IOException e) {
							throw new RuntimeException(e);
						}
					});
				}
			}
		}
	}
}
