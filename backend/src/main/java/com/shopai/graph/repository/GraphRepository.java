package com.shopai.graph.repository;

import com.shopai.graph.domain.CommerceRelationship;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface GraphRepository {

    CommerceRelationship saveEdge(CommerceRelationship edge);

    List<CommerceRelationship> saveAllEdges(List<CommerceRelationship> edges);

    List<CommerceRelationship> findOutboundEdges(String sourceType, String sourceId);

    List<CommerceRelationship> findInboundEdges(String targetType, String targetId);

    List<CommerceRelationship> findEdgesByType(String relationshipType);

    List<CommerceRelationship> findOutboundEdgesByType(String sourceType, String sourceId, String relationshipType);

    List<CommerceRelationship> findAdjacentEdges(String entityType, String entityId);

    Optional<CommerceRelationship> findEdge(
            String sourceType, String sourceId, String relationshipType, String targetType, String targetId
    );

    Map<String, Long> getRelationshipTypeCounts();

    long countTotalEdges();

    void deleteEdgesByType(String relationshipType);

    void deleteAllEdges();
}
