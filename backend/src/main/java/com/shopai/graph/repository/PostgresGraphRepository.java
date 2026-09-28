package com.shopai.graph.repository;

import com.shopai.graph.domain.CommerceRelationship;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Repository
public class PostgresGraphRepository implements GraphRepository {

    private final CommerceRelationshipJpaRepository jpaRepository;

    public PostgresGraphRepository(CommerceRelationshipJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional
    public CommerceRelationship saveEdge(CommerceRelationship edge) {
        Optional<CommerceRelationship> existing = jpaRepository
                .findBySourceTypeAndSourceIdAndRelationshipTypeAndTargetTypeAndTargetId(
                        edge.getSourceType(),
                        edge.getSourceId(),
                        edge.getRelationshipType(),
                        edge.getTargetType(),
                        edge.getTargetId()
                );
        if (existing.isPresent()) {
            CommerceRelationship current = existing.get();
            current.setWeight(edge.getWeight());
            current.setConfidence(edge.getConfidence());
            if (edge.getMetadata() != null && !edge.getMetadata().isEmpty()) {
                current.getMetadata().putAll(edge.getMetadata());
            }
            current.setUpdatedAt(java.time.Instant.now());
            return jpaRepository.save(current);
        }
        return jpaRepository.save(edge);
    }

    @Override
    @Transactional
    public List<CommerceRelationship> saveAllEdges(List<CommerceRelationship> edges) {
        return edges.stream().map(this::saveEdge).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommerceRelationship> findOutboundEdges(String sourceType, String sourceId) {
        return jpaRepository.findBySourceTypeAndSourceId(sourceType, sourceId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommerceRelationship> findInboundEdges(String targetType, String targetId) {
        return jpaRepository.findByTargetTypeAndTargetId(targetType, targetId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommerceRelationship> findEdgesByType(String relationshipType) {
        return jpaRepository.findByRelationshipType(relationshipType);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommerceRelationship> findOutboundEdgesByType(String sourceType, String sourceId, String relationshipType) {
        return jpaRepository.findBySourceTypeAndSourceIdAndRelationshipType(sourceType, sourceId, relationshipType);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommerceRelationship> findAdjacentEdges(String entityType, String entityId) {
        return jpaRepository.findAllAdjacent(entityType, entityId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CommerceRelationship> findEdge(
            String sourceType, String sourceId, String relationshipType, String targetType, String targetId
    ) {
        return jpaRepository.findBySourceTypeAndSourceIdAndRelationshipTypeAndTargetTypeAndTargetId(
                sourceType, sourceId, relationshipType, targetType, targetId
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> getRelationshipTypeCounts() {
        List<Object[]> rows = jpaRepository.countByRelationshipType();
        Map<String, Long> result = new LinkedHashMap<>();
        for (Object[] row : rows) {
            String relType = (String) row[0];
            Long count = ((Number) row[1]).longValue();
            result.put(relType, count);
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public long countTotalEdges() {
        return jpaRepository.count();
    }

    @Override
    @Transactional
    public void deleteEdgesByType(String relationshipType) {
        jpaRepository.deleteByRelationshipType(relationshipType);
    }

    @Override
    @Transactional
    public void deleteAllEdges() {
        jpaRepository.deleteAll();
    }
}
