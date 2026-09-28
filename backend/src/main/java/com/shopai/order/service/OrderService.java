package com.shopai.order.service;

import com.shopai.common.exception.ResourceNotFoundException;
import com.shopai.order.domain.AbandonedCheckout;
import com.shopai.order.domain.Order;
import com.shopai.order.dto.AbandonedCheckoutResponse;
import com.shopai.order.dto.OrderResponse;
import com.shopai.order.dto.OrderStatsResponse;
import com.shopai.order.repository.AbandonedCheckoutRepository;
import com.shopai.order.repository.OrderRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final AbandonedCheckoutRepository abandonedCheckoutRepository;

    public OrderService(
            OrderRepository orderRepository,
            AbandonedCheckoutRepository abandonedCheckoutRepository
    ) {
        this.orderRepository = orderRepository;
        this.abandonedCheckoutRepository = abandonedCheckoutRepository;
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getOrders(int page, int size, String search, String financialStatus, String fulfillmentStatus) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "shopifyCreatedAt"));
        String cleanSearch = (search != null && !search.isBlank()) ? search.trim() : null;
        String cleanFinancial = (financialStatus != null && !financialStatus.isBlank() && !"ALL".equalsIgnoreCase(financialStatus)) ? financialStatus.trim().toUpperCase() : null;
        String cleanFulfillment = (fulfillmentStatus != null && !fulfillmentStatus.isBlank() && !"ALL".equalsIgnoreCase(fulfillmentStatus)) ? fulfillmentStatus.trim().toUpperCase() : null;

        return orderRepository.findWithFilters(cleanSearch, cleanFinancial, cleanFulfillment, pageable)
                .map(OrderResponse::from);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(UUID id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", id.toString()));
        return OrderResponse.from(order);
    }

    @Transactional(readOnly = true)
    public Page<AbandonedCheckoutResponse> getAbandonedCheckouts(int page, int size, String search, String recoveryStatus) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "shopifyCreatedAt"));
        String cleanSearch = (search != null && !search.isBlank()) ? search.trim() : null;
        String cleanStatus = (recoveryStatus != null && !recoveryStatus.isBlank() && !"ALL".equalsIgnoreCase(recoveryStatus)) ? recoveryStatus.trim().toUpperCase() : null;

        return abandonedCheckoutRepository.findWithFilters(cleanSearch, cleanStatus, pageable)
                .map(AbandonedCheckoutResponse::from);
    }

    @Transactional(readOnly = true)
    public AbandonedCheckoutResponse getAbandonedCheckoutById(UUID id) {
        AbandonedCheckout checkout = abandonedCheckoutRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AbandonedCheckout", id.toString()));
        return AbandonedCheckoutResponse.from(checkout);
    }

    @Transactional(readOnly = true)
    public OrderStatsResponse getOrderStats() {
        long totalOrders = orderRepository.count();
        long unfulfilled = orderRepository.countByFulfillmentStatus("UNFULFILLED");
        long partiallyFulfilled = orderRepository.countByFulfillmentStatus("PARTIALLY_FULFILLED");
        long fulfilled = orderRepository.countByFulfillmentStatus("FULFILLED");
        long paid = orderRepository.countByFinancialStatus("PAID");
        long pending = orderRepository.countByFinancialStatus("PENDING");
        BigDecimal totalSales = orderRepository.sumTotalSales();

        long abandonedCount = abandonedCheckoutRepository.countByRecoveryStatus("ABANDONED");
        long recoveredCount = abandonedCheckoutRepository.countByRecoveryStatus("RECOVERED");
        BigDecimal totalAbandonedValue = abandonedCheckoutRepository.sumAbandonedTotal();

        return new OrderStatsResponse(
                totalOrders,
                unfulfilled,
                partiallyFulfilled,
                fulfilled,
                paid,
                pending,
                totalSales,
                abandonedCount,
                recoveredCount,
                totalAbandonedValue
        );
    }
}
