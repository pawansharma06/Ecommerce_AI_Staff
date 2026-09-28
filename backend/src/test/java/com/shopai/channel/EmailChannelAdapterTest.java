package com.shopai.channel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopai.channel.adapter.EmailChannelAdapter;
import com.shopai.channel.domain.ChannelType;
import com.shopai.channel.dto.ChannelDtos.InboundChannelMessageDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EmailChannelAdapterTest {

    private EmailChannelAdapter adapter;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        objectMapper = new ObjectMapper();
        adapter = new EmailChannelAdapter(objectMapper);
    }

    @Test
    @DisplayName("parseInboundPayload parses email message and extracts sender details")
    void testParseEmailPayload() {
        String payload = """
                {
                  "from": "Jane Doe <jane.doe@example.com>",
                  "to": "support@shopai.dev",
                  "subject": "Question regarding return policy",
                  "text": "What is the return window for clothing?",
                  "messageId": "<msg-101@mail.example.com>"
                }
                """;

        InboundChannelMessageDto dto = adapter.parseInboundPayload(payload, Map.of());

        assertNotNull(dto);
        assertEquals(ChannelType.EMAIL, dto.channelType());
        assertEquals("jane.doe@example.com", dto.senderId());
        assertEquals("Jane Doe", dto.senderName());
        assertEquals("Question regarding return policy", dto.subject());
        assertEquals("What is the return window for clothing?", dto.content());
        assertEquals("<msg-101@mail.example.com>", dto.externalMessageId());
    }

    @Test
    @DisplayName("extractOrderReference extracts order number from subject or body")
    void testExtractOrderReference() {
        String orderNum1 = adapter.extractOrderReference("Re: Order #1001 status inquiry", "Hello");
        assertEquals("1001", orderNum1);

        String orderNum2 = adapter.extractOrderReference("Return request", "I would like to return items from order 8002.");
        assertEquals("8002", orderNum2);
    }
}
