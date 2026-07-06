package org.springframework.content.renditions.renderers;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.InputStream;
import java.util.Arrays;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.content.commons.io.FileRemover;
import org.springframework.content.commons.io.ObservableInputStream;
import org.springframework.content.commons.renditions.RenditionProvider;

@DisplayName("JpegToPngRenditionProvider")
public class JpegToPngRenditionProviderTest {

	private RenditionProvider service;

	@BeforeEach
	public void setUp() {
		service = new JpegToPngRenditionProvider();
	}

	@Test
	@DisplayName("should consume image/jpeg and produce image/png")
	public void testCanConvert() {
		assertThat(service.consumes(), is("image/jpeg"));
		assertThat(Arrays.asList(service.produces()), hasItems("image/png"));
	}

	@Test
	@DisplayName("should convert jpeg to png")
	public void testConvert() throws Exception {
		InputStream converted = service.convert(this.getClass().getResourceAsStream("/sample.jpeg"), "image/png");

		assertThat(converted.available(), is(greaterThan(0)));
		assertThat(((ObservableInputStream)converted).getObservers(), hasItem(is(instanceOf(FileRemover.class))));

		BufferedImage expectedImage = ImageIO.read(this.getClass().getResourceAsStream("/sample.png"));
		byte[] expectedRastaData = ((DataBufferByte) expectedImage.getData().getDataBuffer()).getData();

		BufferedImage actualImage = ImageIO.read(converted);
		byte[] actualRastaData = ((DataBufferByte) actualImage.getData().getDataBuffer()).getData();

        assertThat(expectedRastaData, is(actualRastaData));
	}
}
