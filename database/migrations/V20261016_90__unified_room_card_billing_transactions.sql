-- CANDIDATE ONLY: do not execute before independent plan ACCEPT + sourceReady.
-- MySQL8. All old rows remain in publication_id=0 / LEGACY dimensions.
-- Preconditions: exact migration-derived schema below; SHOW CREATE in isolated DB
-- must match before execution. No IF EXISTS masking schema drift; DDL autocommits.
CREATE TABLE aoo_room_cost_publication (
 publication_id BIGINT UNSIGNED NOT NULL, content_hash CHAR(64) NOT NULL,
 environment VARCHAR(16) NOT NULL, validated_at DATETIME(3) NOT NULL,
 PRIMARY KEY(publication_id), UNIQUE KEY uk_room_cost_publication_hash(content_hash),
 CHECK(publication_id > 0), CHECK(environment IN ('PRODUCTION','TEST_ONLY'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE aoo_room_cost_active (
 pointer_name VARCHAR(32) NOT NULL, publication_id BIGINT UNSIGNED NOT NULL,
 version BIGINT UNSIGNED NOT NULL, activated_at DATETIME(3) NOT NULL,
 PRIMARY KEY(pointer_name),
 FOREIGN KEY(publication_id) REFERENCES aoo_room_cost_publication(publication_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
ALTER TABLE aoo_room_cost_policy
 DROP CHECK chk_room_cost_measure_kind,
 ADD COLUMN publication_id BIGINT UNSIGNED NOT NULL DEFAULT 0,
 ADD COLUMN billing_kind VARCHAR(16) NOT NULL DEFAULT 'LEGACY',
 ADD COLUMN billing_context VARCHAR(16) NOT NULL DEFAULT 'LEGACY',
 ADD COLUMN tier_minor BIGINT UNSIGNED NOT NULL DEFAULT 0,
 ADD COLUMN confirmation_status VARCHAR(16) NOT NULL DEFAULT 'UNCONFIRMED',
 ADD COLUMN free_policy_ref VARCHAR(128) NULL,
 ADD COLUMN refund_policy_ref VARCHAR(128) NULL,
 DROP PRIMARY KEY,
 ADD PRIMARY KEY(game_id,play_version,region_code,measure_kind,measure_value,round_count,player_count,payer_mode,publication_id,billing_kind,billing_context,tier_minor),
 ADD CONSTRAINT chk_room_cost_measure_kind CHECK(measure_kind IN('ROUND_COUNT','DURATION_MINUTES','CARRY_MINOR_PER_TIER')),
 ADD CONSTRAINT chk_room_cost_carry_measure CHECK(
  (measure_kind<>'CARRY_MINOR_PER_TIER' AND billing_kind<>'CARRY') OR
  (measure_kind='CARRY_MINOR_PER_TIER' AND billing_kind='CARRY' AND publication_id>0
   AND tier_minor BETWEEN 1 AND 2147483647 AND measure_value=tier_minor AND round_count=measure_value)),
 ADD CONSTRAINT chk_room_cost_billing_kind CHECK(billing_kind IN('LEGACY','FIXED','LOOP','CARRY')),
 ADD CONSTRAINT chk_room_cost_billing_context CHECK(billing_context IN('LEGACY','HALL','CLUB','UNION')),
 ADD CONSTRAINT chk_room_cost_confirmation CHECK(confirmation_status IN('UNCONFIRMED','CONFIRMED')),
 ADD CONSTRAINT chk_room_cost_new_row CHECK(publication_id=0 OR
  (billing_kind<>'LEGACY' AND billing_context<>'LEGACY' AND confirmation_status='CONFIRMED'
   AND currency_code='ROOM_CARD' AND payer_mode IN('OWNER','AA','CLUB','UNION')
   AND refund_policy_ref IS NOT NULL AND (cost_minor>0 OR free_policy_ref IS NOT NULL)
   AND (billing_kind<>'CARRY' OR (tier_minor>0 AND payer_mode<>'AA'))));
-- No INSERT/UPDATE legacy prices and no active pointer seed: runtime remains closed
-- until an explicitly validated publication (TEST_ONLY only in isolated profile).
ALTER TABLE aoo_room_billing_reservation
 ADD COLUMN billing_operation_key CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL,
 ADD COLUMN command_hash CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL,
 ADD COLUMN actor_id BIGINT UNSIGNED NULL,
 ADD COLUMN room_attempt VARCHAR(48) CHARACTER SET ascii COLLATE ascii_bin NULL,
 ADD COLUMN billing_kind VARCHAR(16) NULL,
 ADD COLUMN billing_unit VARCHAR(48) CHARACTER SET ascii COLLATE ascii_bin NULL,
 ADD COLUMN policy_publication_id BIGINT UNSIGNED NULL,
 ADD COLUMN policy_snapshot JSON NULL,
 ADD COLUMN debit_business_id VARCHAR(128) NULL,
 ADD COLUMN confirmation_id VARCHAR(128) NULL,
 ADD COLUMN confirmation_hash CHAR(64) NULL,
 ADD COLUMN refunded_minor BIGINT UNSIGNED NOT NULL DEFAULT 0,
 DROP INDEX uk_room_billing_room,
 ADD UNIQUE KEY uk_room_billing_operation(billing_operation_key),
 ADD KEY idx_room_billing_instance(room_id,room_attempt,state),
 ADD CONSTRAINT chk_room_billing_refund_cap CHECK(refunded_minor<=amount),
 ADD CONSTRAINT chk_room_billing_new_metadata CHECK(billing_operation_key IS NULL OR
  (command_hash IS NOT NULL AND actor_id IS NOT NULL AND room_attempt IS NOT NULL
   AND billing_kind IN('FIXED','LOOP','CARRY') AND billing_unit IS NOT NULL
   AND policy_publication_id IS NOT NULL AND policy_snapshot IS NOT NULL
   AND currency_code='ROOM_CARD' AND (amount=0 OR debit_business_id IS NOT NULL)));
CREATE TABLE aoo_room_billing_refund (
 refund_id CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 request_id VARCHAR(128) NOT NULL, command_hash CHAR(64) NOT NULL,
 amount BIGINT UNSIGNED NOT NULL, credit_business_id VARCHAR(128) NULL,
 evidence_json JSON NOT NULL, created_at DATETIME(3) NOT NULL,
 PRIMARY KEY(refund_id), KEY idx_room_refund_original(request_id),
 FOREIGN KEY(request_id) REFERENCES aoo_room_billing_reservation(request_id),
 CHECK(amount=0 OR credit_business_id IS NOT NULL)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
