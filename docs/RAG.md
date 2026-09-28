# ShopAI Vector RAG Architecture

> Implementation: Phase 6

## Purpose

Vector RAG provides retrieval of unstructured knowledge:
- FAQ documents
- Shipping policies
- Return and refund policies
- Warranty information
- Size guides
- Product manuals
- Store documents
- Blog articles
- Merchant-provided documents

## Pipeline

```
Source (TXT, Markdown, PDF, HTML, URL, Manual)
    |
  Parser
    |
  Cleaner
    |
  Chunker (configurable chunk size + overlap)
    |
  Metadata extraction (tenant, shop, source, language)
    |
  Embedding (via LLMProvider.embed())
    |
  pgvector storage
    |
  VectorRetriever
    |
  ContextEngine
```

## Storage Tables

- `knowledge_sources` — source registration (URL, file, manual)
- `knowledge_documents` — parsed documents
- `knowledge_chunks` — chunked text with metadata
- `knowledge_embeddings` — vector embeddings (pgvector, HNSW indexed)

## Vector Index

HNSW index on `knowledge_embeddings.embedding` column:
- m=16, ef_construction=64 (configurable)
- Cosine distance operator (`<=>`) for semantic similarity
- ef_search=40 per query (configurable)

## Retrieval

`VectorRetriever` performs:
1. Embed the query using LLMProvider.
2. Run HNSW ANN search in pgvector.
3. Re-rank by cosine similarity threshold.
4. Return top-K chunks with metadata.

## Rules

- Vector RAG is for KNOWLEDGE only.
- Do NOT use vector RAG for live inventory, live tracking, or live order status.
- Live data always comes from Shopify Tools.
