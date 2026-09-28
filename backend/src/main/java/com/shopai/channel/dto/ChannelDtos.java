package com.shopai.channel.dto;

import com.shopai.channel.domain.ChannelType;
import com.shopai.channel.domain.MessageDirection;
import com.shopai.channel.domain.MessageStatus;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class ChannelDtos {

    public record InboundChannelMessageDto(
            ChannelType channelType,
            String externalMessageId,
            String senderId,
            String senderName,
            String recipientId,
            String subject,
            String content,
            Map<String, Object> rawPayload
    ) {}

    public record OutboundChannelMessageDto(
            ChannelType channelType,
            String recipientId,
            String subject,
            String content,
            Map<String, Object> metadata
    ) {}

    public record ChannelConfigDto(
            UUID id,
            ChannelType channelType,
            boolean enabled,
            boolean autoReplyEnabled,
            Map<String, Object> configData,
            Instant updatedAt
    ) {}

    public record UpdateChannelConfigRequest(
            boolean enabled,
            boolean autoReplyEnabled,
            Map<String, Object> configData
    ) {}

    public record ChannelMessageResponse(
            UUID id,
            ChannelType channelType,
            MessageDirection direction,
            String externalMessageId,
            UUID conversationId,
            String senderId,
            String senderName,
            String recipientId,
            String subject,
            String content,
            MessageStatus status,
            String errorMessage,
            Instant createdAt
    ) {}

    public record ChannelSimulateRequest(
            ChannelType channelType,
            String senderId,
            String senderName,
            String message,
            String content,
            String subject
    ) {
        public String getEffectiveMessage() {
            if (message != null && !message.isBlank()) return message;
            if (content != null && !content.isBlank()) return content;
            return "";
        }
    }

    public record ChannelSimulateResponse(
            UUID messageId,
            UUID conversationId,
            ChannelType channelType,
            String inboundContent,
            String outboundReply,
            int toolsExecuted,
            long latencyMs
    ) {}
}
