-- Canonical release snapshot schema contract.
--
-- This additive migration deliberately leaves historical rows NULL.  No
-- historical release is backfilled and no default/constant is introduced.
-- New release writers must provide the field explicitly; ConfigCenter and the
-- room-rules publisher reject a release without it before publication.
--
-- Rollback record (manual and not executed here):
--   ALTER TABLE aoo_game_release
--       DROP CONSTRAINT chk_game_release_snapshot_schema_version,
--       DROP COLUMN snapshot_schema_version;

ALTER TABLE aoo_game_release
    ADD COLUMN snapshot_schema_version VARCHAR(64) NULL AFTER play_version,
    ADD CONSTRAINT chk_game_release_snapshot_schema_version CHECK (
        snapshot_schema_version IS NULL
        OR snapshot_schema_version REGEXP '^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$'
    );
