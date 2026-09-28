package com.shopai.sync.controller;

import com.shopai.common.dto.ApiResponse;
import com.shopai.sync.domain.SyncJob;
import com.shopai.sync.dto.SyncStatusResponse;
import com.shopai.sync.service.DataSyncService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/sync")
public class DataSyncController {

    private final DataSyncService dataSyncService;

    public DataSyncController(DataSyncService dataSyncService) {
        this.dataSyncService = dataSyncService;
    }

    @PostMapping("/catalog")
    @PreAuthorize("hasAuthority('integration.manage')")
    public ResponseEntity<ApiResponse<SyncJob>> triggerCatalogSync() {
        SyncJob job = dataSyncService.triggerSync("CATALOG_SYNC");
        return ResponseEntity.ok(ApiResponse.ok(job));
    }

    @PostMapping("/orders")
    @PreAuthorize("hasAuthority('integration.manage')")
    public ResponseEntity<ApiResponse<SyncJob>> triggerOrderSync() {
        SyncJob job = dataSyncService.triggerSync("ORDER_SYNC");
        return ResponseEntity.ok(ApiResponse.ok(job));
    }

    @GetMapping("/status")
    @PreAuthorize("hasAnyAuthority('integration.manage', 'product.read', 'order.read')")
    public ResponseEntity<ApiResponse<SyncStatusResponse>> getSyncStatus(@RequestParam(required = false) String jobType) {
        SyncStatusResponse status = dataSyncService.getStatus(jobType);
        return ResponseEntity.ok(ApiResponse.ok(status));
    }
}
