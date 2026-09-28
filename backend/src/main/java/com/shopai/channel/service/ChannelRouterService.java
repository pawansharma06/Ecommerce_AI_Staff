package com.shopai.channel.service;

import com.shopai.agent.domain.Conversation;
import com.shopai.agent.dto.AgentTurnResult;
import com.shopai.agent.dto.CreateConversationRequest;
import com.shopai.agent.dto.ConversationResponse;
import com.shopai.agent.repository.ConversationRepository;
import com.shopai.agent.service.AgentRuntimeService;
import com.shopai.channel.adapter.ChannelAdapter;
import com.shopai.channel.domain.ChannelMessage;
import com.shopai.channel.domain.ChannelType;
import com.shopai.channel.domain.MessageDirection;
import com.shopai.channel.domain.MessageStatus;
import com.shopai.channel.dto.ChannelDtos.*;
import com.shopai.channel.repository.ChannelMessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ChannelRouterService {

    private static final Logger log = LoggerFactory.getLogger(ChannelRouterService.class);

    private final Map<ChannelType, ChannelAdapter> adapterMap;
    private final ChannelConfigService channelConfigService;
    private final ChannelMessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final AgentRuntimeService agentRuntimeService;

    public ChannelRouterService(
            List<ChannelAdapter> adapters,
            ChannelConfigService channelConfigService,
            ChannelMessageRepository messageRepository,
            ConversationRepository conversationRepository,
            AgentRuntimeService agentRuntimeService
    ) {
        this.adapterMap = adapters.stream().collect(Collectors.toMap(ChannelAdapter::getChannelType, a -> a));
        this.channelConfigService = channelConfigService;
        this.messageRepository = messageRepository;
        this.conversationRepository = conversationRepository;
        this.agentRuntimeService = agentRuntimeService;
    }

    @Transactional
    public ChannelMessageResponse handleInboundMessage(InboundChannelMessageDto inbound) {
        log.info("Received inbound message on channel [{}] from [{}]", inbound.channelType(), inbound.senderId());

        // 1. Persist Inbound ChannelMessage
        ChannelMessage inMsg = new ChannelMessage(
                inbound.channelType(),
                MessageDirection.INBOUND,
                inbound.senderId(),
                inbound.recipientId(),
                inbound.content()
        );
        inMsg.setSenderName(inbound.senderName());
        inMsg.setSubject(inbound.subject());
        inMsg.setExternalMessageId(inbound.externalMessageId());
        inMsg.setRawPayload(inbound.rawPayload() != null ? inbound.rawPayload() : Map.of());
        inMsg.setStatus(MessageStatus.PROCESSING);

        // 2. Resolve or Create Conversation Session
        String customerEmail = resolveCustomerEmail(inbound);
        UUID conversationId = resolveOrCreateConversation(inbound, customerEmail);
        inMsg.setConversationId(conversationId);
        ChannelMessage savedInMsg = messageRepository.save(inMsg);

        // 3. Check auto-reply policy
        boolean autoReply = channelConfigService.isAutoReplyEnabled(inbound.channelType());
        if (!autoReply) {
            log.info("Auto-reply disabled for channel [{}]. Message queued for manual merchant review.", inbound.channelType());
            savedInMsg.setStatus(MessageStatus.PENDING_APPROVAL);
            return toResponse(messageRepository.save(savedInMsg));
        }

        // 4. Process Turn with Autonomous Agent
        try {
            AgentTurnResult turnResult = agentRuntimeService.processTurn(
                    conversationId,
                    inbound.content(),
                    customerEmail,
                    "CHANNEL_" + inbound.channelType().name()
            );

            String replyText = turnResult.assistantMessage().content();

            // 5. Persist Outbound ChannelMessage
            ChannelMessage outMsg = new ChannelMessage(
                    inbound.channelType(),
                    MessageDirection.OUTBOUND,
                    inbound.recipientId(),
                    inbound.senderId(),
                    replyText
            );
            outMsg.setConversationId(conversationId);
            outMsg.setSubject(inbound.subject() != null ? "Re: " + inbound.subject() : null);
            outMsg.setStatus(MessageStatus.SENT);
            messageRepository.save(outMsg);

            // 6. Dispatch Outbound Message via Adapter
            ChannelAdapter adapter = adapterMap.get(inbound.channelType());
            if (adapter != null) {
                OutboundChannelMessageDto outDto = new OutboundChannelMessageDto(
                        inbound.channelType(),
                        inbound.senderId(),
                        outMsg.getSubject(),
                        replyText,
                        Map.of("conversationId", conversationId.toString())
                );
                adapter.sendOutboundMessage(outDto);
            }

            savedInMsg.setStatus(MessageStatus.SENT);
            messageRepository.save(savedInMsg);

        } catch (Exception e) {
            log.error("Failed to process autonomous response for channel message: {}", e.getMessage(), e);
            savedInMsg.setStatus(MessageStatus.FAILED);
            savedInMsg.setErrorMessage(e.getMessage());
            messageRepository.save(savedInMsg);
        }

        return toResponse(savedInMsg);
    }

    @Transactional
    public ChannelSimulateResponse simulateChannelTurn(ChannelSimulateRequest request) {
        long start = System.currentTimeMillis();

        String messageBody = request.getEffectiveMessage();
        InboundChannelMessageDto inbound = new InboundChannelMessageDto(
                request.channelType(),
                "sim_" + System.currentTimeMillis(),
                request.senderId(),
                request.senderName() != null ? request.senderName() : "Simulator Customer",
                "ShopAI-Support",
                request.subject(),
                messageBody,
                Map.of("simulated", true)
        );

        ChannelMessageResponse inResponse = handleInboundMessage(inbound);
        UUID convId = inResponse.conversationId();

        // Retrieve last outbound message for this conversation
        List<ChannelMessage> msgs = messageRepository.findByConversationIdOrderByCreatedAtAsc(convId);
        String reply = msgs.stream()
                .filter(m -> m.getDirection() == MessageDirection.OUTBOUND)
                .reduce((first, second) -> second)
                .map(ChannelMessage::getContent)
                .orElse("No response generated.");

        long latency = System.currentTimeMillis() - start;

        return new ChannelSimulateResponse(
                inResponse.id(),
                convId,
                request.channelType(),
                request.message(),
                reply,
                1,
                latency
        );
    }

    private String resolveCustomerEmail(InboundChannelMessageDto inbound) {
        if (inbound.channelType() == ChannelType.EMAIL) {
            return inbound.senderId();
        }
        // If WhatsApp, senderId is phone number (e.g. 15551234567); we can also look up email from metadata
        if (inbound.rawPayload() != null && inbound.rawPayload().containsKey("email")) {
            return (String) inbound.rawPayload().get("email");
        }
        return null;
    }

    private UUID resolveOrCreateConversation(InboundChannelMessageDto inbound, String customerEmail) {
        // Look for existing recent conversation for this channel & sender
        List<ChannelMessage> prior = messageRepository.findBySenderIdOrderByCreatedAtDesc(inbound.senderId());
        if (!prior.isEmpty() && prior.get(0).getConversationId() != null) {
            UUID existingId = prior.get(0).getConversationId();
            Optional<Conversation> convOpt = conversationRepository.findById(existingId);
            if (convOpt.isPresent()) {
                return existingId;
            }
        }

        // Create new conversation session
        String title = inbound.channelType().name() + " - " + (inbound.senderName() != null ? inbound.senderName() : inbound.senderId());
        CreateConversationRequest req = new CreateConversationRequest(
                title,
                "CUSTOMER_SUPPORT",
                inbound.channelType().name(),
                customerEmail,
                null,
                Map.of("channel", inbound.channelType().name(), "senderId", inbound.senderId())
        );

        ConversationResponse convResp = agentRuntimeService.createConversation(req, "CHANNEL_" + inbound.channelType().name());
        return convResp.id();
    }

    public ChannelMessageResponse toResponse(ChannelMessage m) {
        return new ChannelMessageResponse(
                m.getId(),
                m.getChannelType(),
                m.getDirection(),
                m.getExternalMessageId(),
                m.getConversationId(),
                m.getSenderId(),
                m.getSenderName(),
                m.getRecipientId(),
                m.getSubject(),
                m.getContent(),
                m.getStatus(),
                m.getErrorMessage(),
                m.getCreatedAt()
        );
    }
}
