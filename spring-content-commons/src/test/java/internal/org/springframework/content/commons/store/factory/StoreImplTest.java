package internal.org.springframework.content.commons.store.factory;

import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.context.ApplicationEventPublisher;

@DisplayName("StoreImpl")
public class StoreImplTest {

    private StoreImpl stores;

    private ContentStore store;
    private ApplicationEventPublisher publisher;
    private Path contentCopyPathRoot;

    @BeforeEach
    void setUp() throws Exception {
        store = mock(ContentStore.class);
        publisher = mock(ApplicationEventPublisher.class);
        contentCopyPathRoot = Files.createTempDirectory("storeimpltest");

        File[] files = contentCopyPathRoot.toFile().listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.getName().endsWith(".tmp")) {
                    f.delete();
                }
            }
        }

        stores = new StoreImpl(ContentStore.class, store, publisher, contentCopyPathRoot);
    }

    @Nested
    @DisplayName("#setContent - inputstream")
    class SetContentInputStream {

        @BeforeEach
        void setUp() {
            when(store.setContent(any(Object.class), any(InputStream.class))).thenReturn(new Object());
        }

        @Test
        @DisplayName("should delete the content copy file")
        void shouldDeleteCopyFile() throws Exception {
            stores.setContent(new Object(), new ByteArrayInputStream("foo".getBytes()));

            File[] files = contentCopyPathRoot.toFile().listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.getName().endsWith(".tmp")) {
                        fail("Found orphaned content copy path");
                    }
                }
            }
        }
    }
}
