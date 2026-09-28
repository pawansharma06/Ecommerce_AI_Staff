package com.shopai.channel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopai.channel.adapter.WhatsAppChannelAdapter;
import com.shopai.channel.domain.ChannelType;
import com.shopai.channel.dto.ChannelDtos.InboundChannelMessageDto;
import com.shopai.channel.dto.ChannelDtos.OutboundChannelMessageDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WhatsAppChannelAdapterTest {

    private WhatsAppChannelAdapter adapter;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        objectMapper = new ObjectMapper();
        adapter = new WhatsAppChannelAdapter(objectMapper);
    }

    @Test
    @DisplayName("verifyWebhook handles hub challenge handshake")
    void testVerifyWebhook() {
        Map<String, String> params = Map.of(
                "hub.mode", "subscribe",
                "hub.challenge", "challenge_code_12345",
                "hub.verify_token", "my_secret_token"
        );
        String challenge = adapter.verifyWebhook(params);
        assertEquals("challenge_code_12345", challenge);
    }

    @Test
    @DisplayName("parseInboundPayload correctly parses WhatsApp Cloud API JSON structure")
    void testParseWhatsAppPayload() {
        String payload = """
                {
                  "object": "whatsapp_business_account",
                  "entry": [{
                    "id": "10001",
                    "changes": [{
                      "value": {
                        "messaging_product": "whatsapp",
                        "metadata": {
                          "display_phone_number": "+15550001",
                          "phone_number_id": "10002"
                        },
                        "contacts": [{
                          "profile": { "name": "Jane Doe" },
                          "wa_id": "15551234567"
                        }],
                        "messages": [{
                          "from": "15551234567",
                          "id": "wamid.HBgL12345",
                          "timestamp": "1727500000",
                          "text": { "body": "Where is my order #1001?" },
                          "type": "text"
                        }]
                      },
                      "field": "messages"
                    }]
                  }]
                }
                """;

        InboundChannelMessageDto dto = adapter.parseInboundPayload(payload, Map.of());

        assertNotNull(dto);
        assertEquals(ChannelType.WHATSAPP, dto.channelType());
        assertEquals("15551234567", dto.senderId());
        assertEquals("Jane Doe", dto.senderName());
        assertEquals("+15550001", dto.recipientId());
        assertEquals("Where is my order #1001?", dto.content());
        assertEquals("wamid.HBgL12345", dto.externalMessageId());
    }

    @Test
    @DisplayName("sendOutboundMessage simulates dispatch successfully")
    void testSendOutboundMessage() {
        OutboundChannelMessageDto out = new OutboundChannelMessageDto(
                ChannelType.WHATSAPP,
                "15551234567",
                null,
                "Your order #1001 has been fulfilled.",
                Map.of()
        );
        boolean sent = adapter.sendOutboundMessage(out);
        assertTrue(sent);
    }
}
