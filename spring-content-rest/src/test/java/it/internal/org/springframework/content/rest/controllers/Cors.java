package it.internal.org.springframework.content.rest.controllers;

import lombok.Getter;
import lombok.Setter;

import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebAppConfiguration
@Getter
@Setter
public class Cors {

	private MockMvc mvc;
	private String url;

	public static Cors tests(){
		return new Cors();
	}

	public void optionsRequestFromKnownHostShouldReturnCorsHeaders() throws Exception {
		mvc.perform(options(url)
				.header("Access-Control-Request-Method", "DELETE")
				.header("Origin", "http://www.someurl.com"))
				.andExpect(status().isOk())
				.andExpect(header().string("Access-Control-Allow-Origin","http://www.someurl.com"));
	}

	public void optionsRequestFromUnknownHostShouldBeForbidden() throws Exception {
		mvc.perform(options(url)
				.header("Access-Control-Request-Method", "DELETE")
				.header("Origin", "http://www.someotherurl.com"))
				.andExpect(status().isForbidden());
	}
}
