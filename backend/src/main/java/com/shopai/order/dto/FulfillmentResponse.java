package com.shopai.order.dto;

import com.shopai.order.domain.Fulfillment;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public record FulfillmentResponse(
        UUID id,
        Long shopifyFulfillmentId,
        String status,
        String trackingCompany,
        String trackingNumber,
        List<String> trackingNumbers,
        String trackingUrl,
        List<String> trackingUrls,
        String service,
        String shipmentStatus,
        Instant estimatedDeliveryAt,
        Instant deliveredAt,
        Instant shopifyCreatedAt,
        List<FulfillmentLineItemResponse> items
) {
    public static FulfillmentResponse from(Fulfillment f) {
        List<FulfillmentLineItemResponse> items = f.getFulfillmentLineItems() != null
                ? f.getFulfillmentLineItems().stream().map(FulfillmentLineItemResponse::from).collect(Collectors.toList())
                : List.of();

        List<String> numbers = f.getTrackingNumbers() != null ? Arrays.asList(f.getTrackingNumbers()) : List.of();
        List<String> urls = f.getTrackingUrls() != null ? Arrays.asList(f.getTrackingUrls()) : List.of();

        return new FulfillmentResponse(
                f.getId(),
                f.getShopifyFulfillmentId(),
                f.getStatus(),
                f.getTrackingCompany(),
                f.getTrackingNumber(),
                numbers,
                f.getTrackingUrl(),
                urls,
                f.getService(),
                f.getShipmentStatus(),
                f.getEstimatedDeliveryAt(),
                f.getDeliveredAt(),
                f.getShopifyCreatedAt(),
                items
        );
    }
}
