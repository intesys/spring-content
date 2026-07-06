package it.internal.org.springframework.content.rest.controllers;

import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class LastModifiedDate {

    private MockMvc mvc;
    private String url;
    private Date lastModifiedDate;
    private String etag;
    private String content;

    public static LastModifiedDate tests() {
        return new LastModifiedDate();
    }

    public void setMvc(MockMvc mvc) {
        this.mvc = mvc;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setLastModifiedDate(Date lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    public void setEtag(String etag) {
        this.etag = format("\"%s\"", etag);
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void runGetRequestNoHeaders() throws Exception {
        MockHttpServletResponse response = mvc
                .perform(get(url)
                        .accept("text/plain"))
                .andExpect(status().isOk())
                .andReturn().getResponse();

        assertThat(response, is(not(nullValue())));
        assertThat(response.getContentAsString(), is(content));
        assertThat(response.getHeader("last-modified"), isWithinASecond(lastModifiedDate));
    }

    public void runGetRequestIfModifiedSinceBefore() throws Exception {
        SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        Calendar cal = Calendar.getInstance();
        cal.setTime(lastModifiedDate);
        cal.add(Calendar.DATE, -1);
        String ifModifiedSince = format.format(cal.getTime());

        MockHttpServletResponse response = mvc
                .perform(get(url)
                        .accept("text/plain")
                        .header("if-modified-since", ifModifiedSince))
                .andExpect(status().isOk())
                .andReturn().getResponse();

        assertThat(response, is(not(nullValue())));
        assertThat(response.getContentAsString(), is(content));
        assertThat(response.getHeader("last-modified"), isWithinASecond(lastModifiedDate));
    }

    public void runGetRequestIfModifiedSinceSame() throws Exception {
        mvc.perform(get(url)
                        .accept("text/plain")
                        .header("if-modified-since", toHeaderDateFormat(lastModifiedDate)))
                        .andExpect(status().isNotModified())
                        .andExpect(content().string(is("")));
    }

    public void runGetRequestIfUnmodifiedSinceBefore() throws Exception {
        mvc.perform(get(url)
                        .accept("text/plain")
                        .header("if-unmodified-since", toHeaderDateFormat(addDays(lastModifiedDate, -1))))
                        .andExpect(status().isPreconditionFailed())
                        .andReturn();
    }

    public void runGetRequestIfUnmodifiedSinceSame() throws Exception {
        mvc.perform(get(url)
                        .accept("text/plain")
                        .header("if-unmodified-since", isWithinASecond(lastModifiedDate)))
                        .andExpect(status().isOk())
                        .andExpect(content().string(is(content)));
    }

    public void runPutIfUnmodifiedSinceBefore() throws Exception {
        mvc.perform(put(url)
                        .content("Hello Modified Spring Content World!")
                        .contentType("text/plain")
                        .header("if-unmodified-since", toHeaderDateFormat(addDays(lastModifiedDate, -1))))
                        .andExpect(status().isPreconditionFailed());
    }

    public void runPutIfUnmodifiedSinceSame() throws Exception {
        mvc.perform(put(url)
                        .content("Hello Modified Spring Content World!")
                        .contentType("text/plain")
                        .header("if-unmodified-since", toHeaderDateFormat(lastModifiedDate)))
                        .andExpect(status().isOk());
    }

    public void runPutWithMatchingIfUnmodifiedSinceAndMatchingIfNoneMatch() throws Exception {
        if (etag != null) {
            mvc.perform(put(url)
                    .content("Hello Modified Spring Content World!")
                    .contentType("text/plain")
                    .header("if-unmodified-since", toHeaderDateFormat(lastModifiedDate))
                    .header("if-none-match", etag))
                    .andExpect(status().isPreconditionFailed());
        }
    }

    public void runDeleteIfUnmodifiedSinceBefore() throws Exception {
        mvc.perform(delete(url)
                        .header("if-unmodified-since", toHeaderDateFormat(addDays(lastModifiedDate, -1))))
                        .andExpect(status().isPreconditionFailed());
    }

    public void runDeleteIfUnmodifiedSinceSame() throws Exception {
        mvc.perform(delete(url)
                        .header("if-unmodified-since", toHeaderDateFormat(lastModifiedDate)))
                        .andExpect(status().isNoContent());
    }

    public static Matcher<String> isWithinASecond(final Date expectedDate) {
        return new TypeSafeMatcher<String>() {

            @Override
            protected void describeMismatchSafely(String foo, Description description) {
                description.appendText("was ").appendValue(foo);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("Date ").appendValue(expectedDate);
            }

            @Override
            protected boolean matchesSafely(String actualDate) {
                Instant instant = Instant.ofEpochMilli(expectedDate.getTime());
                LocalDateTime expectedDateTime = LocalDateTime.ofInstant(instant, ZoneId.of("GMT"));

                LocalDateTime actualDateTime = LocalDateTime.parse(actualDate, DateTimeFormatter.ofPattern("EEE, d MMM yyyy HH:mm:ss z", Locale.ENGLISH));

                long diff = ChronoUnit.SECONDS.between(expectedDateTime, actualDateTime);
                return diff <= 1;
            }
        };
    }

    private static String toHeaderDateFormat(Date dt) {
        SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        return format.format(dt);
    }

    private static Date addDays(Date dt, int n) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(dt);
        cal.add(Calendar.DATE, n);
        return cal.getTime();
    }
}
