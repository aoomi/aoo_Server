-- Region is gameplay provenance only; it must never be interpreted as room routing or asset isolation.
ALTER TABLE aoo_hall_room
    RENAME COLUMN region_code TO classification_region_code;
