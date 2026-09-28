package com.shopai.order.controller;

import com.shopai.common.dto.ApiResponse;
import com.shopai.order.dto.OrderResponse;
import com.shopai.order.dto.OrderStatsResponse;
import com.shopai.order.service.OrderService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('order.read')")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String financialStatus,
            @RequestParam(required = false) String fulfillmentStatus
    ) {
        Page<OrderResponse> orders = orderService.getOrders(page, size, search, financialStatus, fulfillmentStatus);
        return ResponseEntity.ok(ApiResponse.ok(orders));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('order.read')")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(@PathVariable UUID id) {
        OrderResponse order = orderService.getOrderById(id);
        return ResponseEntity.ok(ApiResponse.ok(order));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('order.read')")
    public ResponseEntity<ApiResponse<OrderStatsResponse>> getOrderStats() {
        OrderStatsResponse stats = orderService.getOrderStats();
        return ResponseEntity.ok(ApiResponse.ok(stats));
    }
}
