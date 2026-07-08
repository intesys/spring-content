package it.internal.org.springframework.content.rest.controllers;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.repository.Store;
import org.springframework.data.repository.CrudRepository;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import internal.org.springframework.content.rest.support.ContentEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Content {

    private MockMvc mvc;
    private String url;
    private String contextPath = "";
    private ContentEntity entity;
    private CrudRepository repository;
    private Store store;

    public static Content tests() {
        return new Content();
    }

    public void getStarAcceptShouldReturn404() throws Exception {
        mvc.perform(get(url)
                .accept("*/*"))
        .andExpect(status().isNotFound());
    }

    public void getContentMimeTypeAcceptShouldReturn404() throws Exception {
        mvc.perform(get(url)
                .accept("text/plain"))
        .andExpect(status().isNotFound());
    }

    public void putWithContentBodyShouldSetContentAndReturn201() throws Exception {
        String content = "Hello New Spring Content World!";
        mvc.perform(
                put(url)
                .contextPath(contextPath)
                .content(content)
                .contentType("text/plain"))
        .andExpect(status().isCreated());

        Optional<ContentEntity> fetched = repository.findById(entity.getId());
        assertThat(fetched.isPresent(), is(true));
        assertThat(fetched.get().getContentId(), is(not(nullValue())));
        assertThat(fetched.get().getLen(), is(31L));
        assertThat(fetched.get().getMimeType(), is("text/plain"));
        assertThat(IOUtils.toString(((ContentStore)store).getContent(fetched.get()), Charset.defaultCharset()), is(content));
    }

    public void deleteWithMimeTypeShouldReturn404() throws Exception {
        mvc.perform(delete(url)
                .contextPath(contextPath)
                .accept("text/plain")).andExpect(status().isNotFound());
    }

    public void postMultiPartFormDataShouldSetContentAndReturn200() throws Exception {
        String content = "This is Spring Content!";

        mvc.perform(multipart(url)
                .file(new MockMultipartFile("file", "\u8868\u5355ID\u53CA\u5B57\u6BB5.txt", "text/plain", content.getBytes()))
                .contextPath(contextPath)
                )
        .andExpect(status().isCreated());

        Optional<ContentEntity> fetched = repository.findById(entity.getId());
        assertThat(fetched.isPresent(), is(true));
        assertThat(fetched.get().getContentId(), is(not(nullValue())));
        assertThat(fetched.get().getOriginalFileName(), is("\u8868\u5355ID\u53CA\u5B57\u6BB5.txt"));
        assertThat(fetched.get().getMimeType(), is("text/plain"));
        assertThat(fetched.get().getLen(), is(Long.valueOf(content.length())));
    }

    // Methods requiring the Entity to have text/plain content
    public void getContentShouldReturnOriginalContentFilenameAnd200() throws Exception {
        assertThat(Charset.defaultCharset(), is(Charset.forName("UTF-8")));

        MockHttpServletResponse response = mvc
                .perform(get(url)
                        .contextPath(contextPath)
                        .accept("text/plain"))
                .andExpect(status().isOk()).andReturn().getResponse();

        assertThat(response, is(not(nullValue())));
        assertThat(response.getHeader("Content-Disposition"), containsString("filename*=UTF-8''" + URLEncoder.encode("\u8868\u5355ID\u53CA\u5B57\u6BB5.txt")));
        assertThat(response.getContentAsString(), is("Hello Spring Content World!"));
    }

    public void getWithNoAcceptHeaderShouldReturnOriginalContent() throws Exception {
        MockHttpServletResponse response = mvc.perform(
                get(url)
                .contextPath(contextPath)
                )
                .andExpect(status().isOk())
                .andReturn().getResponse();

        assertThat(response, is(not(nullValue())));
        assertThat(response.getContentAsString(),is("Hello Spring Content World!"));
    }

    public void getWithRendererMimeTypeShouldReturnRenditionAnd200() throws Exception {
        MockHttpServletResponse response = mvc
                .perform(get(url)
                        .contextPath(contextPath)
                        .accept("text/html"))
                .andExpect(status().isOk()).andReturn()
                .getResponse();

        assertThat(response, is(not(nullValue())));
        assertThat(response.getContentAsString(), is("<html><body>Hello Spring Content World!</body></html>"));
        assertThat(response.getContentType(), is("text/html"));
    }

    public void getWithRendererMimeTypeAndOriginalContentTypeShouldReturnRenditionAnd200() throws Exception {
        MockHttpServletResponse response = mvc
                .perform(get(url)
                        .contextPath(contextPath)
                        .accept("text/html"))
                .andExpect(status().isOk()).andReturn()
                .getResponse();

        assertThat(response, is(not(nullValue())));
        assertThat(response.getContentAsString(), is("<html><body>original content</body></html>"));
        assertThat(response.getContentType(), is("text/html"));
    }

    public void getWithMultipleMimeTypesLastMatchingContentShouldReturnOriginalAnd200() throws Exception {
        MockHttpServletResponse response = mvc.perform(get(url)
                .contextPath(contextPath)
                .accept(new String[] { "text/xml", "text/plain" }))
                .andExpect(status().isOk()).andReturn()
                .getResponse();

        assertThat(response, is(not(nullValue())));
        assertThat(response.getContentAsString(), is("Hello Spring Content World!"));
        assertThat(response.getContentType(), is("text/plain"));
    }

    public void getWithMultipleMimeTypesMiddleMatchingContentShouldReturnRenditionAnd200() throws Exception {
        MockHttpServletResponse response = mvc.perform(get(url)
                .contextPath(contextPath)
                .accept(new String[] { "text/xml", "text/html", "*/*" }))
                .andExpect(status().isOk()).andReturn()
                .getResponse();

        assertThat(response, is(not(nullValue())));
        assertThat(response.getContentAsString(), is("<html><body>Hello Spring Content World!</body></html>"));
        assertThat(response.getContentType(), is("text/html"));
    }

    public void getWithAcceptAllMimeTypeShouldReturnOriginalAnd200() throws Exception {
        MockHttpServletResponse response = mvc.perform(get(url)
                .contextPath(contextPath)
                .accept(new String[] { "*/*" }))
                .andExpect(status().isOk()).andReturn()
                .getResponse();

        assertThat(response, is(not(nullValue())));
        assertThat(response.getContentAsString(), is("Hello Spring Content World!"));
        assertThat(response.getContentType(), is("text/plain"));
    }

    public void getWithMimeTypeSpecifyingCharsetShouldReturnOriginalAnd200() throws Exception {
        MockHttpServletResponse response = mvc.perform(get(url)
                .contextPath(contextPath)
                .accept(new String[] { "text/html;charset=ISO-8859-1" }))
                .andExpect(status().isOk()).andReturn()
                .getResponse();

        assertThat(response, is(not(nullValue())));
        assertThat(response.getContentAsString(), is("<html><body>Hello Spring Content World!</body></html>"));
        assertThat(response.getContentType(), is("text/html;charset=ISO-8859-1"));
    }

    public void getWhenOriginalMimeTypeHasCharsetShouldReturnOriginalAnd200() throws Exception {
        MockHttpServletResponse response = mvc.perform(get(url)
                .contextPath(contextPath)
                .accept(new String[] { "text/plain" }))
                .andExpect(status().isOk()).andReturn()
                .getResponse();

        assertThat(response, is(not(nullValue())));
        assertThat(response.getContentAsString(), is("Hello Spring Content World!"));
        assertThat(response.getContentType(), is("text/plain;charset=ISO-8859-1"));
    }

    public void getWithMimeTypeNotMatchingRendererOrContentShouldReturnNotFound() throws Exception {
        mvc.perform(get(url)
            .contextPath(contextPath)
            .accept("text/css"))
        .andExpect(status().isNotFound());
    }

    public void getWithRangeHeaderShouldReturnContentRangeAnd206() throws Exception {
        MockHttpServletResponse response = mvc
                .perform(get(url)
                        .contextPath(contextPath)
                        .accept("text/plain")
                        .header("range", "bytes=6-19"))
                .andExpect(status().isPartialContent()).andReturn()
                .getResponse();

        assertThat(response, is(not(nullValue())));
        assertThat(response.getContentAsString(), is("Spring Content"));
    }

    public void putShouldOverwriteContentAndReturn200() throws Exception {
        mvc.perform(put(url)
                .contextPath(contextPath)
                .content("Hello Modified Spring Content World!")
                .contentType("text/plain"))
        .andExpect(status().isOk());

        assertThat(IOUtils.toString(((ContentStore)store).getContent(entity), Charset.defaultCharset()), is("Hello Modified Spring Content World!"));
    }

    public void postMultiPartShouldOverwriteContentAndReturn200() throws Exception {
        String content = "This is Modified Spring Content!";

        mvc.perform(multipart(url)
                .file(new MockMultipartFile("file",
                        "tests-file-modified.txt",
                        "text/plain", content.getBytes()))
                .contextPath(contextPath)
                )
        .andExpect(status().isOk());

        Optional<ContentEntity> fetched = repository.findById(entity.getId());
        assertThat(fetched.isPresent(), is(true));
        assertThat(fetched.get().getContentId(), is(not(nullValue())));
        assertThat(fetched.get().getOriginalFileName(), is("tests-file-modified.txt"));
        assertThat(fetched.get().getMimeType(), is("text/plain"));
        assertThat(fetched.get().getLen(), is(Long.valueOf(content.length())));
    }

    public void postWithMissingContentTypeShouldReturn400() throws Exception {
        mvc.perform(post(url)
                        .content("some content")
                        .contextPath(contextPath)
                )
                .andExpect(status().isBadRequest());
    }

    public void putWithMultiPartRequestShouldOverwriteContentAndReturn200() throws Exception {
        String content = "This is Modified Spring Content!";

        mvc.perform(multipart(HttpMethod.PUT, url)
                        .file(new MockMultipartFile("file",
                                "tests-file-modified.txt",
                                "text/plain", content.getBytes()))
                        .contextPath(contextPath)
                )
                .andExpect(status().isOk());

        Optional<ContentEntity> fetched = repository.findById(entity.getId());
        assertThat(fetched.isPresent(), is(true));
        assertThat(fetched.get().getContentId(), is(not(nullValue())));
        assertThat(fetched.get().getOriginalFileName(), is("tests-file-modified.txt"));
        assertThat(fetched.get().getMimeType(), is("text/plain"));
        assertThat(fetched.get().getLen(), is(Long.valueOf(content.length())));
    }

    public void deleteWithMimeTypeShouldDeleteContentAttributesAndReturn200() throws Exception {
        mvc.perform(delete(url)
                .contentType("text/plain")
                .contextPath(contextPath)
                )
        .andExpect(status().isNoContent());

        Optional<ContentEntity> fetched = repository.findById(entity.getId());
        assertThat(fetched.isPresent(), is(true));
        assertThat(fetched.get().getContentId(), is(nullValue()));
        assertThat(fetched.get().getLen(), is(nullValue()));
        assertThat(fetched.get().getMimeType(), is(nullValue()));
        assertThat(((ContentStore)store).getContent(entity), is(nullValue()));
    }
}
