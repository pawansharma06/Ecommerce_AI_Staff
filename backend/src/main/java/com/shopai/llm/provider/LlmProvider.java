package com.shopai.llm.provider;

import com.shopai.llm.dto.LlmPrompt;
import com.shopai.llm.dto.LlmResponse;
import com.shopai.llm.dto.ToolDefinition;

import java.util.List;

public interface LlmProvider {

    String getProviderName();

    String getModelName();

    boolean isConfigured();

    LlmResponse generateChat(LlmPrompt prompt);

    LlmResponse generateWithTools(LlmPrompt prompt, List<ToolDefinition> tools);
}