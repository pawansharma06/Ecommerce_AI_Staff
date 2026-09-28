package com.shopai.tool.core;

import com.shopai.llm.dto.ToolDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ToolRegistry {

    private static final Logger log = LoggerFactory.getLogger(ToolRegistry.class);

    private final Map<String, Tool> registeredTools = new ConcurrentHashMap<>();

    public ToolRegistry() {}

    public ToolRegistry(List<Tool> tools) {
        registerTools(tools);
    }

    @Autowired(required = false)
    public void registerTools(List<Tool> tools) {
        if (tools != null) {
            for (Tool tool : tools) {
                registeredTools.put(tool.getName().toLowerCase(), tool);
                log.info("Registered Agent Tool: [{}] (Risk: {}, RequiresApproval: {})",
                        tool.getName(), tool.getRiskLevel(), tool.requiresHumanApproval());
            }
        }
    }

    public Optional<Tool> getTool(String name) {
        if (name == null) return Optional.empty();
        return Optional.ofNullable(registeredTools.get(name.trim().toLowerCase()));
    }

    public List<Tool> getAllTools() {
        return new ArrayList<>(registeredTools.values());
    }

    public List<ToolDefinition> getToolDefinitions() {
        List<ToolDefinition> defs = new ArrayList<>();
        for (Tool tool : registeredTools.values()) {
            defs.add(new ToolDefinition(
                    tool.getName(),
                    tool.getDescription(),
                    tool.getParametersSchema()
            ));
        }
        return defs;
    }

    public List<ToolDefinition> getToolDefinitionsForRisk(ToolRiskLevel maxRisk) {
        List<ToolDefinition> defs = new ArrayList<>();
        for (Tool tool : registeredTools.values()) {
            if (tool.getRiskLevel().ordinal() <= maxRisk.ordinal()) {
                defs.add(new ToolDefinition(
                        tool.getName(),
                        tool.getDescription(),
                        tool.getParametersSchema()
                ));
            }
        }
        return defs;
    }

    public int getToolCount() {
        return registeredTools.size();
    }
}