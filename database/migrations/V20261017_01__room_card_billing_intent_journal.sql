-- Forward intent metadata only; no active task, business success, price, ledger, or refund state.
-- Deploy only under a separately authorized exact-schema migration window.
CREATE TABLE aoo_room_billing_intent_journal (
 intent_sequence BIGINT UNSIGNED NOT NULL AUTO_INCREMENT UNIQUE,
 operation_key CHAR(64) NOT NULL,
 command_hash CHAR(64) NOT NULL,
 room_id BIGINT UNSIGNED NOT NULL,
 room_attempt VARCHAR(48) NOT NULL,
 billing_unit VARCHAR(48) NOT NULL,
 actor_id BIGINT UNSIGNED NOT NULL,
 payer_id BIGINT UNSIGNED NOT NULL,
 game_id BIGINT UNSIGNED NOT NULL,
 play_version VARCHAR(64) NOT NULL,
 region_code VARCHAR(32) NOT NULL,
 billing_kind VARCHAR(16) NOT NULL,
 billing_context VARCHAR(16) NOT NULL,
 payer_mode VARCHAR(16) NOT NULL,
 measure_kind VARCHAR(32) NOT NULL,
 measure_value INT UNSIGNED NOT NULL,
 player_count INT UNSIGNED NOT NULL,
 carry_minor BIGINT UNSIGNED NOT NULL,
 created_at DATETIME(3) NOT NULL,
 PRIMARY KEY(operation_key),
 CHECK(intent_sequence>0 AND room_id>0 AND actor_id>0 AND payer_id>0 AND game_id>0 AND measure_value>0 AND player_count>0),
 CHECK(billing_kind IN('FIXED','LOOP','CARRY')),
 CHECK(billing_context IN('HALL','CLUB','UNION')),
 CHECK(payer_mode IN('OWNER','AA','CLUB','UNION'))
) ENGINE=InnoDB;
