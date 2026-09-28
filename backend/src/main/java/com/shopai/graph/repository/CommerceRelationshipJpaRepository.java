package com.shopai.graph.repository;

import com.shopai.graph.domain.CommerceRelationship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CommerceRelationshipJpaRepository extends JpaRepository<CommerceRelationship, UUID> {

    List<CommerceRelationship> findBySourceTypeAndSourceId(String sourceType, String sourceId);

    List<CommerceRelationship> findByTargetTypeAndTargetId(String targetType, String targetId);

    List<CommerceRelationship> findByRelationshipType(String relationshipType);

    List<CommerceRelationship> findBySourceTypeAndSourceIdAndRelationshipType(
            String sourceType, String sourceId, String relationshipType
    );

    Optional<CommerceRelationship> findBySourceTypeAndSourceIdAndRelationshipTypeAndTargetTypeAndTargetId(
            String sourceType, String sourceId, String relationshipType, String targetType, String targetId
    );

    @Query("SELECT r FROM CommerceRelationship r WHERE " +
           "(r.sourceType = :type AND r.sourceId = :id) OR " +
           "(r.targetType = :type AND r.targetId = :id)")
    List<CommerceRelationship> findAllAdjacent(
            @Param("type") String type,
            @Param("id") String id
    );

    @Query("SELECT r.relationshipType, COUNT(r) FROM CommerceRelationship r GROUP BY r.relationshipType")
    List<Object[]> countByRelationshipType();

    @Modifying
    @Query("DELETE FROM CommerceRelationship r WHERE r.relationshipType = :relationshipType")
    void deleteByRelationshipType(@Param("relationshipType") String relationshipType);
}
