package com.shopai.tool.impl;

import com.shopai.catalog.domain.ProductVariant;
import com.shopai.catalog.repository.ProductVariantRepository;
import com.shopai.tool.core.Tool;
import com.shopai.tool.core.ToolRiskLevel;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Component
public class UpdateInventoryTool implements Tool {

    private final ProductVariantRepository variantRepository;

    public UpdateInventoryTool(ProductVariantRepository variantRepository) {
        this.variantRepository = variantRepository;
    }

    @Override
    public String getName() {
        return "update_inventory";
    }

    @Override
    public String getDescription() {
        return "Adjust or set product inventory quantities for a variant by SKU or Shopify Variant ID.";
    }

    @Override
    public String getRequiredPermission() {
        return "product.write";
    }

    @Override
    public ToolRiskLevel getRiskLevel() {
        return ToolRiskLevel.MEDIUM;
    }

    @Override
    public Map<String, Object> getParametersSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "sku", Map.of("type", "string", "description", "Stock Keeping Unit (SKU) of the product variant"),
                        "shopifyVariantId", Map.of("type", "integer", "description", "Shopify Variant ID"),
                        "delta", Map.of("type", "integer", "description", "Relative adjustment to quantity (+5, -2)"),
                        "setQuantity", Map.of("type", "integer", "description", "Absolute new inventory quantity to set")
                )
        );
    }

    @Override
    @Transactional
    public Object execute(Map<String, Object> params) {
        String sku = (String) params.get("sku");
        Number variantIdNum = (Number) params.get("shopifyVariantId");
        Number deltaNum = (Number) params.get("delta");
        Number setQuantityNum = (Number) params.get("setQuantity");

        if ((sku == null || sku.isBlank()) && variantIdNum == null) {
            return Map.of("error", "Either sku or shopifyVariantId must be provided");
        }

        if (deltaNum == null && setQuantityNum == null) {
            return Map.of("error", "Either delta or setQuantity must be provided");
        }

        Optional<ProductVariant> variantOpt = Optional.empty();
        if (sku != null && !sku.isBlank()) {
            variantOpt = variantRepository.findBySku(sku.trim());
        }
        if (variantOpt.isEmpty() && variantIdNum != null) {
            variantOpt = variantRepository.findByShopifyVariantId(variantIdNum.longValue());
        }

        if (variantOpt.isEmpty()) {
            return Map.of("found", false, "message", "Product variant not found");
        }

        ProductVariant variant = variantOpt.get();
        int oldQuantity = variant.getInventoryQuantity() != null ? variant.getInventoryQuantity() : 0;
        int newQuantity;

        if (setQuantityNum != null) {
            newQuantity = setQuantityNum.intValue();
        } else {
            newQuantity = oldQuantity + deltaNum.intValue();
        }

        if (newQuantity < 0) {
            newQuantity = 0;
        }

        variant.setInventoryQuantity(newQuantity);
        variant.setUpdatedAt(Instant.now());
        variantRepository.save(variant);

        return Map.of(
                "success", true,
                "sku", variant.getSku() != null ? variant.getSku() : "",
                "variantTitle", variant.getTitle(),
                "shopifyVariantId", variant.getShopifyVariantId(),
                "previousQuantity", oldQuantity,
                "newQuantity", newQuantity,
                "updatedAt", variant.getUpdatedAt().toString()
        );
    }
}
