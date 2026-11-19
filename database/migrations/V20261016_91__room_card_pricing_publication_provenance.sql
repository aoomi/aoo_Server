-- Forward metadata only. Apply only after exact target schema/approval and publication FK guard.
-- No historical data changes, prices, ledger entries, or active pointer seed.
CREATE TABLE aoo_room_cost_policy_provenance (
 publication_id BIGINT UNSIGNED NOT NULL,
 policy_key CHAR(64) NOT NULL,
 configuration_version VARCHAR(128) NOT NULL,
 workbook_manifest_hash CHAR(64) NOT NULL,
 authority_hash CHAR(64) NOT NULL,
 source_class VARCHAR(32) NOT NULL,
 release_id BIGINT UNSIGNED NOT NULL,
 index_generation BIGINT UNSIGNED NOT NULL,
 source_hash VARCHAR(256) NOT NULL,
 free_policy_ref VARCHAR(128) NULL,
 free_valid_from DATETIME(3) NULL,
 free_valid_until DATETIME(3) NULL,
 always_free BOOLEAN NOT NULL,
 PRIMARY KEY(publication_id,policy_key),
 FOREIGN KEY(publication_id) REFERENCES aoo_room_cost_publication(publication_id),
 CHECK(publication_id>0 AND release_id>0 AND index_generation>0),
 CHECK(source_class='CURRENT_PUBLISHED'),
 CHECK((free_policy_ref IS NULL AND free_valid_from IS NULL AND free_valid_until IS NULL AND always_free=FALSE)
    OR (free_policy_ref IS NOT NULL AND always_free=TRUE AND free_valid_from IS NULL AND free_valid_until IS NULL)
    OR (free_policy_ref IS NOT NULL AND always_free=FALSE AND free_valid_from IS NOT NULL AND free_valid_until IS NOT NULL AND free_valid_until>=free_valid_from))
) ENGINE=InnoDB;
