package com.shopai.graph.controller;

import com.shopai.common.dto.ApiResponse;
import com.shopai.graph.dto.GraphDtos.*;
import com.shopai.graph.service.CommerceGraphService;
import com.shopai.graph.service.GraphRetriever;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/graph")
@Tag(name = "Commerce Knowledge Graph", description = "Endpoints for graph exploration, entity relationships, and graph RAG")
public class GraphController {

    private final CommerceGraphService commerceGraphService;
    private final GraphRetriever graphRetriever;

    public GraphController(CommerceGraphService commerceGraphService, GraphRetriever graphRetriever) {
        this.commerceGraphService = commerceGraphService;
        this.graphRetriever = graphRetriever;
    }

    @GetMapping("/stats")
    @Operation(summary = "Get Commerce Graph metrics and relationship counts")
    public ResponseEntity<ApiResponse<GraphStatsDto>> getStats() {
        return ResponseEntity.ok(ApiResponse.ok(commerceGraphService.getGraphStats()));
    }

    @GetMapping("/explore")
    @Operation(summary = "Explore graph nodes and edges for visual canvas rendering")
    public ResponseEntity<ApiResponse<GraphSubgraphDto>> exploreGraph(
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String entityId,
            @RequestParam(defaultValue = "2") int depth,
            @RequestParam(defaultValue = "50") int limit
    ) {
        GraphSubgraphDto subgraph = graphRetriever.exploreSubgraph(entityType, entityId, depth, limit);
        return ResponseEntity.ok(ApiResponse.ok(subgraph));
    }

    @GetMapping("/frequently-bought-together")
    @Operation(summary = "Query frequently bought together products")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getFrequentlyBoughtTogether(
            @RequestParam String productId,
            @RequestParam(defaultValue = "5") int limit
    ) {
        return ResponseEntity.ok(ApiResponse.ok(graphRetriever.findFrequentlyBoughtProducts(productId, limit)));
    }

    @GetMapping("/related-products")
    @Operation(summary = "Query related/alternative products")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getRelatedProducts(
            @RequestParam String productId,
            @RequestParam(defaultValue = "5") int limit
    ) {
        return ResponseEntity.ok(ApiResponse.ok(graphRetriever.findRelatedProducts(productId, limit)));
    }

    @GetMapping("/customer-purchases")
    @Operation(summary = "Query purchase graph for a customer")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getCustomerPurchases(
            @RequestParam String customerEmail
    ) {
        return ResponseEntity.ok(ApiResponse.ok(graphRetriever.getCustomerPurchaseHistory(customerEmail)));
    }

    @PostMapping("/rebuild")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    @Operation(summary = "Mine entity relationships and rebuild the Commerce Knowledge Graph")
    public ResponseEntity<ApiResponse<RebuildGraphResponse>> rebuildGraph() {
        RebuildGraphResponse response = commerceGraphService.rebuildKnowledgeGraph();
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
