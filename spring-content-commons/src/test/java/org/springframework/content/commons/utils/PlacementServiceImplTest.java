package org.springframework.content.commons.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;

@DisplayName("PlacementServiceImpl")
public class PlacementServiceImplTest {

	private PlacementServiceImpl placer = null;

	@BeforeEach
	void setUp() {
		placer = new PlacementServiceImpl();
	}

	@Nested
	@DisplayName("given a placement service")
	class GivenAPlacementService {
		@Test
		@DisplayName("should have removed the FallbackObjectToStringConverter")
		void shouldRemoveConverter() {
			assertThat(placer.canConvert(Object.class, String.class), is(false));
		}
	}
}
