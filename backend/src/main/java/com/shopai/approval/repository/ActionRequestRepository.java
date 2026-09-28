package com.shopai.approval.repository;

import com.shopai.approval.domain.ActionRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ActionRequestRepository extends JpaRepository<ActionRequest, UUID> {

    List<ActionRequest> findByStatusOrderByCreatedAtDesc(String status);

    @Query("SELECT a FROM ActionRequest a WHERE (:status IS NULL OR a.status = :status) ORDER BY a.createdAt DESC")
    Page<ActionRequest> findWithFilters(@Param("status") String status, Pageable pageable);

    long countByStatus(String status);
}