package it.internal.org.springframework.content.rest.controllers;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import internal.org.springframework.content.rest.support.ContentEntity;
import internal.org.springframework.content.rest.support.TestEntity2;
import internal.org.springframework.content.rest.support.TestEntity4;
import lombok.Setter;
import org.apache.commons.io.IOUtils;

import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.repository.Store;
import org.springframework.data.repository.CrudRepository;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Setter
public class Version {

    private MockMvc mvc;
    private String url;
    private String collectionUrl;
    private String contentLinkRel;
    private CrudRepository repo;
    private Store store;
    private String etag;
    private ContentEntity entity;

    public static Version tests() {
        return new Version();
    }

    public void runIssue1975() throws Exception {
        String entityUrl = mvc.perform(post(collectionUrl).content("{}"))
                .andExpect(status().is2xxSuccessful()).andReturn().getResponse().getHeader("Location");
        assertThat(entityUrl, is(not(nullValue())));

        String body = mvc.perform(get(entityUrl).accept("application/json"))
                .andExpect(status().is2xxSuccessful()).andReturn().getResponse().getContentAsString();

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode responseJson = objectMapper.readTree(body);
        JsonNode linksNode = responseJson.get("_links");
        JsonNode contentNode = linksNode.get(contentLinkRel);
        String contentHref = contentNode.get("href").asText();

        mvc.perform(put(contentHref).header("If-Match", "\"0\"").contentType("text/plain").content("Hello world!"))
                .andExpect(status().is2xxSuccessful());  // Content created
        mvc.perform(delete(contentHref).header("If-Match", "\"1\""))
                .andExpect(status().is2xxSuccessful());  // User A
        mvc.perform(put(contentHref).header("If-Match", "\"1\"").contentType("text/plain").content("foo bar"))
                .andExpect(status().isPreconditionFailed());  // User B
    }

    public void runGetRequestReturnsEtag() throws Exception {
        MockHttpServletResponse response = mvc
                .perform(get(url)
                        .accept("text/plain"))
                .andExpect(status().isOk())
                .andExpect(header().string("etag", is(etag)))
                .andReturn().getResponse();

        assertThat(response, is(not(nullValue())));
        assertThat(response.getContentAsString(), is("Hello Spring Content World!"));
    }

    public void runGetRequestWithMatchingIfNoneMatch() throws Exception {
        mvc.perform(get(url)
                .accept("text/plain")
                .header("if-none-match", etag))
                .andExpect(status().isNotModified());
    }

    public void runGetRequestWithNonMatchingIfNoneMatch() throws Exception {
        MockHttpServletResponse response = mvc
                .perform(get(url)
                        .accept("text/plain")
                        .header("if-none-match", "\"999\""))
                .andExpect(status().isOk())
                .andExpect(header().string("etag", is(etag)))
                .andReturn().getResponse();

        assertThat(response, is(not(nullValue())));
        assertThat(response.getContentAsString(), is("Hello Spring Content World!"));
    }

    public void runPutWithMatchingIfMatchUpdatesContent() throws Exception {
        mvc.perform(put(url)
                .content("Hello Modified Spring Content World!")
                .contentType("text/other")
                .header("if-match", etag))
                .andExpect(status().isOk());
    }

    public void runPutWithMatchingIfMatchUpdatesAttributes() throws Exception {
        mvc.perform(multipart(url)
                .file(new MockMultipartFile("file",
                        "test-file-modified.txt",
                        "text/other", "Hello Modified Spring Content World!".getBytes()))
                .header("if-match", etag))
                .andExpect(status().isOk());

        if (entity != null) {
            Optional<ContentEntity> fetched = repo.findById(entity.getId());
            assertThat(fetched.isPresent(), is(true));
            assertThat(fetched.get().getLen(), is(36L));
            assertThat(fetched.get().getMimeType(), is("text/other"));
            assertThat(fetched.get().getOriginalFileName(), is("test-file-modified.txt"));
        }
    }

    public void runDeleteWithMatchingIfMatch() throws Exception {
        mvc.perform(delete(url)
                .contentType("text/plain"))
                .andExpect(status().isNoContent());

        if (entity != null) {
            Optional<ContentEntity> fetched = repo.findById(entity.getId());
            assertThat(fetched.isPresent(), is(true));
            assertThat(fetched.get().getContentId(), is(nullValue()));
            assertThat(fetched.get().getLen(), is(nullValue()));
            assertThat(fetched.get().getMimeType(), is(nullValue()));
            assertThat(((ContentStore)store).getContent(fetched.get()), is(nullValue()));
        }
    }

    public void runPutWithNonMatchingIfMatch() throws Exception {
        mvc.perform(put(url)
                .content("Hello Modified Spring Content World!")
                .contentType("text/plain")
                .header("if-match", "\"999\""))
                .andExpect(status().isPreconditionFailed());
    }

    public void runPutWithMatchingIfNoneMatch() throws Exception {
        mvc.perform(put(url)
                .content("Hello Modified Spring Content World!")
                .contentType("text/plain")
                .header("if-none-match", etag))
                .andExpect(status().isPreconditionFailed());
    }

    public void runPutWithNonMatchingIfNoneMatch() throws Exception {
        mvc.perform(put(url)
                .content("Hello Modified Spring Content World!")
                .contentType("text/plain")
                .header("if-none-match", "\"999\""))
                .andExpect(status().isOk());
    }

    public void runPutWithMatchingIfMatchAndMatchingIfNoneMatch() throws Exception {
        mvc.perform(put(url)
                .content("Hello Modified Spring Content World!")
                .contentType("text/plain")
                .header("if-match", etag)
                .header("if-none-match", etag))
                .andExpect(status().isPreconditionFailed());
    }

    public void runPostWithNonMatchingIfMatch() throws Exception {
        mvc.perform(multipart(url)
                .file(new MockMultipartFile("file",
                        "tests-file-modified.txt",
                        "text/plain", "Hello Spring Content World!".getBytes()))
                .header("if-match", "\"999\""))
                .andExpect(status().isPreconditionFailed());
    }

    public void runDeleteWithNonMatchingIfMatch() throws Exception {
        mvc.perform(delete(url)
                .header("if-match", "\"999\""))
                .andExpect(status().isPreconditionFailed());
    }
}
