CREATE TABLE IF NOT EXISTS aoo_consumed_event (
    consumer_name VARCHAR(128) NOT NULL,
    event_id VARCHAR(64) NOT NULL,
    processed_at DATETIME(3) NOT NULL,
    PRIMARY KEY (consumer_name, event_id),
    KEY idx_consumed_event_retention (processed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
