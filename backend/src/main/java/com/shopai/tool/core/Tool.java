package com.shopai.tool.core;

import java.util.Map;

public interface Tool {

    String getName();

    String getDescription();

    String getRequiredPermission();

    ToolRiskLevel getRiskLevel();

    default boolean requiresHumanApproval() {
        return getRiskLevel() == ToolRiskLevel.HIGH || getRiskLevel() == ToolRiskLevel.CRITICAL;
    }

    Map<String, Object> getParametersSchema();

    Object execute(Map<String, Object> params) throws Exception;
}