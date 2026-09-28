package com.shopai.common.crypto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EncryptionServiceTest {

    private EncryptionService encryptionService;

    @BeforeEach
    void setUp() {
        encryptionService = new EncryptionService("my-super-secret-key-32-chars-long!");
    }

    @Test
    void encryptsAndDecryptsSuccessfully() {
        String secretToken = "shpat_1234567890abcdef";
        String encrypted = encryptionService.encrypt(secretToken);

        assertThat(encrypted).isNotBlank();
        assertThat(encrypted).isNotEqualTo(secretToken);

        String decrypted = encryptionService.decrypt(encrypted);
        assertThat(decrypted).isEqualTo(secretToken);
    }

    @Test
    void handlesNullInputsGracefully() {
        assertThat(encryptionService.encrypt(null)).isNull();
        assertThat(encryptionService.decrypt(null)).isNull();
    }

    @Test
    void rejectsTamperedCiphertext() {
        String encrypted = encryptionService.encrypt("secret-data");
        String tampered = encrypted.substring(0, encrypted.length() - 4) + "AAAA";

        assertThatThrownBy(() -> encryptionService.decrypt(tampered))
                .isInstanceOf(IllegalStateException.class);
    }
}
