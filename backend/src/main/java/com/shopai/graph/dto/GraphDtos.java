package com.shopai.graph.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class GraphDtos {

    public record GraphNodeDto(
            String id,
            String label,
            String type,
            Map<String, Object> metadata
    ) {}

    public record GraphEdgeDto(
            String id,
            String source,
            String sourceType,
            String target,
            String targetType,
            String relationshipType,
            BigDecimal weight,
            BigDecimal confidence,
            Map<String, Object> metadata
    ) {}

    public record GraphSubgraphDto(
            List<GraphNodeDto> nodes,
            List<GraphEdgeDto> edges
    ) {}

    public record GraphStatsDto(
            long totalEdges,
            int totalNodes,
            Map<String, Long> relationshipCounts
    ) {}

    public record RebuildGraphResponse(
            long edgesCreated,
            long durationMs,
            Map<String, Long> countsByType,
            String message
    ) {}
}
