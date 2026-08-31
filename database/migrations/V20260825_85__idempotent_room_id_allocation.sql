CREATE TABLE IF NOT EXISTS aoo_room_idempotent_allocation (
    request_id VARCHAR(128) NOT NULL,
    account_id BIGINT NOT NULL,
    room_id BIGINT NOT NULL,
    created_at TIMESTAMP(3) NOT NULL,
    PRIMARY KEY (request_id),
    UNIQUE KEY uk_room_idempotent_allocation_room (room_id),
    KEY idx_room_idempotent_allocation_account (account_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
