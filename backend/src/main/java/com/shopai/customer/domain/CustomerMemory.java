package com.shopai.customer.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "customer_memories")
public class CustomerMemory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "customer_email", nullable = false, length = 255)
    private String customerEmail;

    @Column(nullable = false, length = 50)
    private String category = "PREFERENCE"; // PREFERENCE, ORDER_FACT, ADDRESS_FACT, FEEDBACK, INTERACTION

    @Column(name = "memory_key", nullable = false, length = 255)
    private String memoryKey;

    @Column(name = "memory_value", nullable = false, columnDefinition = "TEXT")
    private String memoryValue;

    @Column(name = "confidence_score", nullable = false, precision = 3, scale = 2)
    private BigDecimal confidenceScore = BigDecimal.ONE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public CustomerMemory() {}

    public CustomerMemory(String customerEmail, String category, String memoryKey, String memoryValue) {
        this.customerEmail = customerEmail;
        this.category = category;
        this.memoryKey = memoryKey;
        this.memoryValue = memoryValue;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }

    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getMemoryKey() { return memoryKey; }
    public void setMemoryKey(String memoryKey) { this.memoryKey = memoryKey; }

    public String getMemoryValue() { return memoryValue; }
    public void setMemoryValue(String memoryValue) { this.memoryValue = memoryValue; }

    public BigDecimal getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(BigDecimal confidenceScore) { this.confidenceScore = confidenceScore; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
