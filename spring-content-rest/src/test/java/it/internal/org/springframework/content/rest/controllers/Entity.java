package it.internal.org.springframework.content.rest.controllers;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringRegularExpression.matchesRegex;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.StringReader;
import java.util.Optional;

import org.springframework.data.repository.CrudRepository;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.theoryinpractise.halbuilder.api.ReadableRepresentation;
import com.theoryinpractise.halbuilder.api.RepresentationFactory;
import com.theoryinpractise.halbuilder.standard.StandardRepresentationFactory;

import internal.org.springframework.content.rest.support.ContentEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Entity {

	private MockMvc mvc;
	private String url;
	private String linkRel;
	private ContentEntity entity;
	private CrudRepository repository;

	public static Entity tests() {
		return new Entity();
	}

	public void getRequestAcceptingHalJsonShouldReturnEntity() throws Exception {
		MockHttpServletResponse response = mvc
				.perform(get(url)
						.accept("application/hal+json"))
				.andExpect(status().isOk())
				.andReturn().getResponse();

		RepresentationFactory representationFactory = new StandardRepresentationFactory();
		ReadableRepresentation halResponse = representationFactory
				.readRepresentation("application/hal+json",
						new StringReader(response.getContentAsString()));
		assertThat(halResponse.getLinksByRel(linkRel), is(not(nullValue())));
		assertThat(halResponse.getLinksByRel(linkRel).size(), is(1));
		assertThat(halResponse.getLinksByRel(linkRel).get(0).getHref(), matchesRegex("http://localhost" + url));
	}

	public void putRequestWithJsonBodyShouldSetDataAndReturn200() throws Exception {
		entity.setTitle("Spring Content");
		mvc.perform(put(url)
				.content(new ObjectMapper().writeValueAsString(entity))
				.contentType("application/hal+json"))
				.andExpect(status().is2xxSuccessful());

		Optional<ContentEntity> fetched = repository.findById(entity.getId());
		assertThat(fetched.isPresent(), is(true));
		assertThat(fetched.get().getTitle(), is("Spring Content"));
		assertThat(fetched.get().getContentId(), is(nullValue()));
		assertThat(fetched.get().getLen(), is(nullValue()));
	}

	public void patchRequestWithJsonBodyShouldPatchDataAndReturn200() throws Exception {
		mvc.perform(patch(url)
				.content("{\"title\":\"Spring Content Modified\"}")
				.contentType("application/hal+json"))
				.andExpect(status().is2xxSuccessful());

		Optional<ContentEntity> fetched = repository.findById(entity.getId());
		assertThat(fetched.isPresent(), is(true));
		assertThat(fetched.get().getTitle(), is("Spring Content Modified"));
		assertThat(fetched.get().getContentId(), is(nullValue()));
		assertThat(fetched.get().getLen(), is(nullValue()));
		assertThat(fetched.get().getMimeType(), is(nullValue()));
	}

	public void headRequestWithJsonBodyShouldReturn200() throws Exception {
		mvc.perform(head(url))
				.andExpect(status().is2xxSuccessful());
	}
}
