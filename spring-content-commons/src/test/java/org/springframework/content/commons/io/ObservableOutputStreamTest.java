package org.springframework.content.commons.io;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.io.IOException;
import java.io.OutputStream;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("ObservableOutputStream")
public class ObservableOutputStreamTest {

    private ObservableOutputStream observable;

    private OutputStreamObserver observer1;
    private OutputStreamObserver observer2;

    private OutputStream os;

    private Exception exception;

    @BeforeEach
    void setUp() {
        os = mock(OutputStream.class);
        observer1 = mock(OutputStreamObserver.class);
        observer2 = mock(OutputStreamObserver.class);
        observable = new ObservableOutputStream(os);
        observable.addObservers(observer1);
        observable.addObservers(observer2);
    }

    @Test
    @DisplayName("when the output stream has listeners should return them")
    void returnListeners() {
        assertThat(observable.getObservers(), hasItem(observer1));
    }

    @Test
    @DisplayName("when the output stream is written to should delegate to the underlying input stream")
    void delegateWrite() throws Exception {
        observable.write(32);
        verify(os).write(32);
    }

    @Test
    @DisplayName("when the output stream is closed should call listeners on closed event handler (in order)")
    void callListenersOnClose() throws Exception {
        observable.close();
        InOrder inOrder = inOrder(observer1, observer2);
        inOrder.verify(observer1).closed();
        inOrder.verify(observer2).closed();
        verifyNoMoreInteractions(observer1, observer2);
    }

    @Nested
    @DisplayName("when the output stream is closed and throws an exception")
    class CloseWithException {
        @BeforeEach
        void setUp() throws Exception {
            doThrow(new IOException("badness")).when(os).close();
        }

        @Test
        @DisplayName("should call listeners on closed event handler and throw the exception")
        void callListenersAndThrow() {
            try {
                observable.close();
            } catch (Exception e) {
                exception = e;
            }
            verify(observer1).closed();
            assertThat(exception, is(not(nullValue())));
        }
    }
}
