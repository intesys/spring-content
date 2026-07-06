package org.springframework.content.commons.utils;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.util.ReflectionUtils;

public class ReflectionServiceTest {

	private ReflectionService reflectionService;

	// mocks
	private HelloWorldService service;

	@Nested
	@DisplayName("ReflectionService")
	class ReflectionServiceSection {

		@Nested
		@DisplayName("invokeMethod")
		class InvokeMethod {

			@BeforeEach
			void setUp() {
				service = mock(HelloWorldService.class);
				reflectionService = new ReflectionServiceImpl();
				reflectionService.invokeMethod(ReflectionUtils
						.findMethod(HelloWorldService.class, "helloWorld"), service,
						new Object[] {});
			}

			@Test
			@DisplayName("should invoke the method")
			void shouldInvokeMethod() {
				verify(service).helloWorld();
			}
		}
	}

	public interface HelloWorldService {
		void helloWorld();
	}
}
