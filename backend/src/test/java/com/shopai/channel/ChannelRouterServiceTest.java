package com.shopai.channel;

import com.shopai.agent.domain.Conversation;
import com.shopai.agent.domain.ConversationMessage;
import com.shopai.agent.dto.AgentTurnResult;
import com.shopai.agent.dto.ConversationResponse;
import com.shopai.agent.repository.ConversationRepository;
import com.shopai.agent.service.AgentRuntimeService;
import com.shopai.channel.adapter.ChannelAdapter;
import com.shopai.channel.adapter.WhatsAppChannelAdapter;
import com.shopai.channel.domain.ChannelMessage;
import com.shopai.channel.domain.ChannelType;
import com.shopai.channel.domain.MessageDirection;
import com.shopai.channel.domain.MessageStatus;
import com.shopai.channel.dto.ChannelDtos.ChannelMessageResponse;
import com.shopai.channel.dto.ChannelDtos.InboundChannelMessageDto;
import com.shopai.channel.repository.ChannelMessageRepository;
import com.shopai.channel.service.ChannelConfigService;
import com.shopai.channel.service.ChannelRouterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ChannelRouterServiceTest {

    private WhatsAppChannelAdapter whatsAppAdapter;
    private ChannelConfigService channelConfigService;
    private ChannelMessageRepository messageRepository;
    private ConversationRepository conversationRepository;
    private AgentRuntimeService agentRuntimeService;
    private ChannelRouterService routerService;

    @BeforeEach
    void setup() {
        whatsAppAdapter = Mockito.mock(WhatsAppChannelAdapter.class);
        when(whatsAppAdapter.getChannelType()).thenReturn(ChannelType.WHATSAPP);

        channelConfigService = Mockito.mock(ChannelConfigService.class);
        messageRepository = Mockito.mock(ChannelMessageRepository.class);
        conversationRepository = Mockito.mock(ConversationRepository.class);
        agentRuntimeService = Mockito.mock(AgentRuntimeService.class);

        routerService = new ChannelRouterService(
                List.of(whatsAppAdapter),
                channelConfigService,
                messageRepository,
                conversationRepository,
                agentRuntimeService
        );
    }

    @Test
    @DisplayName("handleInboundMessage processes message through agent and dispatches outbound reply")
    void testHandleInboundMessage() {
        UUID convId = UUID.randomUUID();
        when(channelConfigService.isAutoReplyEnabled(ChannelType.WHATSAPP)).thenReturn(true);
        when(messageRepository.findBySenderIdOrderByCreatedAtDesc("15551234567")).thenReturn(List.of());

        when(agentRuntimeService.createConversation(any(), any())).thenReturn(
                new ConversationResponse(convId, "WHATSAPP - Jane", "CUSTOMER_SUPPORT", "WHATSAPP", null, null, null, null, java.time.Instant.now(), java.time.Instant.now())
        );

        when(messageRepository.save(any(ChannelMessage.class))).thenAnswer(i -> {
            ChannelMessage m = i.getArgument(0);
            if (m.getId() == null) m.setId(UUID.randomUUID());
            return m;
        });

        Conversation conv = new Conversation();
        conv.setId(convId);
        ConversationMessage userMsg = new ConversationMessage(conv, "USER", "Where is my order?");
        ConversationMessage assistantMsg = new ConversationMessage(conv, "ASSISTANT", "Your order #1001 is on its way!");
        AgentTurnResult turnResult = new AgentTurnResult(
                convId,
                com.shopai.agent.dto.MessageResponse.from(userMsg),
                com.shopai.agent.dto.MessageResponse.from(assistantMsg),
                List.of(),
                List.of(),
                120,
                250L
        );

        when(agentRuntimeService.processTurn(eq(convId), eq("Where is my order?"), any(), any()))
                .thenReturn(turnResult);

        InboundChannelMessageDto inbound = new InboundChannelMessageDto(
                ChannelType.WHATSAPP,
                "msg_123",
                "15551234567",
                "Jane Doe",
                "+15550001",
                null,
                "Where is my order?",
                Map.of()
        );

        ChannelMessageResponse response = routerService.handleInboundMessage(inbound);

        assertNotNull(response);
        assertEquals(ChannelType.WHATSAPP, response.channelType());
        assertEquals("Where is my order?", response.content());
        verify(agentRuntimeService, times(1)).processTurn(eq(convId), eq("Where is my order?"), any(), any());
        verify(whatsAppAdapter, times(1)).sendOutboundMessage(any());
    }
}
