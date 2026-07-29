CREATE TABLE short_urls (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    short_code   VARCHAR(20)   NOT NULL,
    original_url VARCHAR(2048) NOT NULL,
    created_at   TIMESTAMP     NOT NULL,
    expires_at   TIMESTAMP,
    click_count  BIGINT        NOT NULL DEFAULT 0,
    active       BOOLEAN       NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_short_urls_short_code UNIQUE (short_code)
);

CREATE INDEX idx_short_urls_short_code ON short_urls (short_code);
CREATE INDEX idx_short_urls_created_at ON short_urls (created_at);
