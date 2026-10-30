-- Canonical release runtime metadata contract.
--
-- This migration is additive and intentionally leaves historical releases
-- NULL. No historical row is backfilled and no default, gameId, stateVersion,
-- lookup hash, bundle value, or supported-version constant is introduced.
-- Current release writers must provide all five fields explicitly; readers
-- reject an incomplete or conflicting release before publication. The checks
-- below validate each nullable historical sidecar independently so this
-- additive migration does not manufacture/backfill values or invalidate a
-- prior release that only carried snapshot_schema_version.
--
-- Rollback record (manual and not executed here):
--   ALTER TABLE aoo_game_release
--       DROP CONSTRAINT chk_game_release_runtime_metadata,
--       DROP COLUMN source_hash,
--       DROP COLUMN protocol_version,
--       DROP COLUMN event_interpreter_version,
--       DROP COLUMN card_codec_version;

ALTER TABLE aoo_game_release
    ADD COLUMN card_codec_version VARCHAR(64) NULL AFTER snapshot_schema_version,
    ADD COLUMN event_interpreter_version VARCHAR(64) NULL AFTER card_codec_version,
    ADD COLUMN protocol_version VARCHAR(64) NULL AFTER event_interpreter_version,
    ADD COLUMN source_hash CHAR(64) NULL AFTER protocol_version,
    ADD CONSTRAINT chk_game_release_runtime_metadata CHECK (
        (snapshot_schema_version IS NULL
            OR snapshot_schema_version REGEXP '^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$')
        AND (card_codec_version IS NULL
            OR card_codec_version REGEXP '^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$')
        AND (event_interpreter_version IS NULL
            OR event_interpreter_version REGEXP '^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$')
        AND (protocol_version IS NULL
            OR protocol_version REGEXP '^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$')
        AND (source_hash IS NULL OR source_hash REGEXP '^[0-9a-f]{64}$')
    );
