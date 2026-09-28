package com.shopai.channel.controller;

import com.shopai.channel.adapter.EmailChannelAdapter;
import com.shopai.channel.adapter.WhatsAppChannelAdapter;
import com.shopai.channel.domain.ChannelType;
import com.shopai.channel.dto.ChannelDtos.*;
import com.shopai.channel.repository.ChannelMessageRepository;
import com.shopai.channel.service.ChannelConfigService;
import com.shopai.channel.service.ChannelRouterService;
import com.shopai.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/channels")
@Tag(name = "Multi-Channel Commerce Engine", description = "Endpoints for WhatsApp, Email, and external communication channel routing")
public class ChannelController {

    private final ChannelConfigService channelConfigService;
    private final ChannelRouterService channelRouterService;
    private final WhatsAppChannelAdapter whatsAppChannelAdapter;
    private final EmailChannelAdapter emailChannelAdapter;
    private final ChannelMessageRepository messageRepository;

    public ChannelController(
            ChannelConfigService channelConfigService,
            ChannelRouterService channelRouterService,
            WhatsAppChannelAdapter whatsAppChannelAdapter,
            EmailChannelAdapter emailChannelAdapter,
            ChannelMessageRepository messageRepository
    ) {
        this.channelConfigService = channelConfigService;
        this.channelRouterService = channelRouterService;
        this.whatsAppChannelAdapter = whatsAppChannelAdapter;
        this.emailChannelAdapter = emailChannelAdapter;
        this.messageRepository = messageRepository;
    }

    @GetMapping("/configs")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    @Operation(summary = "Get all configured communication channels")
    public ResponseEntity<ApiResponse<List<ChannelConfigDto>>> getAllConfigs() {
        return ResponseEntity.ok(ApiResponse.ok(channelConfigService.getAllConfigs()));
    }

    @PutMapping("/{type}/config")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update channel configuration and credentials")
    public ResponseEntity<ApiResponse<ChannelConfigDto>> updateConfig(
            @PathVariable ChannelType type,
            @RequestBody UpdateChannelConfigRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(channelConfigService.updateConfig(type, request)));
    }

    @GetMapping(value = "/whatsapp/webhook", produces = MediaType.TEXT_PLAIN_VALUE)
    @Operation(summary = "WhatsApp Cloud API Webhook handshake verification")
    public ResponseEntity<String> verifyWhatsAppWebhook(@RequestParam Map<String, String> queryParams) {
        String challenge = whatsAppChannelAdapter.verifyWebhook(queryParams);
        if (challenge != null) {
            return ResponseEntity.ok(challenge);
        }
        return ResponseEntity.status(403).body("Verification failed");
    }

    @PostMapping("/whatsapp/webhook")
    @Operation(summary = "Receive inbound WhatsApp Cloud API message events")
    public ResponseEntity<ApiResponse<ChannelMessageResponse>> handleWhatsAppWebhook(
            @RequestBody String rawPayload,
            @RequestHeader Map<String, String> headers
    ) {
        InboundChannelMessageDto inbound = whatsAppChannelAdapter.parseInboundPayload(rawPayload, headers);
        ChannelMessageResponse response = channelRouterService.handleInboundMessage(inbound);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/email/webhook")
    @Operation(summary = "Receive inbound Email support messages")
    public ResponseEntity<ApiResponse<ChannelMessageResponse>> handleEmailWebhook(
            @RequestBody String rawPayload,
            @RequestHeader Map<String, String> headers
    ) {
        InboundChannelMessageDto inbound = emailChannelAdapter.parseInboundPayload(rawPayload, headers);
        ChannelMessageResponse response = channelRouterService.handleInboundMessage(inbound);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/simulate")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    @Operation(summary = "Simulate an incoming channel message for interactive testing")
    public ResponseEntity<ApiResponse<ChannelSimulateResponse>> simulateMessage(
            @RequestBody ChannelSimulateRequest request
    ) {
        ChannelSimulateResponse response = channelRouterService.simulateChannelTurn(request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/messages")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    @Operation(summary = "Get message audit logs across all channels")
    public ResponseEntity<ApiResponse<Page<ChannelMessageResponse>>> getMessages(
            @RequestParam(required = false) ChannelType channelType,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<ChannelMessageResponse> result = messageRepository.findWithFilters(channelType, search, PageRequest.of(page, size))
                .map(channelRouterService::toResponse);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
