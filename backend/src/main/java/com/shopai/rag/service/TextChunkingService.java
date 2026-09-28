package com.shopai.rag.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class TextChunkingService {

    public record Chunk(int index, String content, int tokenCount) {}

    private static final int DEFAULT_CHUNK_SIZE_TOKENS = 400;
    private static final int DEFAULT_CHUNK_OVERLAP_TOKENS = 50;
    private static final int CHARS_PER_TOKEN = 4;

    public List<Chunk> chunkText(String text) {
        return chunkText(text, DEFAULT_CHUNK_SIZE_TOKENS, DEFAULT_CHUNK_OVERLAP_TOKENS);
    }

    public List<Chunk> chunkText(String text, int maxChunkTokens, int overlapTokens) {
        List<Chunk> chunks = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return chunks;
        }

        String cleanText = text.trim().replaceAll("\\r\\n", "\n").replaceAll("\\r", "\n");
        int maxChunkChars = maxChunkTokens * CHARS_PER_TOKEN;
        int overlapChars = overlapTokens * CHARS_PER_TOKEN;

        if (cleanText.length() <= maxChunkChars) {
            int tokens = estimateTokens(cleanText);
            chunks.add(new Chunk(0, cleanText, tokens));
            return chunks;
        }

        // Split by paragraphs first
        String[] paragraphs = cleanText.split("\n\n+");
        StringBuilder currentChunk = new StringBuilder();
        int chunkIndex = 0;

        for (String para : paragraphs) {
            String trimmedPara = para.trim();
            if (trimmedPara.isEmpty()) continue;

            if (currentChunk.length() + trimmedPara.length() + 2 <= maxChunkChars) {
                if (currentChunk.length() > 0) currentChunk.append("\n\n");
                currentChunk.append(trimmedPara);
            } else {
                if (currentChunk.length() > 0) {
                    String chunkContent = currentChunk.toString();
                    chunks.add(new Chunk(chunkIndex++, chunkContent, estimateTokens(chunkContent)));

                    // Keep tail for overlap
                    int keepStart = Math.max(0, currentChunk.length() - overlapChars);
                    String overlap = currentChunk.substring(keepStart);
                    currentChunk = new StringBuilder(overlap);
                    if (currentChunk.length() > 0) currentChunk.append("\n\n");
                }

                // If a single paragraph is longer than maxChunkChars, split by sentences
                if (trimmedPara.length() > maxChunkChars) {
                    List<String> sentences = splitIntoSentences(trimmedPara);
                    for (String sentence : sentences) {
                        if (currentChunk.length() + sentence.length() + 1 <= maxChunkChars) {
                            if (currentChunk.length() > 0) currentChunk.append(" ");
                            currentChunk.append(sentence);
                        } else {
                            if (currentChunk.length() > 0) {
                                String chunkContent = currentChunk.toString();
                                chunks.add(new Chunk(chunkIndex++, chunkContent, estimateTokens(chunkContent)));
                                int keepStart = Math.max(0, currentChunk.length() - overlapChars);
                                currentChunk = new StringBuilder(currentChunk.substring(keepStart));
                                if (currentChunk.length() > 0) currentChunk.append(" ");
                            }
                            currentChunk.append(sentence);
                        }
                    }
                } else {
                    currentChunk.append(trimmedPara);
                }
            }
        }

        if (currentChunk.length() > 0) {
            String chunkContent = currentChunk.toString();
            chunks.add(new Chunk(chunkIndex, chunkContent, estimateTokens(chunkContent)));
        }

        return chunks;
    }

    public int estimateTokens(String text) {
        if (text == null || text.isBlank()) return 0;
        return (int) Math.ceil((double) text.length() / CHARS_PER_TOKEN);
    }

    private List<String> splitIntoSentences(String text) {
        List<String> sentences = new ArrayList<>();
        String[] parts = text.split("(?<=[.!?])\\s+");
        for (String p : parts) {
            if (!p.isBlank()) {
                sentences.add(p.trim());
            }
        }
        return sentences;
    }
}
