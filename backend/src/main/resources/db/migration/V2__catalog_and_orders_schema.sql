-- =============================================================================
-- ShopAI Standalone Platform
-- Migration: V2__catalog_and_orders_schema.sql
-- Description: Product Catalog, Orders, Multi-Fulfillments, Transactions, and Abandoned Checkouts
-- =============================================================================

-- =============================================================================
-- 1. PRODUCTS & CATALOG
-- =============================================================================
CREATE TABLE products (
    id                  UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    shopify_product_id  BIGINT          NOT NULL UNIQUE,
    title               VARCHAR(500)    NOT NULL,
    handle              VARCHAR(500)    NOT NULL,
    description         TEXT,
    vendor              VARCHAR(255),
    product_type        VARCHAR(255),
    tags                TEXT[]          DEFAULT '{}',
    status              VARCHAR(50)     NOT NULL DEFAULT 'ACTIVE',
    total_inventory     INT             NOT NULL DEFAULT 0,
    published_at        TIMESTAMPTZ,
    shopify_created_at  TIMESTAMPTZ,
    shopify_updated_at  TIMESTAMPTZ,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    synced_at           TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_products_shopify_id ON products (shopify_product_id);
CREATE INDEX idx_products_handle ON products (handle);
CREATE INDEX idx_products_vendor ON products (vendor);
CREATE INDEX idx_products_product_type ON products (product_type);
CREATE INDEX idx_products_status ON products (status);
CREATE INDEX idx_products_tags ON products USING gin (tags);

-- Product Variants
CREATE TABLE product_variants (
    id                  UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id          UUID            NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    shopify_variant_id  BIGINT          NOT NULL UNIQUE,
    title               VARCHAR(255)    NOT NULL,
    sku                 VARCHAR(100),
    barcode             VARCHAR(100),
    price               NUMERIC(12, 2)  NOT NULL DEFAULT 0.00,
    compare_at_price    NUMERIC(12, 2),
    inventory_quantity  INT             NOT NULL DEFAULT 0,
    position            INT             NOT NULL DEFAULT 1,
    image_url           TEXT,
    requires_shipping   BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_variants_product_id ON product_variants (product_id);
CREATE INDEX idx_variants_shopify_id ON product_variants (shopify_variant_id);
CREATE INDEX idx_variants_sku ON product_variants (sku);

-- Collections
CREATE TABLE collections (
    id                    UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    shopify_collection_id BIGINT          NOT NULL UNIQUE,
    title                 VARCHAR(255)    NOT NULL,
    handle                VARCHAR(255)    NOT NULL,
    description           TEXT,
    collection_type       VARCHAR(50)     NOT NULL DEFAULT 'CUSTOM',
    synced_at             TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_collections_shopify_id ON collections (shopify_collection_id);
CREATE INDEX idx_collections_handle ON collections (handle);

-- Product Collections (Many-to-Many)
CREATE TABLE product_collections (
    product_id    UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    collection_id UUID NOT NULL REFERENCES collections(id) ON DELETE CASCADE,
    PRIMARY KEY (product_id, collection_id)
);

CREATE INDEX idx_product_collections_collection_id ON product_collections (collection_id);

-- =============================================================================
-- 2. ORDERS & ORDER LINE ITEMS
-- =============================================================================
CREATE TABLE orders (
    id                  UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    shopify_order_id    BIGINT          NOT NULL UNIQUE,
    order_number        VARCHAR(100)    NOT NULL,
    name                VARCHAR(100)    NOT NULL,
    email               VARCHAR(255),
    phone               VARCHAR(50),
    financial_status    VARCHAR(50)     NOT NULL DEFAULT 'PENDING',
    fulfillment_status  VARCHAR(50)     NOT NULL DEFAULT 'UNFULFILLED',
    currency            VARCHAR(10)     NOT NULL DEFAULT 'USD',
    subtotal_price      NUMERIC(12, 2)  NOT NULL DEFAULT 0.00,
    total_discounts     NUMERIC(12, 2)  NOT NULL DEFAULT 0.00,
    total_tax           NUMERIC(12, 2)  NOT NULL DEFAULT 0.00,
    total_shipping      NUMERIC(12, 2)  NOT NULL DEFAULT 0.00,
    total_price         NUMERIC(12, 2)  NOT NULL DEFAULT 0.00,
    cancelled_at        TIMESTAMPTZ,
    cancel_reason       VARCHAR(255),
    customer_id         BIGINT,
    customer_first_name VARCHAR(100),
    customer_last_name  VARCHAR(100),
    customer_email      VARCHAR(255),
    shipping_address    JSONB,
    billing_address     JSONB,
    tags                TEXT[]          DEFAULT '{}',
    note                TEXT,
    shopify_created_at  TIMESTAMPTZ,
    shopify_updated_at  TIMESTAMPTZ,
    synced_at           TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_orders_shopify_id ON orders (shopify_order_id);
CREATE INDEX idx_orders_order_number ON orders (order_number);
CREATE INDEX idx_orders_name ON orders (name);
CREATE INDEX idx_orders_financial_status ON orders (financial_status);
CREATE INDEX idx_orders_fulfillment_status ON orders (fulfillment_status);
CREATE INDEX idx_orders_customer_email ON orders (customer_email);
CREATE INDEX idx_orders_shopify_created_at ON orders (shopify_created_at DESC);

-- Order Line Items
CREATE TABLE order_line_items (
    id                    UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id              UUID            NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    shopify_line_item_id  BIGINT          NOT NULL,
    product_id            UUID            REFERENCES products(id) ON DELETE SET NULL,
    variant_id            UUID            REFERENCES product_variants(id) ON DELETE SET NULL,
    title                 VARCHAR(500)    NOT NULL,
    variant_title         VARCHAR(255),
    sku                   VARCHAR(100),
    quantity              INT             NOT NULL DEFAULT 1,
    fulfillable_quantity  INT             NOT NULL DEFAULT 0,
    fulfilled_quantity    INT             NOT NULL DEFAULT 0,
    price                 NUMERIC(12, 2)  NOT NULL DEFAULT 0.00,
    total_discount        NUMERIC(12, 2)  NOT NULL DEFAULT 0.00,
    requires_shipping     BOOLEAN         NOT NULL DEFAULT TRUE,
    taxable               BOOLEAN         NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_order_line_items_order_id ON order_line_items (order_id);
CREATE INDEX idx_order_line_items_shopify_id ON order_line_items (shopify_line_item_id);

-- =============================================================================
-- 3. FULFILLMENTS & MULTI-FULFILLMENT TRACKING
-- =============================================================================
CREATE TABLE fulfillments (
    id                      UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id                UUID            NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    shopify_fulfillment_id  BIGINT          NOT NULL UNIQUE,
    status                  VARCHAR(50)     NOT NULL DEFAULT 'SUCCESS',
    tracking_company        VARCHAR(100),
    tracking_number         VARCHAR(100),
    tracking_numbers        TEXT[]          DEFAULT '{}',
    tracking_url            TEXT,
    tracking_urls           TEXT[]          DEFAULT '{}',
    service                 VARCHAR(100),
    shipment_status         VARCHAR(50),
    estimated_delivery_at   TIMESTAMPTZ,
    delivered_at            TIMESTAMPTZ,
    shopify_created_at      TIMESTAMPTZ,
    shopify_updated_at      TIMESTAMPTZ,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_fulfillments_order_id ON fulfillments (order_id);
CREATE INDEX idx_fulfillments_shopify_id ON fulfillments (shopify_fulfillment_id);
CREATE INDEX idx_fulfillments_tracking_number ON fulfillments (tracking_number);

-- Fulfillment Line Items (Tracking which items were shipped in each fulfillment package)
CREATE TABLE fulfillment_line_items (
    id                  UUID    PRIMARY KEY DEFAULT gen_random_uuid(),
    fulfillment_id      UUID    NOT NULL REFERENCES fulfillments(id) ON DELETE CASCADE,
    order_line_item_id  UUID    NOT NULL REFERENCES order_line_items(id) ON DELETE CASCADE,
    quantity            INT     NOT NULL DEFAULT 1
);

CREATE INDEX idx_fli_fulfillment_id ON fulfillment_line_items (fulfillment_id);
CREATE INDEX idx_fli_order_line_item_id ON fulfillment_line_items (order_line_item_id);

-- =============================================================================
-- 4. ORDER TRANSACTIONS & FINANCIAL LEDGER
-- =============================================================================
CREATE TABLE order_transactions (
    id                      UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id                UUID            NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    shopify_transaction_id  BIGINT          NOT NULL UNIQUE,
    parent_id               BIGINT,
    gateway                 VARCHAR(100),
    kind                    VARCHAR(50)     NOT NULL,
    status                  VARCHAR(50)     NOT NULL DEFAULT 'SUCCESS',
    amount                  NUMERIC(12, 2)  NOT NULL DEFAULT 0.00,
    currency                VARCHAR(10)     NOT NULL DEFAULT 'USD',
    payment_method_name     VARCHAR(100),
    error_code              VARCHAR(100),
    error_message           TEXT,
    receipt                 JSONB,
    processed_at            TIMESTAMPTZ,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_transactions_order_id ON order_transactions (order_id);
CREATE INDEX idx_transactions_shopify_id ON order_transactions (shopify_transaction_id);
CREATE INDEX idx_transactions_kind ON order_transactions (kind);
CREATE INDEX idx_transactions_status ON order_transactions (status);

-- =============================================================================
-- 5. ABANDONED CHECKOUTS
-- =============================================================================
CREATE TABLE abandoned_checkouts (
    id                      UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    shopify_checkout_id     BIGINT          NOT NULL UNIQUE,
    cart_token              VARCHAR(255),
    email                   VARCHAR(255),
    phone                   VARCHAR(50),
    customer_name           VARCHAR(255),
    subtotal_price          NUMERIC(12, 2)  NOT NULL DEFAULT 0.00,
    total_price             NUMERIC(12, 2)  NOT NULL DEFAULT 0.00,
    currency                VARCHAR(10)     NOT NULL DEFAULT 'USD',
    abandoned_checkout_url  TEXT,
    recovery_status         VARCHAR(50)     NOT NULL DEFAULT 'ABANDONED',
    completed_at            TIMESTAMPTZ,
    line_items              JSONB           DEFAULT '[]',
    shopify_created_at      TIMESTAMPTZ,
    shopify_updated_at      TIMESTAMPTZ,
    synced_at               TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_abandoned_checkouts_shopify_id ON abandoned_checkouts (shopify_checkout_id);
CREATE INDEX idx_abandoned_checkouts_email ON abandoned_checkouts (email);
CREATE INDEX idx_abandoned_checkouts_status ON abandoned_checkouts (recovery_status);
CREATE INDEX idx_abandoned_checkouts_created_at ON abandoned_checkouts (shopify_created_at DESC);

-- =============================================================================
-- 6. SYNC JOBS & HISTORY
-- =============================================================================
CREATE TABLE sync_jobs (
    id                UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    job_type          VARCHAR(50)     NOT NULL,
    status            VARCHAR(50)     NOT NULL DEFAULT 'IN_PROGRESS',
    items_processed   INT             NOT NULL DEFAULT 0,
    error_message     TEXT,
    started_at        TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    completed_at      TIMESTAMPTZ
);

CREATE INDEX idx_sync_jobs_job_type ON sync_jobs (job_type);
CREATE INDEX idx_sync_jobs_status ON sync_jobs (status);
CREATE INDEX idx_sync_jobs_started_at ON sync_jobs (started_at DESC);