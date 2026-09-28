package com.shopai.common.health;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record HealthResponse(
        String status,
        String application,
        String version,
        String database,
        String redis,
        String pgvector,
        String activeLlmProvider,
        Integer registeredToolsCount,
        Integer channelsReady
) {
    public HealthResponse(String status, String application, String version, String database, String redis) {
        this(status, application, version, database, redis, null, null, null, null);
    }
}
