package com.shopai.tool.impl;

import com.shopai.order.domain.Order;
import com.shopai.order.domain.OrderTransaction;
import com.shopai.order.repository.OrderRepository;
import com.shopai.order.repository.OrderTransactionRepository;
import com.shopai.tool.core.Tool;
import com.shopai.tool.core.ToolRiskLevel;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Component
public class RefundOrderTool implements Tool {

    private final OrderRepository orderRepository;
    private final OrderTransactionRepository transactionRepository;

    public RefundOrderTool(OrderRepository orderRepository, OrderTransactionRepository transactionRepository) {
        this.orderRepository = orderRepository;
        this.transactionRepository = transactionRepository;
    }

    @Override
    public String getName() {
        return "refund_order";
    }

    @Override
    public String getDescription() {
        return "Issue a full or partial refund for an existing order. HIGH RISK ACTION: Requires human merchant approval before execution.";
    }

    @Override
    public String getRequiredPermission() {
        return "order.refund";
    }

    @Override
    public ToolRiskLevel getRiskLevel() {
        return ToolRiskLevel.HIGH;
    }

    @Override
    public boolean requiresHumanApproval() {
        return true;
    }

    @Override
    public Map<String, Object> getParametersSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "orderNumber", Map.of("type", "string", "description", "Order number or reference (e.g. '#1001')"),
                        "amount", Map.of("type", "number", "description", "Refund amount in order currency"),
                        "reason", Map.of("type", "string", "description", "Reason for refund (customer request, damaged goods, return)"),
                        "restock", Map.of("type", "boolean", "description", "Whether items should be restocked in inventory")
                ),
                "required", List.of("orderNumber", "amount", "reason")
        );
    }

    @Override
    @Transactional
    public Object execute(Map<String, Object> params) {
        String orderNumber = (String) params.get("orderNumber");
        Number amountNum = (Number) params.get("amount");
        String reason = (String) params.get("reason");

        if (orderNumber == null || orderNumber.isBlank()) {
            return Map.of("error", "orderNumber is required");
        }
        if (amountNum == null) {
            return Map.of("error", "amount is required");
        }

        String cleanNum = orderNumber.trim();
        Optional<Order> orderOpt = orderRepository.findByOrderNumber(cleanNum);
        if (orderOpt.isEmpty() && cleanNum.startsWith("#")) {
            orderOpt = orderRepository.findByOrderNumber(cleanNum.substring(1));
        } else if (orderOpt.isEmpty()) {
            orderOpt = orderRepository.findByOrderNumber("#" + cleanNum);
        }

        if (orderOpt.isEmpty()) {
            return Map.of("success", false, "error", "Order not found: " + orderNumber);
        }

        Order order = orderOpt.get();
        BigDecimal refundAmount = BigDecimal.valueOf(amountNum.doubleValue());

        // Create transaction record
        OrderTransaction tx = new OrderTransaction();
        tx.setOrder(order);
        tx.setShopifyTransactionId(System.currentTimeMillis());
        tx.setKind("REFUND");
        tx.setStatus("SUCCESS");
        tx.setAmount(refundAmount);
        tx.setCurrency(order.getCurrency());
        tx.setProcessedAt(Instant.now());
        tx.setErrorMessage(reason);
        transactionRepository.save(tx);

        if (refundAmount.compareTo(order.getTotalPrice()) >= 0) {
            order.setFinancialStatus("REFUNDED");
        } else {
            order.setFinancialStatus("PARTIALLY_REFUNDED");
        }
        order.setUpdatedAt(Instant.now());
        orderRepository.save(order);

        return Map.of(
                "success", true,
                "orderNumber", order.getOrderNumber(),
                "refundAmount", refundAmount,
                "currency", order.getCurrency(),
                "financialStatus", order.getFinancialStatus(),
                "reason", reason != null ? reason : "Merchant approved refund",
                "processedAt", Instant.now().toString()
        );
    }
}
