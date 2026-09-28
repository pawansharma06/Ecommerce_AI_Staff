package com.shopai.rag;

import com.shopai.rag.service.TextChunkingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TextChunkingServiceTest {

    private TextChunkingService chunkingService;

    @BeforeEach
    void setUp() {
        chunkingService = new TextChunkingService();
    }

    @Test
    void testChunkEmptyText() {
        List<TextChunkingService.Chunk> chunks = chunkingService.chunkText("");
        assertThat(chunks).isEmpty();

        List<TextChunkingService.Chunk> nullChunks = chunkingService.chunkText(null);
        assertThat(nullChunks).isEmpty();
    }

    @Test
    void testChunkShortText() {
        String text = "Returns are accepted within 30 days of delivery. Full refund issued.";
        List<TextChunkingService.Chunk> chunks = chunkingService.chunkText(text);

        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0).index()).isEqualTo(0);
        assertThat(chunks.get(0).content()).isEqualTo(text);
        assertThat(chunks.get(0).tokenCount()).isGreaterThan(0);
    }

    @Test
    void testChunkLongTextSplitsByParagraphs() {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 20; i++) {
            sb.append("Section ").append(i).append(": This is a comprehensive policy clause outlining merchant guarantees and customer protection rights in detail.\n\n");
        }

        List<TextChunkingService.Chunk> chunks = chunkingService.chunkText(sb.toString());
        assertThat(chunks).isNotEmpty();
        for (TextChunkingService.Chunk c : chunks) {
            assertThat(c.content()).isNotBlank();
            assertThat(c.tokenCount()).isLessThanOrEqualTo(600);
        }
    }
}
