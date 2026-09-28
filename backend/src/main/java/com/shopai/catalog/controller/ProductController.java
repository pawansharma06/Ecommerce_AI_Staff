package com.shopai.catalog.controller;

import com.shopai.catalog.dto.CatalogStatsResponse;
import com.shopai.catalog.dto.CollectionResponse;
import com.shopai.catalog.dto.ProductResponse;
import com.shopai.catalog.service.ProductService;
import com.shopai.common.dto.ApiResponse;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('product.read')")
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> getProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String vendor
    ) {
        Page<ProductResponse> products = productService.getProducts(page, size, search, status, vendor);
        return ResponseEntity.ok(ApiResponse.ok(products));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('product.read')")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(@PathVariable UUID id) {
        ProductResponse product = productService.getProductById(id);
        return ResponseEntity.ok(ApiResponse.ok(product));
    }

    @GetMapping("/collections")
    @PreAuthorize("hasAuthority('product.read')")
    public ResponseEntity<ApiResponse<List<CollectionResponse>>> getCollections() {
        List<CollectionResponse> collections = productService.getCollections();
        return ResponseEntity.ok(ApiResponse.ok(collections));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('product.read')")
    public ResponseEntity<ApiResponse<CatalogStatsResponse>> getCatalogStats() {
        CatalogStatsResponse stats = productService.getCatalogStats();
        return ResponseEntity.ok(ApiResponse.ok(stats));
    }
}
