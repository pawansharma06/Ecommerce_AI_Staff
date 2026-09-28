package com.shopai.agent.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopai.agent.domain.Conversation;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ConversationResponse(
        UUID id,
        String title,
        String agentType,
        String channel,
        String customerEmail,
        Long customerId,
        Map<String, Object> metadata,
        List<MessageResponse> messages,
        Instant createdAt,
        Instant updatedAt
) {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static ConversationResponse from(Conversation c, List<MessageResponse> messages) {
        Map<String, Object> metaMap = null;
        if (c.getMetadata() != null && !c.getMetadata().isBlank()) {
            try {
                metaMap = mapper.readValue(c.getMetadata(), Map.class);
            } catch (Exception ignored) {}
        }

        return new ConversationResponse(
                c.getId(),
                c.getTitle(),
                c.getAgentType(),
                c.getChannel(),
                c.getCustomerEmail(),
                c.getCustomerId(),
                metaMap,
                messages,
                c.getCreatedAt(),
                c.getUpdatedAt()
        );
    }

    public static ConversationResponse from(Conversation c) {
        return from(c, null);
    }
}
