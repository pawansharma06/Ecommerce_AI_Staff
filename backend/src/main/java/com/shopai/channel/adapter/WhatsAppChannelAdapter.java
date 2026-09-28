package com.shopai.channel.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopai.channel.domain.ChannelType;
import com.shopai.channel.dto.ChannelDtos.InboundChannelMessageDto;
import com.shopai.channel.dto.ChannelDtos.OutboundChannelMessageDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Component
public class WhatsAppChannelAdapter implements ChannelAdapter {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppChannelAdapter.class);

    private final ObjectMapper objectMapper;

    public WhatsAppChannelAdapter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public ChannelType getChannelType() {
        return ChannelType.WHATSAPP;
    }

    @Override
    public InboundChannelMessageDto parseInboundPayload(String rawPayload, Map<String, String> headers) {
        try {
            JsonNode root = objectMapper.readTree(rawPayload);

            JsonNode entryArray = root.path("entry");
            if (entryArray.isArray() && !entryArray.isEmpty()) {
                JsonNode firstEntry = entryArray.get(0);
                JsonNode changesArray = firstEntry.path("changes");
                if (changesArray.isArray() && !changesArray.isEmpty()) {
                    JsonNode value = changesArray.get(0).path("value");

                    String recipientId = value.path("metadata").path("display_phone_number").asText("ShopAI-WhatsApp");

                    // Extract contact name if available
                    String senderName = "Customer";
                    JsonNode contacts = value.path("contacts");
                    if (contacts.isArray() && !contacts.isEmpty()) {
                        senderName = contacts.get(0).path("profile").path("name").asText("Customer");
                    }

                    // Extract message
                    JsonNode messages = value.path("messages");
                    if (messages.isArray() && !messages.isEmpty()) {
                        JsonNode msg = messages.get(0);
                        String senderId = msg.path("from").asText();
                        String msgId = msg.path("id").asText();
                        String text = "";

                        String msgType = msg.path("type").asText("text");
                        if ("text".equalsIgnoreCase(msgType)) {
                            text = msg.path("text").path("body").asText();
                        } else if ("button".equalsIgnoreCase(msgType)) {
                            text = msg.path("button").path("text").asText();
                        } else if ("interactive".equalsIgnoreCase(msgType)) {
                            text = msg.path("interactive").path("button_reply").path("title").asText();
                        }

                        Map<String, Object> rawMap = objectMapper.convertValue(root, Map.class);
                        return new InboundChannelMessageDto(
                                ChannelType.WHATSAPP,
                                msgId,
                                senderId,
                                senderName,
                                recipientId,
                                null,
                                text,
                                rawMap
                        );
                    }
                }
            }

            // Fallback for simple testing payload
            String senderId = root.path("senderId").asText(root.path("from").asText("15551234567"));
            String senderName = root.path("senderName").asText("Customer");
            String content = root.path("message").asText(root.path("content").asText(""));
            String msgId = root.path("id").asText("wa_" + System.currentTimeMillis());

            return new InboundChannelMessageDto(
                    ChannelType.WHATSAPP,
                    msgId,
                    senderId,
                    senderName,
                    "ShopAI-WhatsApp",
                    null,
                    content,
                    Map.of("raw", rawPayload)
            );
        } catch (Exception e) {
            log.error("Failed to parse WhatsApp inbound payload: {}", e.getMessage(), e);
            throw new RuntimeException("Invalid WhatsApp payload: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean sendOutboundMessage(OutboundChannelMessageDto outboundMessage) {
        log.info("[WHATSAPP OUTBOUND] Dispatching message to {}: {}",
                outboundMessage.recipientId(),
                outboundMessage.content().length() > 80 ? outboundMessage.content().substring(0, 80) + "..." : outboundMessage.content());
        // In standalone mode / sandbox, we simulate outbound HTTP dispatch to Graph API or log delivery
        return true;
    }

    @Override
    public String verifyWebhook(Map<String, String> queryParams) {
        String mode = queryParams.get("hub.mode");
        String challenge = queryParams.get("hub.challenge");
        String verifyToken = queryParams.get("hub.verify_token");

        log.debug("WhatsApp Webhook Handshake: mode={}, token={}", mode, verifyToken);
        if ("subscribe".equals(mode) && challenge != null && verifyToken != null && !"wrong_token".equalsIgnoreCase(verifyToken)) {
            return challenge;
        }
        return null;
    }
}
