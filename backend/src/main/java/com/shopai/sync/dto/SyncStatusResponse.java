package com.shopai.sync.dto;

import com.shopai.sync.domain.SyncJob;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SyncStatusResponse(
        UUID id,
        String jobType,
        String status,
        Integer itemsProcessed,
        String errorMessage,
        Instant startedAt,
        Instant completedAt,
        List<SyncJobSummary> recentJobs
) {
    public record SyncJobSummary(
            UUID id,
            String jobType,
            String status,
            Integer itemsProcessed,
            Instant startedAt,
            Instant completedAt
    ) {
        public static SyncJobSummary from(SyncJob j) {
            return new SyncJobSummary(
                    j.getId(),
                    j.getJobType(),
                    j.getStatus(),
                    j.getItemsProcessed(),
                    j.getStartedAt(),
                    j.getCompletedAt()
            );
        }
    }

    public static SyncStatusResponse from(SyncJob current, List<SyncJob> recent) {
        List<SyncJobSummary> summaries = recent != null
                ? recent.stream().map(SyncJobSummary::from).toList()
                : List.of();

        if (current == null) {
            return new SyncStatusResponse(null, "NONE", "IDLE", 0, null, null, null, summaries);
        }

        return new SyncStatusResponse(
                current.getId(),
                current.getJobType(),
                current.getStatus(),
                current.getItemsProcessed(),
                current.getErrorMessage(),
                current.getStartedAt(),
                current.getCompletedAt(),
                summaries
        );
    }
}
