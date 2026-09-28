package com.shopai.channel.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopai.channel.domain.ChannelType;
import com.shopai.channel.dto.ChannelDtos.InboundChannelMessageDto;
import com.shopai.channel.dto.ChannelDtos.OutboundChannelMessageDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class EmailChannelAdapter implements ChannelAdapter {

    private static final Logger log = LoggerFactory.getLogger(EmailChannelAdapter.class);
    private static final Pattern ORDER_NUMBER_PATTERN = Pattern.compile("(?i)(?:order\\s*#?\\s*|#)(\\d{4,})");

    private final ObjectMapper objectMapper;

    public EmailChannelAdapter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public ChannelType getChannelType() {
        return ChannelType.EMAIL;
    }

    @Override
    public InboundChannelMessageDto parseInboundPayload(String rawPayload, Map<String, String> headers) {
        try {
            JsonNode root = objectMapper.readTree(rawPayload);

            String sender = root.path("from").asText(root.path("senderId").asText("customer@example.com"));
            String senderName = root.path("fromName").asText(root.path("senderName").asText(""));
            String recipient = root.path("to").asText(root.path("recipientId").asText("support@shopai.dev"));
            String subject = root.path("subject").asText("Customer Inquiry");
            String body = root.path("text").asText(root.path("content").asText(root.path("body").asText("")));
            String msgId = root.path("messageId").asText(root.path("id").asText("em_" + System.currentTimeMillis()));

            // Extract sender name from "Jane Doe <jane@example.com>" if present
            if (sender.contains("<") && sender.contains(">")) {
                int start = sender.indexOf("<");
                int end = sender.indexOf(">");
                senderName = sender.substring(0, start).trim().replace("\"", "");
                sender = sender.substring(start + 1, end).trim();
            }

            Map<String, Object> rawMap = objectMapper.convertValue(root, Map.class);

            return new InboundChannelMessageDto(
                    ChannelType.EMAIL,
                    msgId,
                    sender.toLowerCase().trim(),
                    senderName.isBlank() ? "Customer" : senderName,
                    recipient,
                    subject,
                    body,
                    rawMap
            );
        } catch (Exception e) {
            log.error("Failed to parse Email inbound payload: {}", e.getMessage(), e);
            throw new RuntimeException("Invalid Email payload: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean sendOutboundMessage(OutboundChannelMessageDto outboundMessage) {
        log.info("[EMAIL OUTBOUND] Dispatching email to <{}> [Subject: {}]:\n{}",
                outboundMessage.recipientId(),
                outboundMessage.subject(),
                outboundMessage.content().length() > 100 ? outboundMessage.content().substring(0, 100) + "..." : outboundMessage.content());
        return true;
    }

    public String extractOrderReference(String subject, String content) {
        if (subject != null) {
            Matcher m = ORDER_NUMBER_PATTERN.matcher(subject);
            if (m.find()) {
                return m.group(1);
            }
        }
        if (content != null) {
            Matcher m = ORDER_NUMBER_PATTERN.matcher(content);
            if (m.find()) {
                return m.group(1);
            }
        }
        return null;
    }
}
