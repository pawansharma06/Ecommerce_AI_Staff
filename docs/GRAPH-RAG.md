# ShopAI Graph RAG Architecture

> Implementation: Phase 5

## Purpose

Graph RAG provides relationship-based retrieval over commerce entities:
- Customer purchase history
- Order relationships
- Product relationships
- Frequently bought products
- Related products
- Customer behavior
- Product affinity
- Cross-entity queries

## Commerce Knowledge Graph

### Entities
- Store
- Customer
- Order
- OrderItem
- Product
- ProductVariant
- Collection
- Vendor
- Fulfillment
- Shipment

### Relationships
```
Customer --> PLACED --> Order
Order --> CONTAINS --> OrderItem
OrderItem --> REFERENCES --> Product
OrderItem --> REFERENCES_VARIANT --> ProductVariant
Product --> BELONGS_TO --> Collection
Product --> CREATED_BY --> Vendor
Order --> FULFILLED_BY --> Fulfillment
Fulfillment --> TRACKED_BY --> Shipment
Customer --> PURCHASED --> Product
Product --> FREQUENTLY_BOUGHT_WITH --> Product
Product --> RELATED_TO --> Product
Product --> ALTERNATIVE_TO --> Product
Product --> UPSELL_TO --> Product
```

## Storage

### Phase 5 (V1) — PostgreSQL

Table: `commerce_relationships`

| Column | Type | Description |
|---|---|---|
| id | UUID | PK |
| tenant_id | UUID | Tenant isolation |
| source_type | VARCHAR | Entity type of source |
| source_id | VARCHAR | Entity ID of source |
| relationship_type | VARCHAR | Relationship name |
| target_type | VARCHAR | Entity type of target |
| target_id | VARCHAR | Entity ID of target |
| weight | DECIMAL | Relationship strength |
| confidence | DECIMAL | Confidence score |
| metadata | JSONB | Additional context |
| created_at | TIMESTAMPTZ | Creation time |
| updated_at | TIMESTAMPTZ | Last update |

Indexes:
- `tenant_id`
- `source_type + source_id`
- `relationship_type`
- `target_type + target_id`
- `tenant_id + relationship_type + source_type + source_id` (composite)

### Future — Neo4j

The `GraphRepository` interface allows switching to Neo4j without changing the Agent Engine.

## GraphRetriever Methods

- `getCustomerHistory(tenantId, customerId)`
- `getCustomerOrders(tenantId, customerId)`
- `getCustomerProducts(tenantId, customerId)`
- `getCustomerLastOrder(tenantId, customerId)`
- `findRelatedProducts(tenantId, productId)`
- `findFrequentlyBoughtProducts(tenantId, productId)`
- `findCustomersWhoBought(tenantId, productId)`
- `findProductsBoughtTogether(tenantId, productId1, productId2)`
- `findProductsNotPurchased(tenantId, customerId)`
- `findCustomerSegments(tenantId)`
- `getOrderRelationships(tenantId, orderId)`

## Rules

- Graph RAG is for RELATIONSHIPS only.
- Do NOT use Graph RAG for live inventory, tracking, or pricing.
- No arbitrary LLM-generated SQL — only controlled query methods.
- All queries must include tenant_id.