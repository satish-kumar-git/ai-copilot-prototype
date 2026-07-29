CREATE TABLE click_events (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    short_url_id BIGINT       NOT NULL,
    clicked_at   TIMESTAMP    NOT NULL,
    user_agent   VARCHAR(512),
    ip_address   VARCHAR(45),
    referer      VARCHAR(2048),
    CONSTRAINT fk_click_events_short_url
        FOREIGN KEY (short_url_id) REFERENCES short_urls (id) ON DELETE CASCADE
);

CREATE INDEX idx_click_events_short_url_id ON click_events (short_url_id);
CREATE INDEX idx_click_events_clicked_at   ON click_events (clicked_at);
