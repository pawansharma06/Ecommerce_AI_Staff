package com.shopai.tool.impl;

import com.shopai.catalog.domain.Product;
import com.shopai.catalog.repository.ProductRepository;
import com.shopai.tool.core.Tool;
import com.shopai.tool.core.ToolRiskLevel;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class GetProductDetailsTool implements Tool {

    private final ProductRepository productRepository;

    public GetProductDetailsTool(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public String getName() {
        return "get_product_details";
    }

    @Override
    public String getDescription() {
        return "Get comprehensive product details including variant pricing, live stock, and SKUs.";
    }

    @Override
    public String getRequiredPermission() {
        return "product.read";
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
                        "handle", Map.of("type", "string", "description", "Product handle (slug)"),
                        "productId", Map.of("type", "string", "description", "UUID or Shopify Product ID")
                )
        );
    }

    @Override
    public Object execute(Map<String, Object> params) {
        String handle = (String) params.get("handle");
        String productId = (String) params.get("productId");

        Optional<Product> prodOpt = Optional.empty();
        if (handle != null && !handle.isBlank()) {
            prodOpt = productRepository.findByHandle(handle.trim());
        } else if (productId != null && !productId.isBlank()) {
            try {
                prodOpt = productRepository.findById(UUID.fromString(productId));
            } catch (Exception e) {
                try {
                    prodOpt = productRepository.findByShopifyProductId(Long.parseLong(productId));
                } catch (Exception ex) {
                    // ignore
                }
            }
        }

        if (prodOpt.isEmpty()) {
            return Map.of("error", "Product not found");
        }

        Product p = prodOpt.get();
        List<Map<String, Object>> variantsList = new ArrayList<>();
        if (p.getVariants() != null) {
            p.getVariants().forEach(v -> variantsList.add(Map.of(
                    "id", v.getId(),
                    "title", v.getTitle(),
                    "sku", v.getSku() != null ? v.getSku() : "",
                    "price", v.getPrice() != null ? v.getPrice() : "0.00",
                    "compareAtPrice", v.getCompareAtPrice() != null ? v.getCompareAtPrice() : "0.00",
                    "inventoryQuantity", v.getInventoryQuantity(),
                    "imageUrl", v.getImageUrl() != null ? v.getImageUrl() : ""
            )));
        }

        return Map.of(
                "id", p.getId(),
                "shopifyProductId", p.getShopifyProductId(),
                "title", p.getTitle(),
                "handle", p.getHandle(),
                "vendor", p.getVendor() != null ? p.getVendor() : "",
                "productType", p.getProductType() != null ? p.getProductType() : "",
                "status", p.getStatus(),
                "totalInventory", p.getTotalInventory(),
                "variants", variantsList
        );
    }
}