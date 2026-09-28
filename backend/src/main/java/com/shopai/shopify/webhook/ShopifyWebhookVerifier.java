package com.shopai.shopify.webhook;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

@Component
public class ShopifyWebhookVerifier {

    private static final Logger log = LoggerFactory.getLogger(ShopifyWebhookVerifier.class);
    private static final String HMAC_SHA256 = "HmacSHA256";

    public boolean verifyHmac(byte[] payloadBytes, String hmacHeader, String secretKey) {
        if (hmacHeader == null || secretKey == null || payloadBytes == null) {
            return false;
        }

        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(keySpec);

            byte[] calculatedHmac = mac.doFinal(payloadBytes);
            String calculatedBase64 = Base64.getEncoder().encodeToString(calculatedHmac);

            return MessageDigest.isEqual(
                    calculatedBase64.getBytes(StandardCharsets.UTF_8),
                    hmacHeader.trim().getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            log.error("Error computing Shopify webhook HMAC: {}", e.getMessage());
            return false;
        }
    }
}
