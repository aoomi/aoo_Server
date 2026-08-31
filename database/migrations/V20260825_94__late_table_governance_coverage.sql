-- Close governance coverage for late 2026-08-25 migrations. INSERT IGNORE keeps this
-- compatible if an owning migration adds a more specific row during rolling merges.
INSERT IGNORE INTO aoo_table_governance(
 table_name,owner_code,data_classification,authoritative_source,hot_retention_days,
 archive_retention_days,purge_after_days,legal_hold_supported,anonymization_strategy,backup_purge_sla_days
) VALUES
('aoo_display_id_segment','SECURITY_PRIVACY','INTERNAL','AOO_DB',3650,3650,NULL,0,NULL,NULL),
('aoo_account_identity','SECURITY_PRIVACY','RESTRICTED','AOO_DB',365,2555,3650,1,'IDENTITY_VALUE_HASH',35),
('aoo_display_id_history','SECURITY_PRIVACY','CONFIDENTIAL','AOO_DB',365,2555,3650,1,'ACCOUNT_ID_TOKENIZE',35),
('aoo_identity_admin_request','SECURITY_PRIVACY','CONFIDENTIAL','AOO_DB',90,365,730,1,'OPERATOR_ID_TOKENIZE',35),
('aoo_device_pin','SECURITY_PRIVACY','RESTRICTED','AOO_DB',90,365,730,1,'DEVICE_ID_TOKENIZE',35),
('aoo_device_challenge','SECURITY_PRIVACY','RESTRICTED','AOO_DB',7,30,90,0,'DEVICE_ID_TOKENIZE',35),
('aoo_identity_migration','SECURITY_PRIVACY','RESTRICTED','AOO_DB',365,2555,3650,1,'ACCOUNT_ID_TOKENIZE',35),
('aoo_room_authority_route','GAME_RUNTIME','INTERNAL','AOO_DB',30,180,730,1,'PLAYER_ID_TOKENIZE',35),
('aoo_room_billing_reservation','PLAYER_ASSET','RESTRICTED','AOO_DB',365,2555,3650,1,'PLAYER_ID_TOKENIZE',35),
('aoo_room_create_saga','GAME_RUNTIME','CONFIDENTIAL','AOO_DB',30,180,730,1,'PLAYER_ID_TOKENIZE',35),
('aoo_room_idempotent_allocation','GAME_RUNTIME','INTERNAL','AOO_DB',30,180,730,1,'PLAYER_ID_TOKENIZE',35),
('aoo_room_id_sequence','GAME_RUNTIME','INTERNAL','AOO_DB',3650,3650,NULL,0,NULL,NULL),
('aoo_region_semantics_policy','GAME_CATALOG','INTERNAL','AOO_DB',3650,3650,NULL,0,NULL,NULL),
('aoo_region_identity_legacy_snapshot','SECURITY_PRIVACY','RESTRICTED','AOO_DB',365,730,730,1,'ENTITY_ID_TOKENIZE',35),
('playerWallet','PLAYER_ASSET','RESTRICTED','AOO_DB',730,2555,3650,1,'PLAYER_ID_TOKENIZE',35);
