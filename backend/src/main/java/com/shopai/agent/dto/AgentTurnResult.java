package com.shopai.agent.dto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record AgentTurnResult(
        UUID conversationId,
        MessageResponse userMessage,
        MessageResponse assistantMessage,
        List<Map<String, Object>> toolsExecuted,
        List<Map<String, Object>> pendingApprovals,
        int totalTokensUsed,
        long latencyMs
) {}
