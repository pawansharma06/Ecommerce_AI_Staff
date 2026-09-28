package com.shopai.graph;

import com.shopai.graph.domain.CommerceRelationship;
import com.shopai.graph.repository.CommerceRelationshipJpaRepository;
import com.shopai.graph.repository.PostgresGraphRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PostgresGraphRepositoryTest {

    private CommerceRelationshipJpaRepository jpaRepository;
    private PostgresGraphRepository repository;

    @BeforeEach
    void setup() {
        jpaRepository = Mockito.mock(CommerceRelationshipJpaRepository.class);
        repository = new PostgresGraphRepository(jpaRepository);
    }

    @Test
    @DisplayName("saveEdge creates new edge if not present")
    void testSaveNewEdge() {
        when(jpaRepository.findBySourceTypeAndSourceIdAndRelationshipTypeAndTargetTypeAndTargetId(
                any(), any(), any(), any(), any()
        )).thenReturn(Optional.empty());

        when(jpaRepository.save(any(CommerceRelationship.class))).thenAnswer(i -> {
            CommerceRelationship r = i.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });

        CommerceRelationship edge = new CommerceRelationship(
                "Product", "p-1", "FREQUENTLY_BOUGHT_WITH", "Product", "p-2", BigDecimal.valueOf(3), BigDecimal.valueOf(0.9)
        );

        CommerceRelationship saved = repository.saveEdge(edge);
        assertNotNull(saved.getId());
        assertEquals("p-1", saved.getSourceId());
        assertEquals("FREQUENTLY_BOUGHT_WITH", saved.getRelationshipType());
        verify(jpaRepository, times(1)).save(any(CommerceRelationship.class));
    }

    @Test
    @DisplayName("saveEdge updates weight and metadata if edge already exists")
    void testUpdateExistingEdge() {
        CommerceRelationship existing = new CommerceRelationship(
                "Product", "p-1", "FREQUENTLY_BOUGHT_WITH", "Product", "p-2", BigDecimal.valueOf(1), BigDecimal.valueOf(0.5)
        );
        existing.setId(UUID.randomUUID());

        when(jpaRepository.findBySourceTypeAndSourceIdAndRelationshipTypeAndTargetTypeAndTargetId(
                "Product", "p-1", "FREQUENTLY_BOUGHT_WITH", "Product", "p-2"
        )).thenReturn(Optional.of(existing));

        when(jpaRepository.save(any(CommerceRelationship.class))).thenAnswer(i -> i.getArgument(0));

        CommerceRelationship newEdge = new CommerceRelationship(
                "Product", "p-1", "FREQUENTLY_BOUGHT_WITH", "Product", "p-2", BigDecimal.valueOf(5), BigDecimal.valueOf(0.95)
        );
        newEdge.setMetadata(Map.of("coOccurrence", 5));

        CommerceRelationship updated = repository.saveEdge(newEdge);
        assertEquals(BigDecimal.valueOf(5), updated.getWeight());
        assertEquals(BigDecimal.valueOf(0.95), updated.getConfidence());
        assertEquals(5, updated.getMetadata().get("coOccurrence"));
    }

    @Test
    @DisplayName("getRelationshipTypeCounts returns aggregated counts")
    void testGetCounts() {
        Object[] r1 = new Object[]{"PURCHASED", 15L};
        Object[] r2 = new Object[]{"FREQUENTLY_BOUGHT_WITH", 8L};
        when(jpaRepository.countByRelationshipType()).thenReturn(List.of(r1, r2));

        Map<String, Long> counts = repository.getRelationshipTypeCounts();
        assertEquals(2, counts.size());
        assertEquals(15L, counts.get("PURCHASED"));
        assertEquals(8L, counts.get("FREQUENTLY_BOUGHT_WITH"));
    }
}
