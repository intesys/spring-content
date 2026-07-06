package org.springframework.content.commons.io;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.FileInputStream;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@DisplayName("ObservableInputStream")
public class ObservableInputStreamTest {

    private ObservableInputStream ois;

    private FileInputStream fis;
    private InputStreamObserver observer;

    @Nested
    @DisplayName("when an input stream is observed")
    class Observed {
        @BeforeEach
        void setUp() {
            fis = mock(FileInputStream.class);
            observer = mock(InputStreamObserver.class);
            ois = new ObservableInputStream(fis, observer);
        }

        @Test
        @DisplayName("when the input stream has listeners should return them")
        void returnListeners() {
            assertThat(ois.getObservers(), hasItem(observer));
        }

        @Test
        @DisplayName("when the input stream is read should delegate to the underlying input stream")
        void delegateRead() throws Exception {
            ois.read();
            verify(fis).read();
        }

        @Test
        @DisplayName("when the input stream is closed should call listeners on closed event handler")
        void callListenersOnClose() throws Exception {
            ois.close();
            verify(observer).closed();
        }
    }
}
