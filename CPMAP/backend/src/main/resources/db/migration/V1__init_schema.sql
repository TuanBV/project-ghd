-- Core schema for MC Price Optimizer
-- Timezone nghiep vu: Asia/Ho_Chi_Minh. Currency mac dinh: VND. Tien luu bang NUMERIC, khong dung float.

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255),
    role VARCHAR(20) NOT NULL CHECK (role IN ('ADMIN', 'ANALYST', 'OPERATOR')),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    mc_offer_id VARCHAR(255) UNIQUE,
    external_id VARCHAR(255),
    item_group_id VARCHAR(255),
    item_group_id_normalized VARCHAR(255),
    sku_original VARCHAR(255),
    sku_normalized VARCHAR(255),
    title VARCHAR(1000) NOT NULL,
    description TEXT,
    product_url VARCHAR(1000),
    image_url VARCHAR(1000),
    brand VARCHAR(255),
    google_category VARCHAR(500),
    product_type VARCHAR(500),
    condition VARCHAR(30) NOT NULL DEFAULT 'UNKNOWN',
    raw_condition VARCHAR(50),
    availability VARCHAR(30) NOT NULL DEFAULT 'UNKNOWN',
    raw_availability VARCHAR(50),
    current_website_price NUMERIC(18,2),
    current_mc_price NUMERIC(18,2),
    currency VARCHAR(3) NOT NULL DEFAULT 'VND',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    import_source VARCHAR(50),
    source_updated_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX ix_products_sku_normalized ON products (sku_normalized);
CREATE INDEX ix_products_product_url ON products (product_url);
CREATE INDEX ix_products_item_group_id_normalized ON products (item_group_id_normalized);
CREATE INDEX ix_products_availability ON products (availability);

CREATE TABLE product_aliases (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    alias_type VARCHAR(20) NOT NULL CHECK (alias_type IN ('SKU', 'MODEL', 'TITLE_TOKEN', 'LEGACY_SKU', 'URL_SLUG')),
    alias_original VARCHAR(500) NOT NULL,
    alias_normalized VARCHAR(500) NOT NULL,
    confirmed BOOLEAN NOT NULL DEFAULT FALSE,
    confidence NUMERIC(5,2) NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ux_product_alias UNIQUE (product_id, alias_type, alias_normalized)
);

CREATE INDEX ix_product_aliases_alias_normalized ON product_aliases (alias_normalized);
CREATE INDEX ix_product_aliases_product_id ON product_aliases (product_id);

CREATE TABLE competitors (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    base_url VARCHAR(500) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    crawl_mode VARCHAR(20) NOT NULL CHECK (crawl_mode IN ('STATIC_HTML', 'BROWSER', 'MANUAL_ONLY')),
    requests_per_minute INT NOT NULL DEFAULT 10,
    timeout_seconds INT NOT NULL DEFAULT 10,
    extractor_config JSONB NOT NULL DEFAULT '{}'::jsonb,
    last_success_at TIMESTAMPTZ,
    last_error_at TIMESTAMPTZ,
    last_error_message VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE competitor_listings (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    competitor_id BIGINT NOT NULL REFERENCES competitors(id) ON DELETE CASCADE,
    url VARCHAR(1000) NOT NULL,
    external_sku VARCHAR(255),
    match_method VARCHAR(30) NOT NULL,
    match_score NUMERIC(5,2) NOT NULL DEFAULT 0,
    match_reason VARCHAR(1000),
    match_status VARCHAR(20) NOT NULL CHECK (match_status IN ('AUTO_CONFIRMED', 'MANUALLY_CONFIRMED', 'REVIEW_REQUIRED', 'REJECTED')),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ux_competitor_listing_url UNIQUE (competitor_id, url)
);

CREATE INDEX ix_competitor_listings_product_id ON competitor_listings (product_id);
CREATE INDEX ix_competitor_listings_match_status ON competitor_listings (match_status);

CREATE TABLE price_observations (
    id BIGSERIAL PRIMARY KEY,
    competitor_listing_id BIGINT NOT NULL REFERENCES competitor_listings(id) ON DELETE CASCADE,
    price NUMERIC(18,2),
    currency VARCHAR(3) NOT NULL DEFAULT 'VND',
    availability VARCHAR(30),
    raw_price_text VARCHAR(500),
    source_type VARCHAR(20) NOT NULL CHECK (source_type IN ('CRAWL', 'MANUAL', 'IMPORT')),
    observation_status VARCHAR(20) NOT NULL CHECK (observation_status IN ('VALID', 'NO_PRICE', 'CONTACT_ONLY', 'OUT_OF_STOCK', 'PARSE_ERROR', 'STALE')),
    http_status INT,
    final_url VARCHAR(1000),
    captured_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    verified_by VARCHAR(255),
    note VARCHAR(1000),
    excluded BOOLEAN NOT NULL DEFAULT FALSE,
    exclusion_reason VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_price_observations_listing_captured ON price_observations (competitor_listing_id, captured_at DESC);
CREATE INDEX ix_price_observations_status ON price_observations (observation_status);
CREATE INDEX ix_price_observations_captured_at ON price_observations (captured_at);

CREATE TABLE price_policies (
    id BIGSERIAL PRIMARY KEY,
    scope VARCHAR(20) NOT NULL CHECK (scope IN ('GLOBAL', 'CATEGORY', 'PRODUCT')),
    category VARCHAR(500),
    product_id BIGINT REFERENCES products(id) ON DELETE CASCADE,
    minimum_competitor_count INT NOT NULL DEFAULT 2,
    max_observation_age_hours INT NOT NULL DEFAULT 48,
    rounding_step NUMERIC(18,2) NOT NULL DEFAULT 10000,
    max_increase_percent NUMERIC(5,2) NOT NULL DEFAULT 15,
    max_decrease_percent NUMERIC(5,2) NOT NULL DEFAULT 15,
    outlier_threshold_percent NUMERIC(5,2) NOT NULL DEFAULT 30,
    outlier_strategy VARCHAR(20) NOT NULL DEFAULT 'FLAG_ONLY',
    require_manual_approval BOOLEAN NOT NULL DEFAULT TRUE,
    minimum_allowed_price NUMERIC(18,2),
    maximum_allowed_price NUMERIC(18,2),
    auto_publish_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX ux_price_policies_global ON price_policies (scope) WHERE scope = 'GLOBAL';
CREATE UNIQUE INDEX ux_price_policies_category ON price_policies (category) WHERE scope = 'CATEGORY';
CREATE UNIQUE INDEX ux_price_policies_product ON price_policies (product_id) WHERE scope = 'PRODUCT';

CREATE TABLE price_recommendations (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    current_price NUMERIC(18,2),
    raw_average_price NUMERIC(18,2),
    rounded_price NUMERIC(18,2),
    final_suggested_price NUMERIC(18,2),
    included_source_count INT NOT NULL DEFAULT 0,
    excluded_source_count INT NOT NULL DEFAULT 0,
    calculation_snapshot JSONB NOT NULL DEFAULT '{}'::jsonb,
    status VARCHAR(20) NOT NULL CHECK (status IN ('INSUFFICIENT_DATA', 'REVIEW_REQUIRED', 'READY', 'APPROVED', 'REJECTED', 'PUBLISHED', 'FAILED')),
    override_price NUMERIC(18,2),
    override_by VARCHAR(255),
    override_reason VARCHAR(1000),
    override_expires_at TIMESTAMPTZ,
    approved_by VARCHAR(255),
    approved_at TIMESTAMPTZ,
    rejected_by VARCHAR(255),
    rejected_at TIMESTAMPTZ,
    rejection_reason VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX ix_price_recommendations_product_id ON price_recommendations (product_id, created_at DESC);
CREATE INDEX ix_price_recommendations_status ON price_recommendations (status);

CREATE TABLE price_recommendation_sources (
    id BIGSERIAL PRIMARY KEY,
    recommendation_id BIGINT NOT NULL REFERENCES price_recommendations(id) ON DELETE CASCADE,
    price_observation_id BIGINT NOT NULL REFERENCES price_observations(id) ON DELETE CASCADE,
    included BOOLEAN NOT NULL DEFAULT TRUE,
    exclusion_reason VARCHAR(500),
    CONSTRAINT ux_recommendation_source UNIQUE (recommendation_id, price_observation_id)
);

CREATE INDEX ix_recommendation_sources_recommendation_id ON price_recommendation_sources (recommendation_id);

CREATE TABLE import_runs (
    id BIGSERIAL PRIMARY KEY,
    import_type VARCHAR(20) NOT NULL CHECK (import_type IN ('MC', 'COMPARISON')),
    file_name VARCHAR(500),
    file_hash VARCHAR(128) NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('QUEUED', 'RUNNING', 'SUCCESS', 'PARTIAL_SUCCESS', 'FAILED', 'CANCELLED')),
    total_rows INT NOT NULL DEFAULT 0,
    success_rows INT NOT NULL DEFAULT 0,
    issue_rows INT NOT NULL DEFAULT 0,
    started_at TIMESTAMPTZ,
    finished_at TIMESTAMPTZ,
    triggered_by VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ux_import_runs_hash UNIQUE (import_type, file_hash)
);

CREATE TABLE import_rows (
    id BIGSERIAL PRIMARY KEY,
    import_run_id BIGINT NOT NULL REFERENCES import_runs(id) ON DELETE CASCADE,
    row_number INT NOT NULL,
    row_identity VARCHAR(128) NOT NULL,
    raw_row_json JSONB NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('IMPORTED', 'SKIPPED', 'ISSUE')),
    product_id BIGINT REFERENCES products(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ux_import_rows_run_row UNIQUE (import_run_id, row_number)
);

CREATE INDEX ix_import_rows_run_id ON import_rows (import_run_id);

CREATE TABLE import_issues (
    id BIGSERIAL PRIMARY KEY,
    import_run_id BIGINT NOT NULL REFERENCES import_runs(id) ON DELETE CASCADE,
    import_row_id BIGINT REFERENCES import_rows(id) ON DELETE CASCADE,
    issue_type VARCHAR(50) NOT NULL,
    severity VARCHAR(10) NOT NULL CHECK (severity IN ('INFO', 'WARNING', 'ERROR')),
    message VARCHAR(1000) NOT NULL,
    details JSONB NOT NULL DEFAULT '{}'::jsonb,
    resolved BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_import_issues_run_id ON import_issues (import_run_id);
CREATE INDEX ix_import_issues_type ON import_issues (issue_type);

CREATE TABLE job_runs (
    id BIGSERIAL PRIMARY KEY,
    job_key VARCHAR(100) NOT NULL,
    trigger_type VARCHAR(20) NOT NULL CHECK (trigger_type IN ('MANUAL', 'SCHEDULED')),
    status VARCHAR(20) NOT NULL CHECK (status IN ('QUEUED', 'RUNNING', 'SUCCESS', 'PARTIAL_SUCCESS', 'FAILED', 'CANCELLED')),
    total_items INT NOT NULL DEFAULT 0,
    success_items INT NOT NULL DEFAULT 0,
    failed_items INT NOT NULL DEFAULT 0,
    progress_percent INT NOT NULL DEFAULT 0,
    started_at TIMESTAMPTZ,
    finished_at TIMESTAMPTZ,
    error_detail TEXT,
    correlation_id VARCHAR(100),
    triggered_by VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_job_runs_job_key ON job_runs (job_key, created_at DESC);
CREATE UNIQUE INDEX ux_job_runs_running ON job_runs (job_key) WHERE status IN ('QUEUED', 'RUNNING');

CREATE TABLE crawl_runs (
    id BIGSERIAL PRIMARY KEY,
    job_run_id BIGINT REFERENCES job_runs(id) ON DELETE SET NULL,
    trigger_type VARCHAR(20) NOT NULL CHECK (trigger_type IN ('MANUAL', 'SCHEDULED')),
    status VARCHAR(20) NOT NULL CHECK (status IN ('QUEUED', 'RUNNING', 'SUCCESS', 'PARTIAL_SUCCESS', 'FAILED', 'CANCELLED')),
    total_items INT NOT NULL DEFAULT 0,
    success_items INT NOT NULL DEFAULT 0,
    failed_items INT NOT NULL DEFAULT 0,
    started_at TIMESTAMPTZ,
    finished_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE crawl_items (
    id BIGSERIAL PRIMARY KEY,
    crawl_run_id BIGINT NOT NULL REFERENCES crawl_runs(id) ON DELETE CASCADE,
    competitor_listing_id BIGINT NOT NULL REFERENCES competitor_listings(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL CHECK (status IN ('SUCCESS', 'FAILED', 'SKIPPED')),
    price_observation_id BIGINT REFERENCES price_observations(id) ON DELETE SET NULL,
    error_message VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_crawl_items_run_id ON crawl_items (crawl_run_id);

CREATE TABLE website_publish_runs (
    id BIGSERIAL PRIMARY KEY,
    job_run_id BIGINT REFERENCES job_runs(id) ON DELETE SET NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('QUEUED', 'RUNNING', 'SUCCESS', 'PARTIAL_SUCCESS', 'FAILED', 'CANCELLED')),
    total_items INT NOT NULL DEFAULT 0,
    success_items INT NOT NULL DEFAULT 0,
    failed_items INT NOT NULL DEFAULT 0,
    dry_run BOOLEAN NOT NULL DEFAULT TRUE,
    started_at TIMESTAMPTZ,
    finished_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE website_publish_items (
    id BIGSERIAL PRIMARY KEY,
    website_publish_run_id BIGINT NOT NULL REFERENCES website_publish_runs(id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    recommendation_id BIGINT REFERENCES price_recommendations(id) ON DELETE SET NULL,
    previous_price NUMERIC(18,2),
    new_price NUMERIC(18,2),
    status VARCHAR(20) NOT NULL CHECK (status IN ('SUCCESS', 'FAILED', 'SKIPPED')),
    verification_status VARCHAR(20) NOT NULL DEFAULT 'NOT_CHECKED' CHECK (verification_status IN ('VERIFIED', 'MISMATCH', 'NOT_CHECKED')),
    verified_price NUMERIC(18,2),
    error_message VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_website_publish_items_run_id ON website_publish_items (website_publish_run_id);
CREATE INDEX ix_website_publish_items_product_id ON website_publish_items (product_id);

CREATE TABLE merchant_sync_runs (
    id BIGSERIAL PRIMARY KEY,
    job_run_id BIGINT REFERENCES job_runs(id) ON DELETE SET NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('QUEUED', 'RUNNING', 'SUCCESS', 'PARTIAL_SUCCESS', 'FAILED', 'CANCELLED')),
    total_items INT NOT NULL DEFAULT 0,
    success_items INT NOT NULL DEFAULT 0,
    failed_items INT NOT NULL DEFAULT 0,
    dry_run BOOLEAN NOT NULL DEFAULT TRUE,
    started_at TIMESTAMPTZ,
    finished_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE merchant_sync_items (
    id BIGSERIAL PRIMARY KEY,
    merchant_sync_run_id BIGINT NOT NULL REFERENCES merchant_sync_runs(id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    recommendation_id BIGINT REFERENCES price_recommendations(id) ON DELETE SET NULL,
    previous_price NUMERIC(18,2),
    new_price NUMERIC(18,2),
    status VARCHAR(20) NOT NULL CHECK (status IN ('SUCCESS', 'FAILED', 'SKIPPED')),
    merchant_status VARCHAR(100),
    error_message VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_merchant_sync_items_run_id ON merchant_sync_items (merchant_sync_run_id);
CREATE INDEX ix_merchant_sync_items_product_id ON merchant_sync_items (product_id);

CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    actor VARCHAR(255) NOT NULL,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id VARCHAR(100),
    details JSONB NOT NULL DEFAULT '{}'::jsonb,
    correlation_id VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_audit_logs_entity ON audit_logs (entity_type, entity_id);
CREATE INDEX ix_audit_logs_created_at ON audit_logs (created_at);
