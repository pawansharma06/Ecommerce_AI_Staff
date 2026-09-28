package com.shopai.sync.service;

import com.shopai.catalog.service.CatalogSyncService;
import com.shopai.order.service.OrderSyncService;
import com.shopai.sync.domain.SyncJob;
import com.shopai.sync.dto.SyncStatusResponse;
import com.shopai.sync.repository.SyncJobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class DataSyncService {

    private static final Logger log = LoggerFactory.getLogger(DataSyncService.class);

    private final CatalogSyncService catalogSyncService;
    private final OrderSyncService orderSyncService;
    private final SyncJobRepository syncJobRepository;

    public DataSyncService(
            CatalogSyncService catalogSyncService,
            OrderSyncService orderSyncService,
            SyncJobRepository syncJobRepository
    ) {
        this.catalogSyncService = catalogSyncService;
        this.orderSyncService = orderSyncService;
        this.syncJobRepository = syncJobRepository;
    }

    public SyncJob triggerSync(String jobType) {
        SyncJob job = new SyncJob(jobType.toUpperCase());
        job.setStatus("IN_PROGRESS");
        job.setStartedAt(Instant.now());
        job = syncJobRepository.save(job);

        runSyncAsync(job.getId(), job.getJobType());
        return job;
    }

    @Async
    public CompletableFuture<Void> runSyncAsync(java.util.UUID jobId, String jobType) {
        try {
            SyncJob job = syncJobRepository.findById(jobId).orElseThrow();
            int count = 0;
            if ("CATALOG_SYNC".equalsIgnoreCase(jobType)) {
                count = catalogSyncService.performFullCatalogSync(job);
            } else if ("ORDER_SYNC".equalsIgnoreCase(jobType)) {
                count = orderSyncService.performFullOrderSync(job);
            }
            job.setItemsProcessed(count);
            job.setStatus("COMPLETED");
            job.setCompletedAt(Instant.now());
            syncJobRepository.save(job);
            log.info("Sync job {} ({}) completed successfully. Processed: {}", jobId, jobType, count);
        } catch (Exception e) {
            log.error("Sync job {} ({}) failed: {}", jobId, jobType, e.getMessage(), e);
            syncJobRepository.findById(jobId).ifPresent(job -> {
                job.setStatus("FAILED");
                job.setErrorMessage(e.getMessage());
                job.setCompletedAt(Instant.now());
                syncJobRepository.save(job);
            });
        }
        return CompletableFuture.completedFuture(null);
    }

    public SyncStatusResponse getStatus(String jobType) {
        SyncJob current = (jobType != null && !jobType.isBlank())
                ? syncJobRepository.findTopByJobTypeOrderByStartedAtDesc(jobType.toUpperCase()).orElse(null)
                : syncJobRepository.findTop10ByOrderByStartedAtDesc().stream().findFirst().orElse(null);

        List<SyncJob> recent = syncJobRepository.findTop10ByOrderByStartedAtDesc();
        return SyncStatusResponse.from(current, recent);
    }
}
