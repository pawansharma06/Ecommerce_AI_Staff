package com.shopai.customer;

import com.shopai.customer.domain.CustomerMemory;
import com.shopai.customer.repository.CustomerMemoryRepository;
import com.shopai.customer.service.CustomerMemoryService;
import com.shopai.rag.embedding.EmbeddingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerMemoryServiceTest {

    @Mock
    private CustomerMemoryRepository customerMemoryRepository;

    @Mock
    private EmbeddingService embeddingService;

    @InjectMocks
    private CustomerMemoryService customerMemoryService;

    @Test
    void testRecordNewMemory() {
        when(embeddingService.getEmbeddingAsVectorString(anyString())).thenReturn("[0.1,0.2]");
        when(customerMemoryRepository.findByCustomerEmailAndMemoryKey("alice@example.com", "preferred_size")).thenReturn(Optional.empty());

        CustomerMemory mem = customerMemoryService.recordMemory(
                "alice@example.com",
                "PREFERENCE",
                "preferred_size",
                "Large",
                BigDecimal.valueOf(0.95)
        );

        assertThat(mem).isNotNull();
        assertThat(mem.getCustomerEmail()).isEqualTo("alice@example.com");
        assertThat(mem.getMemoryKey()).isEqualTo("preferred_size");
        assertThat(mem.getMemoryValue()).isEqualTo("Large");
        verify(customerMemoryRepository, times(1)).insertMemoryWithEmbedding(
                any(UUID.class),
                isNull(),
                eq("alice@example.com"),
                eq("PREFERENCE"),
                eq("preferred_size"),
                eq("Large"),
                eq(BigDecimal.valueOf(0.95)),
                eq("[0.1,0.2]")
        );
    }

    @Test
    void testGetMemoriesByEmail() {
        CustomerMemory m = new CustomerMemory("bob@example.com", "PREFERENCE", "shoe_size", "10");
        when(customerMemoryRepository.findByCustomerEmailOrderByCreatedAtDesc("bob@example.com"))
                .thenReturn(List.of(m));

        List<CustomerMemory> list = customerMemoryService.getMemoriesByEmail("bob@example.com");
        assertThat(list).hasSize(1);
        assertThat(list.get(0).getMemoryKey()).isEqualTo("shoe_size");
    }
}