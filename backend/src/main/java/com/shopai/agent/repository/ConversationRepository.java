package com.shopai.agent.repository;

import com.shopai.agent.domain.Conversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    @Query("""
        SELECT c FROM Conversation c
        WHERE (CAST(:agentType AS string) IS NULL OR c.agentType = :agentType)
          AND (CAST(:customerEmail AS string) IS NULL OR LOWER(c.customerEmail) = LOWER(CAST(:customerEmail AS string)))
          AND (CAST(:search AS string) IS NULL OR LOWER(c.title) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
        ORDER BY c.updatedAt DESC
    """)
    Page<Conversation> findWithFilters(
            @Param("agentType") String agentType,
            @Param("customerEmail") String customerEmail,
            @Param("search") String search,
            Pageable pageable
    );

    List<Conversation> findByCustomerEmailOrderByUpdatedAtDesc(String customerEmail);
}
