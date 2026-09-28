package com.shopai.shopify.webhook;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class ShopifyWebhookVerifierTest {

    private ShopifyWebhookVerifier verifier;
    private final String secretKey = "shpss_test_webhook_secret_key";

    @BeforeEach
    void setUp() {
        verifier = new ShopifyWebhookVerifier();
    }

    @Test
    void validatesValidHmacSignature() throws Exception {
        String payload = "{\"id\": 12345, \"email\": \"customer@example.com\"}";
        byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String expectedHmac = Base64.getEncoder().encodeToString(mac.doFinal(payloadBytes));

        boolean isValid = verifier.verifyHmac(payloadBytes, expectedHmac, secretKey);
        assertThat(isValid).isTrue();
    }

    @Test
    void rejectsInvalidOrForgedSignature() {
        String payload = "{\"id\": 12345}";
        byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);

        boolean isValid = verifier.verifyHmac(payloadBytes, "fake_invalid_hmac_signature", secretKey);
        assertThat(isValid).isFalse();
    }

    @Test
    void rejectsNullOrEmptyInputs() {
        assertThat(verifier.verifyHmac(null, "some_hmac", secretKey)).isFalse();
        assertThat(verifier.verifyHmac(new byte[0], null, secretKey)).isFalse();
        assertThat(verifier.verifyHmac(new byte[0], "some_hmac", null)).isFalse();
    }
}
