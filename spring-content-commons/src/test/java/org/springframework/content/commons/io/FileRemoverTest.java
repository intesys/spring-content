package org.springframework.content.commons.io;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@DisplayName("FileRemover")
public class FileRemoverTest {

    private File file;
    private FileRemover observer;

    @Nested
    @DisplayName("when a file input stream observer's closed is called")
    class ClosedCalled {
        @BeforeEach
        void setUp() {
            file = mock(File.class);
            observer = new FileRemover(file);
            observer.closed();
        }
        @Test
        @DisplayName("should delete the underlying file")
        void deleteFile() {
            verify(file).delete();
        }
    }
}
