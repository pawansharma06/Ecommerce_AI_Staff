package com.shopai.sync.repository;

import com.shopai.sync.domain.SyncJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SyncJobRepository extends JpaRepository<SyncJob, UUID> {
    Optional<SyncJob> findTopByJobTypeOrderByStartedAtDesc(String jobType);
    List<SyncJob> findTop10ByOrderByStartedAtDesc();
}
