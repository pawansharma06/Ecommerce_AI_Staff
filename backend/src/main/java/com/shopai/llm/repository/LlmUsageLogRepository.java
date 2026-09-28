package com.shopai.llm.repository;

import com.shopai.llm.domain.LlmUsageLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LlmUsageLogRepository extends JpaRepository<LlmUsageLog, UUID> {

    List<LlmUsageLog> findTop50ByOrderByCreatedAtDesc();

    @Query("SELECT coalesce(sum(l.totalTokens), 0) FROM LlmUsageLog l")
    long getTotalTokensUsed();

    @Query("SELECT coalesce(sum(l.costUsd), 0) FROM LlmUsageLog l")
    java.math.BigDecimal getTotalCostUsd();
}