-- Canonical room-lock runtime metadata contract.
--
-- This migration is additive. Existing room locks are intentionally allowed to
-- remain incomplete so a reader can reject them without inventing metadata.
-- No value is backfilled from a release, bundle_hash, ui_hash, lookup hash,
-- default version, or snapshot constant. New write paths must provide both
-- fields explicitly; the ConfigCenter reader remains fail-closed for NULL.
--
-- Rollback record (manual and not executed here):
--   ALTER TABLE aoo_room_rule_lock
--       DROP CONSTRAINT chk_room_rule_lock_runtime_metadata,
--       DROP COLUMN source_hash,
--       DROP COLUMN snapshot_schema_version;
-- The forward migration remains in Flyway history. Any rollback is an
-- operational decision and must not be applied to a live room-lock table.

ALTER TABLE aoo_room_rule_lock
    ADD COLUMN source_hash CHAR(64) NULL AFTER protocol_version,
    ADD COLUMN snapshot_schema_version VARCHAR(64) NULL AFTER source_hash,
    ADD CONSTRAINT chk_room_rule_lock_runtime_metadata CHECK (
        (source_hash IS NULL AND snapshot_schema_version IS NULL)
        OR (
            source_hash REGEXP '^[0-9a-f]{64}$'
            AND snapshot_schema_version REGEXP '^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$'
        )
    );
