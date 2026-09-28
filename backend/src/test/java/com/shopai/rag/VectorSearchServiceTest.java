package com.shopai.rag;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopai.customer.service.CustomerMemoryService;
import com.shopai.rag.embedding.EmbeddingService;
import com.shopai.rag.repository.KnowledgeChunkRepository;
import com.shopai.rag.service.VectorSearchService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VectorSearchServiceTest {

    @Mock
    private KnowledgeChunkRepository chunkRepository;

    @Mock
    private CustomerMemoryService customerMemoryService;

    @Mock
    private EmbeddingService embeddingService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private VectorSearchService vectorSearchService;

    @Test
    void testSearchReturnsMappedResults() {
        when(embeddingService.getEmbeddingAsVectorString("return policy warranty")).thenReturn("[0.1,0.2]");

        Object[] row = new Object[]{
                UUID.randomUUID().toString(), // chunkId
                UUID.randomUUID().toString(), // docId
                0,                            // chunkIndex
                "All items can be returned within 30 days.", // content
                10,                           // tokenCount
                null,                         // email
                "{\"category\":\"shipping\"}",// metadataJson
                0.91,                         // score
                "POLICY",                     // type
                "30 Day Returns"              // title
        };

        when(chunkRepository.searchSimilarChunks(eq("[0.1,0.2]"), isNull(), eq("POLICY"), eq(0.50), eq(5)))
                .thenReturn(List.<Object[]>of(row));

        List<VectorSearchService.SearchResultItem> results = vectorSearchService.search(
                "return policy warranty",
                null,
                "POLICY",
                0.50,
                5
        );

        assertThat(results).hasSize(1);
        assertThat(results.get(0).content()).contains("30 days");
        assertThat(results.get(0).documentTitle()).isEqualTo("30 Day Returns");
        assertThat(results.get(0).similarityScore()).isEqualTo(0.91);
    }
}