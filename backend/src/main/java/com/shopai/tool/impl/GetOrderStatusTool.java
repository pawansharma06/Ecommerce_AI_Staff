package com.shopai.tool.impl;

import com.shopai.order.domain.Fulfillment;
import com.shopai.order.domain.Order;
import com.shopai.order.domain.OrderLineItem;
import com.shopai.order.repository.OrderRepository;
import com.shopai.tool.core.Tool;
import com.shopai.tool.core.ToolRiskLevel;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class GetOrderStatusTool implements Tool {

    private final OrderRepository orderRepository;

    public GetOrderStatusTool(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public String getName() {
        return "get_order_status";
    }

    @Override
    public String getDescription() {
        return "Retrieve fulfillment, shipping carrier tracking URLs, and payment status for an order by order number or customer email.";
    }

    @Override
    public String getRequiredPermission() {
        return "order.read";
    }

    @Override
    public ToolRiskLevel getRiskLevel() {
        return ToolRiskLevel.LOW;
    }

    @Override
    public Map<String, Object> getParametersSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "orderNumber", Map.of("type", "string", "description", "Order number or reference (e.g. '#1001' or '1001')"),
                        "email", Map.of("type", "string", "description", "Customer email address to find latest order")
                )
        );
    }

    @Override
    public Object execute(Map<String, Object> params) {
        String orderNumber = (String) params.get("orderNumber");
        String email = (String) params.get("email");

        if ((orderNumber == null || orderNumber.isBlank()) && (email == null || email.isBlank())) {
            return Map.of("error", "Either orderNumber or email must be provided");
        }

        Optional<Order> orderOpt = Optional.empty();

        if (orderNumber != null && !orderNumber.isBlank()) {
            String cleanNum = orderNumber.trim();
            orderOpt = orderRepository.findByOrderNumber(cleanNum);
            if (orderOpt.isEmpty() && cleanNum.startsWith("#")) {
                orderOpt = orderRepository.findByOrderNumber(cleanNum.substring(1));
            } else if (orderOpt.isEmpty()) {
                orderOpt = orderRepository.findByOrderNumber("#" + cleanNum);
            }
        }

        if (orderOpt.isEmpty() && email != null && !email.isBlank()) {
            var page = orderRepository.findWithFilters(email.trim(), null, null, org.springframework.data.domain.PageRequest.of(0, 1));
            if (!page.isEmpty()) {
                orderOpt = Optional.of(page.getContent().get(0));
            }
        }

        if (orderOpt.isEmpty()) {
            return Map.of("found", false, "message", "No order found matching the criteria");
        }

        Order order = orderOpt.get();

        List<Map<String, Object>> fulfillments = order.getFulfillments().stream().map(f -> {
            Map<String, Object> fMap = new HashMap<>();
            fMap.put("status", f.getStatus());
            fMap.put("trackingCompany", f.getTrackingCompany());
            fMap.put("trackingNumber", f.getTrackingNumber());
            fMap.put("trackingUrl", f.getTrackingUrl());
            fMap.put("shipmentStatus", f.getShipmentStatus());
            fMap.put("deliveredAt", f.getDeliveredAt());
            return fMap;
        }).toList();

        List<Map<String, Object>> items = order.getLineItems().stream().map(item -> Map.<String, Object>of(
                "title", item.getTitle(),
                "quantity", item.getQuantity(),
                "price", item.getPrice(),
                "sku", item.getSku() != null ? item.getSku() : ""
        )).toList();

        Map<String, Object> resp = new HashMap<>();
        resp.put("found", true);
        resp.put("orderNumber", order.getOrderNumber());
        resp.put("name", order.getName());
        resp.put("financialStatus", order.getFinancialStatus());
        resp.put("fulfillmentStatus", order.getFulfillmentStatus());
        resp.put("totalPrice", order.getTotalPrice());
        resp.put("currency", order.getCurrency());
        resp.put("customerEmail", order.getCustomerEmail() != null ? order.getCustomerEmail() : "");
        resp.put("lineItems", items);
        resp.put("fulfillments", fulfillments);
        resp.put("createdAt", order.getCreatedAt().toString());
        return resp;
    }
}
