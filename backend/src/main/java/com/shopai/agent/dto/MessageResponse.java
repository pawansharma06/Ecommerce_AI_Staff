package com.shopai.agent.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopai.agent.domain.ConversationMessage;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public record MessageResponse(
        UUID id,
        UUID conversationId,
        String role,
        String content,
        Object toolCalls,
        String toolCallId,
        String toolName,
        Integer tokenCount,
        Instant createdAt
) {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static MessageResponse from(ConversationMessage m) {
        Object parsedToolCalls = null;
        if (m.getToolCalls() != null && !m.getToolCalls().isBlank()) {
            try {
                parsedToolCalls = mapper.readValue(m.getToolCalls(), Object.class);
            } catch (Exception ignored) {
                parsedToolCalls = m.getToolCalls();
            }
        }

        return new MessageResponse(
                m.getId(),
                m.getConversation() != null ? m.getConversation().getId() : null,
                m.getRole(),
                m.getContent(),
                parsedToolCalls,
                m.getToolCallId(),
                m.getToolName(),
                m.getTokenCount(),
                m.getCreatedAt()
        );
    }
}
