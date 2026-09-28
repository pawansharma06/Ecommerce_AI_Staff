package com.shopai.catalog.service;

import com.shopai.catalog.domain.Product;
import com.shopai.catalog.dto.CatalogStatsResponse;
import com.shopai.catalog.dto.CollectionResponse;
import com.shopai.catalog.dto.ProductResponse;
import com.shopai.catalog.repository.CollectionRepository;
import com.shopai.catalog.repository.ProductRepository;
import com.shopai.catalog.repository.ProductVariantRepository;
import com.shopai.common.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final CollectionRepository collectionRepository;

    public ProductService(
            ProductRepository productRepository,
            ProductVariantRepository productVariantRepository,
            CollectionRepository collectionRepository
    ) {
        this.productRepository = productRepository;
        this.productVariantRepository = productVariantRepository;
        this.collectionRepository = collectionRepository;
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> getProducts(int page, int size, String search, String status, String vendor) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
        String cleanSearch = (search != null && !search.isBlank()) ? search.trim() : null;
        String cleanStatus = (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) ? status.trim().toUpperCase() : null;
        String cleanVendor = (vendor != null && !vendor.isBlank()) ? vendor.trim() : null;

        return productRepository.findWithFilters(cleanSearch, cleanStatus, cleanVendor, pageable)
                .map(ProductResponse::from);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductById(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id.toString()));
        return ProductResponse.from(product);
    }

    @Transactional(readOnly = true)
    public List<CollectionResponse> getCollections() {
        return collectionRepository.findAll().stream()
                .map(CollectionResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CatalogStatsResponse getCatalogStats() {
        long total = productRepository.count();
        long active = productRepository.countByStatus("ACTIVE");
        long draft = productRepository.countByStatus("DRAFT");
        long archived = productRepository.countByStatus("ARCHIVED");
        long totalVariants = productVariantRepository.count();
        long lowStock = productVariantRepository.findAll().stream()
                .filter(v -> v.getInventoryQuantity() != null && v.getInventoryQuantity() <= 5)
                .count();

        return new CatalogStatsResponse(total, active, draft, archived, totalVariants, lowStock);
    }
}
