package internal.org.springframework.content.s3.io;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class PartialContentInputStreamTest {

    private static final byte[] FULL_DATA = "This is a test string".getBytes(StandardCharsets.UTF_8);
    private static final byte NUL = 0;

    private InputStream inputStream;

    @Nested
    @DisplayName("PartialContentInputStream")
    class Partialcontentinputstream {

        @Nested
        @DisplayName("with a range from the start")
        class WithARangeFromTheStart {

            @BeforeEach
            void init() throws Exception {
                inputStream = PartialContentInputStream.fromContentRange(
                        new ByteArrayInputStream(FULL_DATA, 0, 4),
                        "bytes 0-3/"+FULL_DATA.length
                );
            }

            @AfterEach
            void cleanup() throws Exception {
                inputStream.close();
            }

            @Test
            @DisplayName("reads fully from start to finish")
            void readsFullyFromStartToFinish() throws Exception {
                var readData = new byte[FULL_DATA.length];
                Arrays.fill(readData, (byte)0xba);
                IOUtils.readFully(inputStream, readData);

                assertThat(readData[0], is(equalTo(FULL_DATA[0])));
                assertThat(readData[1], is(equalTo(FULL_DATA[1])));
                assertThat(readData[2], is(equalTo(FULL_DATA[2])));
                assertThat(readData[3], is(equalTo(FULL_DATA[3])));
                for(int i = 4; i < FULL_DATA.length; i++) {
                    assertThat(readData[i], is(equalTo(NUL)));
                }

                assertThat(inputStream.read(), is(equalTo(-1)));
            }

            @Test
            @DisplayName("skips bytes into the range")
            void skipsBytesIntoTheRange() throws Exception {
                inputStream.skipNBytes(2);

                var readData = new byte[6];
                Arrays.fill(readData, (byte)0xba);
                IOUtils.readFully(inputStream, readData);
                assertThat(readData[0], is(equalTo(FULL_DATA[2])));
                assertThat(readData[1], is(equalTo(FULL_DATA[3])));
                assertThat(readData[2], is(equalTo(NUL)));
            }

            @Test
            @DisplayName("skips bytes inside the range")
            void skipsBytesInsideTheRange() throws Exception {
                assertThat(inputStream.read(), is(equalTo(FULL_DATA[0] & 0xff)));
                inputStream.skipNBytes(2);
                assertThat(inputStream.read(), is(equalTo(FULL_DATA[3] & 0xff)));
                assertThat(inputStream.read(), is(equalTo(NUL & 0xff)));
            }

            @Test
            @DisplayName("skips bytes out of the range")
            void skipsBytesOutOfTheRange() throws Exception {
                assertThat(inputStream.read(), is(equalTo(FULL_DATA[0] & 0xff)));
                inputStream.skipNBytes(FULL_DATA.length - 1);

                assertThat(inputStream.read(), is(equalTo(-1)));
            }

            @Test
            @DisplayName("skips bytes after the end of the range")
            void skipsBytesAfterTheEndOfTheRange() throws Exception {
                inputStream.skipNBytes(4);

                assertThat(inputStream.skip(Long.MAX_VALUE), is(equalTo((long)FULL_DATA.length - 4)));
            }
        }

        @Nested
        @DisplayName("with a range to the end")
        class WithARangeToTheEnd {

            @BeforeEach
            void init() throws Exception {
                inputStream = PartialContentInputStream.fromContentRange(
                        new ByteArrayInputStream(FULL_DATA, 10, FULL_DATA.length-10),
                        "bytes 10-"+FULL_DATA.length+"/*"
                );
            }

            @AfterEach
            void cleanup() throws Exception {
                inputStream.close();
            }

            @Test
            @DisplayName("reads fully from start to finish")
            void readsFullyFromStartToFinish() throws Exception {
                var readData = new byte[FULL_DATA.length];
                Arrays.fill(readData, (byte)0xba);
                IOUtils.readFully(inputStream, readData);

                for(int i = 0; i < 10; i++) {
                    assertThat(readData[i], is(equalTo(NUL)));
                }
                for(int i = 10; i < FULL_DATA.length; i++) {
                    assertThat(readData[i], is(equalTo(FULL_DATA[i])));
                }
                assertThat(inputStream.read(), is(equalTo(-1)));
            }

            @Test
            @DisplayName("skips bytes before start of the range")
            void skipsBytesBeforeStartOfTheRange() throws Exception {
                assertThat(inputStream.skip(5), is(equalTo(5L)));

                var readData = new byte[FULL_DATA.length-5];
                Arrays.fill(readData, (byte)0xba);
                IOUtils.readFully(inputStream, readData);
                assertThat(readData[0], is(equalTo(NUL)));
                assertThat(readData[4], is(equalTo(NUL)));
                for(int i = 5; i < readData.length; i++) {
                    assertThat(readData[i], is(equalTo(FULL_DATA[5+i])));
                }

                assertThat(inputStream.read(), is(equalTo(-1)));
            }

            @Test
            @DisplayName("skips bytes into the range")
            void skipsBytesIntoTheRange() throws Exception {
                inputStream.skipNBytes(15);

                var readData = new byte[6];
                IOUtils.readFully(inputStream, readData);
                for(int i = 0; i < 6; i++) {
                    assertThat(readData[i], is(equalTo(FULL_DATA[15+i])));
                }
            }

            @Test
            @DisplayName("skips bytes inside the range")
            void skipsBytesInsideTheRange() throws Exception {
                inputStream.skipNBytes(10);

                assertThat(inputStream.read(), is(equalTo(FULL_DATA[10] & 0xff)));
                inputStream.skipNBytes(2);
                assertThat(inputStream.read(), is(equalTo(FULL_DATA[13] & 0xff)));
            }

            @Test
            @DisplayName("skips bytes out of the range")
            void skipsBytesOutOfTheRange() throws Exception {
                inputStream.skipNBytes(10);
                assertThat(inputStream.read(), is(equalTo(FULL_DATA[10] & 0xff)));
                inputStream.skipNBytes(FULL_DATA.length - 11);

                assertThat(inputStream.read(), is(equalTo(-1)));
            }
        }

        @Nested
        @DisplayName("with a range in the middle")
        class WithARangeInTheMiddle {

            @BeforeEach
            void init() throws Exception {
                inputStream = PartialContentInputStream.fromContentRange(
                        new ByteArrayInputStream(FULL_DATA, 3, 4),
                        "bytes 3-6/"+FULL_DATA.length
                );
            }

            @AfterEach
            void cleanup() throws Exception {
                inputStream.close();
            }

            @Test
            @DisplayName("reads fully from start to finish")
            void readsFullyFromStartToFinish() throws Exception {
                var readData = new byte[FULL_DATA.length];
                Arrays.fill(readData, (byte)0xba);
                IOUtils.readFully(inputStream, readData);

                for(int i = 0; i < 3; i++) {
                    assertThat(readData[i], is(equalTo(NUL)));
                }
                assertThat(readData[3], is(equalTo(FULL_DATA[3])));
                assertThat(readData[4], is(equalTo(FULL_DATA[4])));
                assertThat(readData[5], is(equalTo(FULL_DATA[5])));
                assertThat(readData[6], is(equalTo(FULL_DATA[6])));
                for(int i = 7; i < FULL_DATA.length; i++) {
                    assertThat(readData[i], is(equalTo(NUL)));
                }

                assertThat(inputStream.read(), is(equalTo(-1)));
            }

            @Test
            @DisplayName("skips bytes before start of the range")
            void skipsBytesBeforeStartOfTheRange() throws Exception {
                assertThat(inputStream.skip(2), is(equalTo(2L)));

                var readData = new byte[6];
                Arrays.fill(readData, (byte)0xba);
                IOUtils.readFully(inputStream, readData);
                assertThat(readData[0], is(equalTo(NUL)));
                assertThat(readData[1], is(equalTo(FULL_DATA[3])));
                assertThat(readData[2], is(equalTo(FULL_DATA[4])));
                assertThat(readData[3], is(equalTo(FULL_DATA[5])));
                assertThat(readData[4], is(equalTo(FULL_DATA[6])));
                assertThat(readData[5], is(equalTo(NUL)));
            }

            @Test
            @DisplayName("skips bytes into the range")
            void skipsBytesIntoTheRange() throws Exception {
                inputStream.skipNBytes(4);

                var readData = new byte[6];
                Arrays.fill(readData, (byte)0xba);
                IOUtils.readFully(inputStream, readData);
                assertThat(readData[0], is(equalTo(FULL_DATA[4])));
                assertThat(readData[1], is(equalTo(FULL_DATA[5])));
                assertThat(readData[2], is(equalTo(FULL_DATA[6])));
                assertThat(readData[3], is(equalTo(NUL)));
            }

            @Test
            @DisplayName("skips bytes inside the range")
            void skipsBytesInsideTheRange() throws Exception {
                inputStream.skipNBytes(3);

                assertThat(inputStream.read(), is(equalTo(FULL_DATA[3] & 0xff)));
                inputStream.skipNBytes(2);
                assertThat(inputStream.read(), is(equalTo(FULL_DATA[6] & 0xff)));
                assertThat(inputStream.read(), is(equalTo(NUL & 0xff)));
            }

            @Test
            @DisplayName("skips bytes out of the range")
            void skipsBytesOutOfTheRange() throws Exception {
                inputStream.skipNBytes(3);
                assertThat(inputStream.read(), is(equalTo(FULL_DATA[3] & 0xff)));
                inputStream.skipNBytes(FULL_DATA.length - 4);

                assertThat(inputStream.read(), is(equalTo(-1)));
            }

            @Test
            @DisplayName("skips bytes after the end of the range")
            void skipsBytesAfterTheEndOfTheRange() throws Exception {
                inputStream.skipNBytes(7);

                assertThat(inputStream.skip(Long.MAX_VALUE), is(equalTo((long)FULL_DATA.length - 7)));
            }
        }
    }

}
