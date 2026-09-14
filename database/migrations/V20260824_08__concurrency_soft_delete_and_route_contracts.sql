-- Remaining relational contracts: partial uniqueness for soft deletion,
-- concurrency/update-time coverage, JSON schemas and bounded shard routing.

ALTER TABLE aoo_room_template
    DROP INDEX uk_template_active_name,
    ADD COLUMN active_display_name VARCHAR(64)
        GENERATED ALWAYS AS (CASE WHEN status='ACTIVE' THEN display_name ELSE NULL END) STORED,
    ADD UNIQUE KEY uk_template_active_display_name (club_id,active_display_name);

ALTER TABLE aoo_play_version
    ADD COLUMN updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3) AFTER created_at;

CREATE TABLE aoo_concurrency_contract (
    table_name VARCHAR(64) NOT NULL,
    mutation_scope VARCHAR(128) NOT NULL,
    concurrency_strategy VARCHAR(24) NOT NULL,
    version_column VARCHAR(64) NULL,
    update_time_column VARCHAR(64) NULL,
    compare_and_set_predicate VARCHAR(512) NOT NULL,
    retry_policy VARCHAR(128) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    PRIMARY KEY (table_name,mutation_scope),
    CONSTRAINT chk_concurrency_strategy CHECK (concurrency_strategy IN ('ROW_VERSION','FENCING_TOKEN','APPEND_ONLY','BUSINESS_IDEMPOTENCY','SERIALIZATION_LOCK','CLAIM_TOKEN')),
    CONSTRAINT chk_concurrency_version CHECK (concurrency_strategy NOT IN ('ROW_VERSION','FENCING_TOKEN','CLAIM_TOKEN') OR version_column IS NOT NULL),
    CONSTRAINT chk_concurrency_status CHECK (status IN ('ACTIVE','RETIRED'))
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO aoo_concurrency_contract(
    table_name,mutation_scope,concurrency_strategy,version_column,
    update_time_column,compare_and_set_predicate,retry_policy
) VALUES
('aoo_game_catalog','catalog row','ROW_VERSION','row_version','updated_at','WHERE game_id=? AND row_version=?','reject stale editor'),
('aoo_region','region row','ROW_VERSION','row_version','updated_at','WHERE region_code=? AND row_version=?','reject stale editor'),
('aoo_play_version','play version status','ROW_VERSION','row_version','updated_at','WHERE game_id=? AND play_version=? AND row_version=?','reject stale editor'),
('aoo_compiled_index_active','game+region activation','SERIALIZATION_LOCK',NULL,'activated_at','catalog row SELECT FOR UPDATE then atomic upsert','aoo_activate_compiled_game_index'),
('aoo_room_template','template version','ROW_VERSION','row_version','updated_at','WHERE club_id=? AND template_code=? AND template_version=? AND row_version=?','retry from latest immutable version'),
('aoo_club_member','club member','ROW_VERSION','row_version','updated_at','WHERE club_id=? AND player_id=? AND row_version=?','club guard then retry'),
('aoo_club_guard','club aggregate','ROW_VERSION','row_version','updated_at','WHERE club_id=? AND row_version=?','bounded deadlock retry'),
('aoo_currency_balance','player currency scope','ROW_VERSION','version','updated_at','WHERE player_id=? AND currency=? AND currency_scope_id=? AND version=?','idempotent ledger transaction retry'),
('aoo_room_lease','room ownership','FENCING_TOKEN','fencing_token','expires_at','WHERE room_id=? AND fencing_token=?','new owner uses larger fencing token'),
('aoo_room_snapshot','room state','FENCING_TOKEN','fencing_token','captured_at','WHERE room_id=? AND fencing_token=? AND state_version=?','reject stale writer'),
('aoo_room_event','room history','APPEND_ONLY',NULL,'created_at','INSERT unique business identity','write compensation event'),
('aoo_ledger','asset history','BUSINESS_IDEMPOTENCY',NULL,'created_at','INSERT unique business_id','return recorded result'),
('aoo_settlement','room settlement','BUSINESS_IDEMPOTENCY',NULL,'created_at','INSERT unique business_id','return recorded result'),
('aoo_business_idempotency','request result','BUSINESS_IDEMPOTENCY',NULL,'created_at','INSERT unique scope+request id','return recorded result'),
('aoo_outbox','publisher claim','CLAIM_TOKEN','claim_token','created_at','WHERE event_id=? AND claim_token=? AND locked_until>?','reclaim after fenced lease expiry'),
('aoo_consumed_event','consumer claim','CLAIM_TOKEN','claim_token','processed_at','WHERE consumer_name=? AND event_id=? AND claim_token=?','repeat stored result');

INSERT INTO aoo_soft_delete_contract(
    table_name,deleted_column,active_unique_key,restore_conflict_policy,
    history_query_policy,detector_sql
) VALUES
('aoo_room_template','status','uk_template_active_display_name(club_id,active_display_name)','REJECT','INCLUDE_EXPLICIT','SELECT club_id,display_name FROM aoo_room_template WHERE status=''ACTIVE'' GROUP BY club_id,display_name HAVING COUNT(*)>1'),
('aoo_club_member','member_status','PRIMARY KEY(club_id,player_id)','MERGE','INCLUDE_EXPLICIT','SELECT club_id,player_id FROM aoo_club_member GROUP BY club_id,player_id HAVING COUNT(*)>1'),
('aoo_game_catalog','status','uk_game_catalog_code(game_code)','REJECT','INCLUDE_EXPLICIT','SELECT game_code FROM aoo_game_catalog GROUP BY game_code HAVING COUNT(*)>1');

INSERT INTO aoo_json_schema_registry(
    schema_code,schema_version,owner_code,json_schema,content_hash,status
) VALUES
('GAME_RELEASE_CATALOG',1,'GAME_CATALOG',JSON_OBJECT('type','object'),'a2c799262a3ce3c19ef5cdd983bf3d12b43ab3c426227091b909dcb7054738c0','ACTIVE'),
('GAME_RELEASE_RULES',1,'GAME_CATALOG',JSON_OBJECT('type','object'),'a2c799262a3ce3c19ef5cdd983bf3d12b43ab3c426227091b909dcb7054738c0','ACTIVE'),
('GAME_RELEASE_UI',1,'GAME_CATALOG',JSON_OBJECT('type','object'),'a2c799262a3ce3c19ef5cdd983bf3d12b43ab3c426227091b909dcb7054738c0','ACTIVE'),
('ROOM_RULE_LOCK',1,'GAME_RUNTIME',JSON_OBJECT('type','object'),'a2c799262a3ce3c19ef5cdd983bf3d12b43ab3c426227091b909dcb7054738c0','ACTIVE'),
('ROOM_EVENT_PAYLOAD',1,'GAME_RUNTIME',JSON_OBJECT('type','object'),'a2c799262a3ce3c19ef5cdd983bf3d12b43ab3c426227091b909dcb7054738c0','ACTIVE'),
('SETTLEMENT_RESULT',1,'GAME_RUNTIME',JSON_OBJECT('type','object'),'a2c799262a3ce3c19ef5cdd983bf3d12b43ab3c426227091b909dcb7054738c0','ACTIVE'),
('OUTBOX_EVENT',1,'PLATFORM_DATA',JSON_OBJECT('type','object'),'a2c799262a3ce3c19ef5cdd983bf3d12b43ab3c426227091b909dcb7054738c0','ACTIVE');

INSERT INTO aoo_shard_route_contract(
    domain_code,table_name,route_key,route_algorithm,virtual_shard_count,
    physical_shard_count,cross_shard_query_policy,maximum_fanout
) VALUES
('PLAYER','aoo_session','user_id','MURMUR3_MOD',1024,64,'FORBIDDEN',1),
('CLUB','aoo_club_member','club_id','MURMUR3_MOD',1024,64,'FORBIDDEN',1),
('CLUB','aoo_room_template','club_id','MURMUR3_MOD',1024,64,'FORBIDDEN',1),
('ROOM','aoo_room_snapshot','room_id','SNOWFLAKE_ROUTE_BITS',1024,64,'FORBIDDEN',1),
('ROOM','aoo_room_event','room_id','SNOWFLAKE_ROUTE_BITS',1024,64,'FORBIDDEN',1),
('HISTORY','perspective_replay_event','room_id','SNOWFLAKE_ROUTE_BITS',1024,64,'FORBIDDEN',1),
('ASSET','aoo_currency_balance','player_id','MURMUR3_MOD',1024,64,'FORBIDDEN',1),
('ASSET','aoo_ledger','player_id','MURMUR3_MOD',1024,64,'BOUNDED_FANOUT',4);

INSERT INTO aoo_table_governance(
    table_name,owner_code,data_classification,authoritative_source,
    hot_retention_days,archive_retention_days,purge_after_days,
    legal_hold_supported,anonymization_strategy,backup_purge_sla_days
) VALUES(
    'aoo_concurrency_contract','PLATFORM_DATA','INTERNAL','AOO_DB',365,730,NULL,
    0,NULL,NULL
);
