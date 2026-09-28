package com.shopai.channel.adapter;

import com.shopai.channel.domain.ChannelType;
import com.shopai.channel.dto.ChannelDtos.InboundChannelMessageDto;
import com.shopai.channel.dto.ChannelDtos.OutboundChannelMessageDto;

import java.util.Map;

/**
 * Common abstraction for all external communication channels.
 * Enforces Rule 26: "Every channel must be behind an interface."
 */
public interface ChannelAdapter {

    ChannelType getChannelType();

    InboundChannelMessageDto parseInboundPayload(String rawPayload, Map<String, String> headers);

    boolean sendOutboundMessage(OutboundChannelMessageDto outboundMessage);

    default String verifyWebhook(Map<String, String> queryParams) {
        return null;
    }
}
