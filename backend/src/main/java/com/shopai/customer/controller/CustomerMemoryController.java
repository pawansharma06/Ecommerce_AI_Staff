package com.shopai.customer.controller;

import com.shopai.common.dto.ApiResponse;
import com.shopai.customer.domain.CustomerMemory;
import com.shopai.customer.dto.CreateCustomerMemoryRequest;
import com.shopai.customer.dto.CustomerMemoryResponse;
import com.shopai.customer.service.CustomerMemoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customers/memory")
@Tag(name = "Customer Memory", description = "Customer-Level Context, Preferences, and Semantic Memory Facts")
public class CustomerMemoryController {

    private final CustomerMemoryService memoryService;

    public CustomerMemoryController(CustomerMemoryService memoryService) {
        this.memoryService = memoryService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('order.read')")
    @Operation(summary = "Get all memory facts for a customer by email")
    public ResponseEntity<ApiResponse<List<CustomerMemoryResponse>>> getCustomerMemories(@RequestParam String email) {
        List<CustomerMemory> list = memoryService.getMemoriesByEmail(email);
        List<CustomerMemoryResponse> responses = list.stream().map(CustomerMemoryResponse::from).toList();
        return ResponseEntity.ok(ApiResponse.ok(responses));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('order.read')")
    @Operation(summary = "Record or update a customer memory fact with vector embedding")
    public ResponseEntity<ApiResponse<CustomerMemoryResponse>> recordCustomerMemory(@Valid @RequestBody CreateCustomerMemoryRequest request) {
        CustomerMemory memory = memoryService.recordMemory(
                request.customerEmail(),
                request.category(),
                request.memoryKey(),
                request.memoryValue(),
                request.confidenceScore()
        );
        return ResponseEntity.ok(ApiResponse.ok(CustomerMemoryResponse.from(memory)));
    }

    public record SearchCustomerMemoryDto(String email, String query, Double minScore, Integer limit) {}

    @PostMapping("/search")
    @PreAuthorize("hasAuthority('order.read')")
    @Operation(summary = "Semantic similarity search on customer memory facts")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> searchMemories(
            @RequestBody(required = false) SearchCustomerMemoryDto body,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0.5") double minScore,
            @RequestParam(defaultValue = "5") int limit
    ) {
        String effectiveEmail = body != null && body.email() != null ? body.email() : email;
        String effectiveQuery = body != null && body.query() != null ? body.query() : query;
        double effectiveScore = body != null && body.minScore() != null ? body.minScore() : minScore;
        int effectiveLimit = body != null && body.limit() != null ? body.limit() : limit;

        List<Map<String, Object>> results = memoryService.searchCustomerMemories(effectiveEmail, effectiveQuery, effectiveScore, effectiveLimit);
        return ResponseEntity.ok(ApiResponse.ok(results));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('order.read')")
    @Operation(summary = "Delete customer memory fact")
    public ResponseEntity<ApiResponse<Map<String, String>>> deleteMemory(@PathVariable UUID id) {
        memoryService.deleteMemory(id);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("message", "Customer memory deleted successfully")));
    }
}
