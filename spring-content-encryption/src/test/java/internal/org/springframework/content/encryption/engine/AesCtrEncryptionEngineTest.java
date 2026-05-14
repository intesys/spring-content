package internal.org.springframework.content.encryption.engine;

import jakarta.xml.bind.DatatypeConverter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.content.encryption.engine.ContentEncryptionEngine.EncryptionParameters;
import org.springframework.content.encryption.engine.ContentEncryptionEngine.InputStreamRequestParameters;
import org.springframework.util.StreamUtils;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

public class AesCtrEncryptionEngineTest {

    private static final byte[] KEY = DatatypeConverter.parseHexBinary("2b7e151628aed2a6abf7158809cf4f3c");
    private static final byte[] IV = DatatypeConverter.parseHexBinary("f0f1f2f3f4f5f6f7f8f9fafbfcfdfeff");
    private static final EncryptionParameters PARAMS = new EncryptionParameters(
            new SecretKeySpec(KEY, "AES"),
            IV
    );
    private static final byte[] BLOCK_1_PLAIN = DatatypeConverter.parseHexBinary("6bc1bee22e409f96e93d7e117393172a");
    private static final byte[] BLOCK_1_CIPHER = DatatypeConverter.parseHexBinary("874d6191b620e3261bef6864990db6ce");
    private static final byte[] BLOCK_2_PLAIN = DatatypeConverter.parseHexBinary("ae2d8a571e03ac9c9eb76fac45af8e51");
    private static final byte[] BLOCK_2_CIPHER = DatatypeConverter.parseHexBinary("9806f66b7970fdff8617187bb9fffdff");
    private static final byte[] BLOCK_3_PLAIN = DatatypeConverter.parseHexBinary("30c81c46a35ce411e5fbc1191a0a52ef");
    private static final byte[] BLOCK_3_CIPHER = DatatypeConverter.parseHexBinary("5ae4df3edbd5d35e5b4f09020db03eab");
    private static final byte[] BLOCK_4_PLAIN = DatatypeConverter.parseHexBinary("f69f2445df4f9b17ad2b417be66c3710");
    private static final byte[] BLOCK_4_CIPHER = DatatypeConverter.parseHexBinary("1e031dda2fbe03d1792170a0f3009cee");

    private static final byte[] PLAINTEXT = concat(BLOCK_1_PLAIN, BLOCK_2_PLAIN, BLOCK_3_PLAIN, BLOCK_4_PLAIN);
    private static final byte[] CIPHERTEXT = concat(BLOCK_1_CIPHER, BLOCK_2_CIPHER, BLOCK_3_CIPHER, BLOCK_4_CIPHER);

    @Nested
    @DisplayName("AES-CTR encryption")
    class AesCtrEncryption {

        @Test
        @DisplayName("Generates appropriate encryption parameters")
        void generatesAppropriateEncryptionParameters() {
            var engine = new AesCtrEncryptionEngine(128);
            var parameters = engine.createNewParameters();

            assertThat(parameters.getSecretKey().getAlgorithm(), is(equalTo("AES")));
            assertThat(parameters.getSecretKey().getEncoded().length, is(equalTo(16)));

            assertThat(parameters.getInitializationVector().length, is(equalTo(16)));
        }

        @Test
        @DisplayName("Encrypts plaintext according to the encryption parameters")
        void encryptsPlaintextAccordingToTheEncryptionParameters()
            throws IOException {
            var engine = new AesCtrEncryptionEngine(128);

            var encrypted = engine.encrypt(new ByteArrayInputStream(PLAINTEXT), PARAMS);

            var encryptedBytes = StreamUtils.copyToByteArray(encrypted);

            assertThat(encryptedBytes, is(equalTo(CIPHERTEXT)));
        }

        @Test
        @DisplayName("Decrypts ciphertext according to the encryption parameters")
        void decryptsCiphertextAccordingToTheEncryptionParameters()
            throws IOException {
            var engine = new AesCtrEncryptionEngine(128);

            try(var decrypted = engine.decrypt(req -> new ByteArrayInputStream(CIPHERTEXT), PARAMS, InputStreamRequestParameters.full())) {
                var decryptedBytes = StreamUtils.copyToByteArray(decrypted);

                assertThat(decryptedBytes, is(equalTo(PLAINTEXT)));
            }
        }

        @Nested
        @DisplayName("decryption with 'weird' IVs")
        class DecryptionWithWeirdIVs {

            @Test
            @DisplayName("Handles an IV that starts with zeroes")
            void handlesAnIVThatStartsWithZeroes()
                throws IOException {
                var engine = new AesCtrEncryptionEngine(128);
                EncryptionParameters params = new EncryptionParameters(
                        new SecretKeySpec(KEY, "AES"),
                        DatatypeConverter.parseHexBinary("000000f3f4f5f6f7f8f9fafbfcfdfeff")
                );

                var encrypted = engine.encrypt(new ByteArrayInputStream(PLAINTEXT), params);
                var offsetStart = BLOCK_1_PLAIN.length + BLOCK_2_PLAIN.length;

                try(var decrypted = engine.decrypt(req -> onlyByteRange(encrypted, req), params, InputStreamRequestParameters.startingFrom(offsetStart))) {
                    decrypted.skipNBytes(offsetStart);

                    var decryptedBytes = StreamUtils.copyToByteArray(decrypted);

                    assertThat(decryptedBytes, is(equalTo(concat(BLOCK_3_PLAIN, BLOCK_4_PLAIN))));
                }
            }

            @Test
            @DisplayName("Handles an IV that behaves normally during calculation")
            void handlesAnIVThatBehavesNormallyDuringCalculation()
                throws IOException {
                var engine = new AesCtrEncryptionEngine(128);

                EncryptionParameters params = new EncryptionParameters(
                        new SecretKeySpec(KEY, "AES"),
                        DatatypeConverter.parseHexBinary("4ffffffffffffffffffffffffffffffe")
                );

                var encrypted = engine.encrypt(new ByteArrayInputStream(PLAINTEXT), params);
                var offsetStart = BLOCK_1_PLAIN.length + BLOCK_2_PLAIN.length;

                try(var decrypted = engine.decrypt(req -> onlyByteRange(encrypted, req), params, InputStreamRequestParameters.startingFrom(offsetStart))) {
                    decrypted.skipNBytes(offsetStart);

                    var decryptedBytes = StreamUtils.copyToByteArray(decrypted);

                    assertThat(decryptedBytes, is(equalTo(concat(BLOCK_3_PLAIN, BLOCK_4_PLAIN))));
                }
            }

            @Test
            @DisplayName("Handles an IV that wraps around during calculation")
            void handlesAnIVThatWrapsAroundDuringCalculation()
                throws IOException {
                var engine = new AesCtrEncryptionEngine(128);

                EncryptionParameters params = new EncryptionParameters(
                        new SecretKeySpec(KEY, "AES"),
                        DatatypeConverter.parseHexBinary("fffffffffffffffffffffffffffffffe")
                );

                var encrypted = engine.encrypt(new ByteArrayInputStream(PLAINTEXT), params);
                var offsetStart = BLOCK_1_PLAIN.length + BLOCK_2_PLAIN.length;

                try(var decrypted = engine.decrypt(req -> onlyByteRange(encrypted, req), params, InputStreamRequestParameters.startingFrom(offsetStart))) {
                    decrypted.skipNBytes(offsetStart);

                    var decryptedBytes = StreamUtils.copyToByteArray(decrypted);

                    assertThat(decryptedBytes, is(equalTo(concat(BLOCK_3_PLAIN, BLOCK_4_PLAIN))));
                }
            }
        }

        @Nested
        @DisplayName("Partial content decryption")
        class PartialContentDecryption {

            @Test
            @DisplayName("Decrypts starting from the third block")
            void decryptsStartingFromTheThirdBlock()
                throws IOException {
                var engine = new AesCtrEncryptionEngine(128);

                var offsetStart = BLOCK_1_PLAIN.length + BLOCK_2_PLAIN.length;
                try(var decrypted = engine.decrypt(req -> {
                            assertThat(req.getStartByteOffset(), is(greaterThan(0L)));
                            return onlyByteRange(new ByteArrayInputStream(CIPHERTEXT), req);
                        }, PARAMS, InputStreamRequestParameters.startingFrom(offsetStart)
                )) {
                    decrypted.skipNBytes(offsetStart);

                    var decryptedBytes = StreamUtils.copyToByteArray(decrypted);

                    assertThat(decryptedBytes, is(equalTo(concat(BLOCK_3_PLAIN, BLOCK_4_PLAIN))));
                }
            }

            @Test
            @DisplayName("Decrypts starting in the middle of the second block")
            void decryptsStartingInTheMiddleOfTheSecondBlock()
                throws IOException {
                var engine = new AesCtrEncryptionEngine(128);

                var offsetStart = BLOCK_1_PLAIN.length+BLOCK_2_PLAIN.length/2;

                try(var decrypted = engine.decrypt(req -> {
                    assertThat(req.getStartByteOffset(), is(greaterThan(0L)));
                    return onlyByteRange(new ByteArrayInputStream(CIPHERTEXT), req);
                }, PARAMS, InputStreamRequestParameters.startingFrom(offsetStart))) {
                    var original = new ByteArrayInputStream(PLAINTEXT);

                    decrypted.skipNBytes(offsetStart);
                    original.skipNBytes(offsetStart);

                    var originalBytes = StreamUtils.copyToByteArray(original);
                    var decryptedBytes = StreamUtils.copyToByteArray(decrypted);

                    assertThat(decryptedBytes, is(equalTo(originalBytes)));
                }
            }

            @Test
            @DisplayName("Decrypts only the first 2 blocks")
            void decryptsOnlyTheFirst2Blocks()
                throws IOException {
                var engine = new AesCtrEncryptionEngine(128);

                var offsetEnd = BLOCK_1_PLAIN.length+BLOCK_2_PLAIN.length;

                try(var decrypted = engine.decrypt(req -> {
                            assertThat(req.getStartByteOffset(), is(equalTo(0L)));
                            assertThat(req.getEndByteOffset(), is(lessThan((long)CIPHERTEXT.length)));
                            return onlyByteRange(new ByteArrayInputStream(CIPHERTEXT), req);
                        }, PARAMS, new InputStreamRequestParameters(0, (long)offsetEnd)
                )) {
                    var decryptedBytes = decrypted.readNBytes(offsetEnd);

                    assertThat(decryptedBytes, is(equalTo(concat(BLOCK_1_PLAIN, BLOCK_2_PLAIN))));
                }
            }

            @Test
            @DisplayName("Decrypts only until the middle of block 3")
            void decryptsOnlyUntilTheMiddleOfBlock3()
                throws IOException {
                var engine = new AesCtrEncryptionEngine(128);

                var offsetEnd = BLOCK_1_PLAIN.length+BLOCK_2_PLAIN.length + BLOCK_3_PLAIN.length/2;

                try(var decrypted = engine.decrypt(req -> {
                            assertThat(req.getStartByteOffset(), is(equalTo(0L)));
                            assertThat(req.getEndByteOffset(), is(lessThan((long)CIPHERTEXT.length)));
                            return onlyByteRange(new ByteArrayInputStream(CIPHERTEXT), req);
                        }, PARAMS, new InputStreamRequestParameters(0, (long)offsetEnd)
                )) {
                    var original = new ByteArrayInputStream(PLAINTEXT);

                    var decryptedBytes = decrypted.readNBytes(offsetEnd);
                    var originalBytes = original.readNBytes(offsetEnd);

                    assertThat(decryptedBytes, is(equalTo(originalBytes)));
                }
            }
        }
    }

    private static byte[] concat(byte[]... blocks) {
        var totalLength = Arrays.stream(blocks).mapToInt(b -> b.length).sum();
        var fullBuffer = ByteBuffer.allocate(totalLength);

        for (var block : blocks) {
            fullBuffer.put(block);
        }

        return fullBuffer.array();
    }

    private static InputStream onlyByteRange(InputStream inputStream, InputStreamRequestParameters params) {
        return new ZeroPrefixedInputStream(new SkippingInputStream(inputStream, params.getStartByteOffset()), params.getStartByteOffset());
    }
}
