package com.shopai.order.controller;

import com.shopai.common.dto.ApiResponse;
import com.shopai.order.dto.AbandonedCheckoutResponse;
import com.shopai.order.service.OrderService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/abandoned-checkouts")
public class AbandonedCheckoutController {

    private final OrderService orderService;

    public AbandonedCheckoutController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('order.read')")
    public ResponseEntity<ApiResponse<Page<AbandonedCheckoutResponse>>> getAbandonedCheckouts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String recoveryStatus
    ) {
        Page<AbandonedCheckoutResponse> checkouts = orderService.getAbandonedCheckouts(page, size, search, recoveryStatus);
        return ResponseEntity.ok(ApiResponse.ok(checkouts));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('order.read')")
    public ResponseEntity<ApiResponse<AbandonedCheckoutResponse>> getAbandonedCheckoutById(@PathVariable UUID id) {
        AbandonedCheckoutResponse checkout = orderService.getAbandonedCheckoutById(id);
        return ResponseEntity.ok(ApiResponse.ok(checkout));
    }
}
