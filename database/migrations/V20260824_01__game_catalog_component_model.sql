-- Database-owned game catalog, region, structured rule/UI and component model.
-- MySQL 8.0.16+. Codes are canonicalized by CHECK constraints and protected by
-- case-insensitive unique keys so differently-cased aliases cannot coexist.

CREATE TABLE aoo_card_category (
    category_code VARCHAR(32) NOT NULL,
    display_name VARCHAR(64) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (category_code),
    CONSTRAINT chk_card_category_code CHECK (category_code REGEXP '^[A-Z][A-Z0-9_]{0,31}$'),
    CONSTRAINT chk_card_category_status CHECK (status IN ('ACTIVE','RETIRED'))
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE aoo_game_family (
    family_code VARCHAR(64) NOT NULL,
    category_code VARCHAR(32) NOT NULL,
    display_name VARCHAR(64) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (family_code),
    KEY idx_game_family_category (category_code,status,family_code),
    CONSTRAINT fk_game_family_category FOREIGN KEY (category_code)
        REFERENCES aoo_card_category(category_code) ON DELETE RESTRICT,
    CONSTRAINT chk_game_family_code CHECK (family_code REGEXP '^[A-Z][A-Z0-9_]{0,63}$'),
    CONSTRAINT chk_game_family_status CHECK (status IN ('ACTIVE','RETIRED'))
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE aoo_region (
    region_code VARCHAR(32) NOT NULL,
    parent_region_code VARCHAR(32) NULL,
    region_type VARCHAR(16) NOT NULL,
    country_code CHAR(2) NULL,
    subdivision_code VARCHAR(16) NULL,
    display_name VARCHAR(64) NOT NULL,
    path VARCHAR(256) NOT NULL,
    depth TINYINT UNSIGNED NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    row_version BIGINT UNSIGNED NOT NULL DEFAULT 1,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (region_code),
    UNIQUE KEY uk_region_path (path),
    KEY idx_region_parent (parent_region_code,status,region_code),
    KEY idx_region_country_subdivision (country_code,subdivision_code,status),
    CONSTRAINT fk_region_parent FOREIGN KEY (parent_region_code)
        REFERENCES aoo_region(region_code) ON DELETE RESTRICT,
    CONSTRAINT chk_region_code CHECK (region_code REGEXP '^[A-Z0-9][A-Z0-9-]{0,31}$'),
    CONSTRAINT chk_region_type CHECK (region_type IN ('GLOBAL','NONE','COUNTRY','PROVINCE','CITY','DISTRICT')),
    CONSTRAINT chk_region_status CHECK (status IN ('ACTIVE','RETIRED')),
    CONSTRAINT chk_region_depth CHECK (depth <= 4),
    CONSTRAINT chk_region_parent_shape CHECK (
        (region_type IN ('GLOBAL','NONE','COUNTRY') AND parent_region_code IS NULL)
        OR (region_type IN ('PROVINCE','CITY','DISTRICT') AND parent_region_code IS NOT NULL)
    )
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE aoo_region_alias (
    source_system VARCHAR(32) NOT NULL,
    source_region_code VARCHAR(64) NOT NULL,
    region_code VARCHAR(32) NOT NULL,
    mapping_status VARCHAR(16) NOT NULL DEFAULT 'VERIFIED',
    verified_by BIGINT UNSIGNED NULL,
    verified_at DATETIME(3) NULL,
    PRIMARY KEY (source_system,source_region_code),
    KEY idx_region_alias_target (region_code,mapping_status),
    CONSTRAINT fk_region_alias_target FOREIGN KEY (region_code)
        REFERENCES aoo_region(region_code) ON DELETE RESTRICT,
    CONSTRAINT chk_region_alias_status CHECK (mapping_status IN ('PROPOSED','VERIFIED','REJECTED'))
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE aoo_game_catalog (
    game_id BIGINT UNSIGNED NOT NULL,
    game_code VARCHAR(64) NOT NULL,
    display_name VARCHAR(128) NOT NULL,
    category_code VARCHAR(32) NOT NULL,
    family_code VARCHAR(64) NOT NULL,
    provider_key VARCHAR(192) NOT NULL,
    catalog_schema_version INT UNSIGNED NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
    row_version BIGINT UNSIGNED NOT NULL DEFAULT 1,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (game_id),
    UNIQUE KEY uk_game_catalog_code (game_code),
    UNIQUE KEY uk_game_catalog_provider (provider_key),
    KEY idx_game_catalog_classification (category_code,family_code,status,game_id),
    CONSTRAINT fk_game_catalog_category FOREIGN KEY (category_code)
        REFERENCES aoo_card_category(category_code) ON DELETE RESTRICT,
    CONSTRAINT fk_game_catalog_family FOREIGN KEY (family_code)
        REFERENCES aoo_game_family(family_code) ON DELETE RESTRICT,
    CONSTRAINT chk_game_catalog_id CHECK (game_id > 0),
    CONSTRAINT chk_game_catalog_code CHECK (game_code REGEXP '^[a-z][a-z0-9_]{0,63}$'),
    CONSTRAINT chk_game_catalog_status CHECK (status IN ('DRAFT','ACTIVE','SUSPENDED','RETIRED')),
    CONSTRAINT chk_game_catalog_schema_version CHECK (catalog_schema_version > 0)
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE aoo_game_region (
    game_id BIGINT UNSIGNED NOT NULL,
    region_code VARCHAR(32) NOT NULL,
    availability VARCHAR(16) NOT NULL DEFAULT 'AVAILABLE',
    priority SMALLINT UNSIGNED NOT NULL DEFAULT 100,
    available_from DATETIME(3) NULL,
    available_until DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (game_id,region_code),
    KEY idx_game_region_lookup (region_code,availability,priority,game_id),
    CONSTRAINT fk_game_region_game FOREIGN KEY (game_id)
        REFERENCES aoo_game_catalog(game_id) ON DELETE RESTRICT,
    CONSTRAINT fk_game_region_region FOREIGN KEY (region_code)
        REFERENCES aoo_region(region_code) ON DELETE RESTRICT,
    CONSTRAINT chk_game_region_availability CHECK (availability IN ('AVAILABLE','HIDDEN','BLOCKED','RETIRED')),
    CONSTRAINT chk_game_region_window CHECK (available_until IS NULL OR available_from IS NULL OR available_until > available_from)
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE aoo_play_version (
    game_id BIGINT UNSIGNED NOT NULL,
    play_version VARCHAR(64) NOT NULL,
    default_region_code VARCHAR(32) NOT NULL,
    rule_schema_version INT UNSIGNED NOT NULL,
    ui_schema_version INT UNSIGNED NOT NULL,
    component_schema_version INT UNSIGNED NOT NULL,
    content_hash CHAR(64) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
    compatibility_floor VARCHAR(64) NULL,
    activated_at DATETIME(3) NULL,
    retired_at DATETIME(3) NULL,
    row_version BIGINT UNSIGNED NOT NULL DEFAULT 1,
    created_by BIGINT UNSIGNED NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (game_id,play_version),
    UNIQUE KEY uk_play_version_content (game_id,content_hash),
    KEY idx_play_version_status (status,game_id,activated_at),
    KEY idx_play_version_region (default_region_code,status,game_id),
    CONSTRAINT fk_play_version_game FOREIGN KEY (game_id)
        REFERENCES aoo_game_catalog(game_id) ON DELETE RESTRICT,
    CONSTRAINT fk_play_version_region FOREIGN KEY (default_region_code)
        REFERENCES aoo_region(region_code) ON DELETE RESTRICT,
    CONSTRAINT chk_play_version_name CHECK (play_version REGEXP '^[a-z0-9][a-z0-9._-]{0,63}$'),
    CONSTRAINT chk_play_version_hash CHECK (content_hash REGEXP '^[0-9a-f]{64}$'),
    CONSTRAINT chk_play_version_status CHECK (status IN ('DRAFT','VALIDATED','ACTIVE','RETIRED')),
    CONSTRAINT chk_play_version_schemas CHECK (rule_schema_version > 0 AND ui_schema_version > 0 AND component_schema_version > 0),
    CONSTRAINT chk_play_version_lifecycle CHECK (retired_at IS NULL OR activated_at IS NOT NULL)
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE aoo_room_rule_definition (
    game_id BIGINT UNSIGNED NOT NULL,
    play_version VARCHAR(64) NOT NULL,
    rule_key VARCHAR(96) NOT NULL,
    rule_kind VARCHAR(24) NOT NULL,
    value_type VARCHAR(16) NOT NULL,
    required_value TINYINT UNSIGNED NOT NULL DEFAULT 0,
    default_value JSON NULL,
    minimum_value DECIMAL(38,9) NULL,
    maximum_value DECIMAL(38,9) NULL,
    validation_pattern VARCHAR(512) NULL,
    description VARCHAR(500) NOT NULL,
    ordinal INT UNSIGNED NOT NULL,
    schema_version INT UNSIGNED NOT NULL,
    content_hash CHAR(64) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    PRIMARY KEY (game_id,play_version,rule_key),
    UNIQUE KEY uk_room_rule_ordinal (game_id,play_version,ordinal),
    KEY idx_room_rule_kind (rule_kind,status,game_id,play_version),
    CONSTRAINT fk_room_rule_play_version FOREIGN KEY (game_id,play_version)
        REFERENCES aoo_play_version(game_id,play_version) ON DELETE RESTRICT,
    CONSTRAINT chk_room_rule_key CHECK (rule_key REGEXP '^[a-z][a-zA-Z0-9_.-]{0,95}$'),
    CONSTRAINT chk_room_rule_kind CHECK (rule_kind IN ('ROOM','DECK','HAND','FIRST_MOVE','ACTION','FLOW','SCORING','SETTLEMENT','REPLAY')),
    CONSTRAINT chk_room_rule_value_type CHECK (value_type IN ('BOOLEAN','INTEGER','DECIMAL','STRING','ENUM','LIST','OBJECT')),
    CONSTRAINT chk_room_rule_required CHECK (required_value IN (0,1)),
    CONSTRAINT chk_room_rule_range CHECK (maximum_value IS NULL OR minimum_value IS NULL OR maximum_value >= minimum_value),
    CONSTRAINT chk_room_rule_hash CHECK (content_hash REGEXP '^[0-9a-f]{64}$'),
    CONSTRAINT chk_room_rule_status CHECK (status IN ('ACTIVE','DEPRECATED'))
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE aoo_room_rule_choice (
    game_id BIGINT UNSIGNED NOT NULL,
    play_version VARCHAR(64) NOT NULL,
    rule_key VARCHAR(96) NOT NULL,
    choice_key VARCHAR(96) NOT NULL,
    wire_value JSON NOT NULL,
    display_name VARCHAR(128) NOT NULL,
    ordinal INT UNSIGNED NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    PRIMARY KEY (game_id,play_version,rule_key,choice_key),
    UNIQUE KEY uk_room_rule_choice_ordinal (game_id,play_version,rule_key,ordinal),
    CONSTRAINT fk_room_rule_choice_rule FOREIGN KEY (game_id,play_version,rule_key)
        REFERENCES aoo_room_rule_definition(game_id,play_version,rule_key) ON DELETE RESTRICT,
    CONSTRAINT chk_room_rule_choice_status CHECK (status IN ('ACTIVE','DEPRECATED'))
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE aoo_create_ui_option (
    game_id BIGINT UNSIGNED NOT NULL,
    play_version VARCHAR(64) NOT NULL,
    option_key VARCHAR(96) NOT NULL,
    control_type VARCHAR(24) NOT NULL,
    title VARCHAR(128) NOT NULL,
    default_value JSON NULL,
    required_value TINYINT UNSIGNED NOT NULL DEFAULT 0,
    visible_expression JSON NULL,
    enabled_expression JSON NULL,
    mutual_exclusion_group VARCHAR(96) NULL,
    ordinal INT UNSIGNED NOT NULL,
    schema_version INT UNSIGNED NOT NULL,
    content_hash CHAR(64) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    PRIMARY KEY (game_id,play_version,option_key),
    UNIQUE KEY uk_create_ui_option_ordinal (game_id,play_version,ordinal),
    CONSTRAINT fk_create_ui_option_play_version FOREIGN KEY (game_id,play_version)
        REFERENCES aoo_play_version(game_id,play_version) ON DELETE RESTRICT,
    CONSTRAINT chk_create_ui_option_key CHECK (option_key REGEXP '^[a-z][a-zA-Z0-9_.-]{0,95}$'),
    CONSTRAINT chk_create_ui_control CHECK (control_type IN ('RADIO','CHECKBOX','SELECT','NUMBER','TEXT','SLIDER','HIDDEN')),
    CONSTRAINT chk_create_ui_required CHECK (required_value IN (0,1)),
    CONSTRAINT chk_create_ui_hash CHECK (content_hash REGEXP '^[0-9a-f]{64}$'),
    CONSTRAINT chk_create_ui_status CHECK (status IN ('ACTIVE','DEPRECATED'))
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE aoo_create_ui_choice (
    game_id BIGINT UNSIGNED NOT NULL,
    play_version VARCHAR(64) NOT NULL,
    option_key VARCHAR(96) NOT NULL,
    choice_key VARCHAR(96) NOT NULL,
    wire_value JSON NOT NULL,
    display_name VARCHAR(128) NOT NULL,
    ordinal INT UNSIGNED NOT NULL,
    visible_expression JSON NULL,
    PRIMARY KEY (game_id,play_version,option_key,choice_key),
    UNIQUE KEY uk_create_ui_choice_ordinal (game_id,play_version,option_key,ordinal),
    CONSTRAINT fk_create_ui_choice_option FOREIGN KEY (game_id,play_version,option_key)
        REFERENCES aoo_create_ui_option(game_id,play_version,option_key) ON DELETE RESTRICT
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE aoo_ui_rule_binding (
    game_id BIGINT UNSIGNED NOT NULL,
    play_version VARCHAR(64) NOT NULL,
    option_key VARCHAR(96) NOT NULL,
    rule_key VARCHAR(96) NOT NULL,
    mapping_expression JSON NOT NULL,
    validation_expression JSON NOT NULL,
    priority SMALLINT UNSIGNED NOT NULL DEFAULT 100,
    PRIMARY KEY (game_id,play_version,option_key,rule_key),
    KEY idx_ui_rule_binding_rule (game_id,play_version,rule_key,priority),
    CONSTRAINT fk_ui_rule_binding_option FOREIGN KEY (game_id,play_version,option_key)
        REFERENCES aoo_create_ui_option(game_id,play_version,option_key) ON DELETE RESTRICT,
    CONSTRAINT fk_ui_rule_binding_rule FOREIGN KEY (game_id,play_version,rule_key)
        REFERENCES aoo_room_rule_definition(game_id,play_version,rule_key) ON DELETE RESTRICT
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE aoo_room_cost_policy (
    game_id BIGINT UNSIGNED NOT NULL,
    play_version VARCHAR(64) NOT NULL,
    region_code VARCHAR(32) NOT NULL,
    round_count INT UNSIGNED NOT NULL,
    player_count SMALLINT UNSIGNED NOT NULL,
    payer_mode VARCHAR(16) NOT NULL,
    currency_code VARCHAR(32) NOT NULL,
    cost_minor BIGINT UNSIGNED NOT NULL,
    club_cost_minor BIGINT UNSIGNED NULL,
    union_cost_minor BIGINT UNSIGNED NULL,
    policy_version INT UNSIGNED NOT NULL,
    content_hash CHAR(64) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    PRIMARY KEY (game_id,play_version,region_code,round_count,player_count,payer_mode),
    KEY idx_room_cost_lookup (region_code,game_id,play_version,player_count,round_count,status),
    CONSTRAINT fk_room_cost_play_version FOREIGN KEY (game_id,play_version)
        REFERENCES aoo_play_version(game_id,play_version) ON DELETE RESTRICT,
    CONSTRAINT fk_room_cost_region FOREIGN KEY (region_code)
        REFERENCES aoo_region(region_code) ON DELETE RESTRICT,
    CONSTRAINT fk_room_cost_currency FOREIGN KEY (currency_code)
        REFERENCES aoo_currency_catalog(currency_code) ON DELETE RESTRICT,
    CONSTRAINT chk_room_cost_counts CHECK (round_count > 0 AND player_count > 0),
    CONSTRAINT chk_room_cost_payer CHECK (payer_mode IN ('OWNER','AA','WINNER','CLUB','UNION')),
    CONSTRAINT chk_room_cost_currency CHECK (currency_code REGEXP '^[A-Z][A-Z0-9_]{0,31}$'),
    CONSTRAINT chk_room_cost_hash CHECK (content_hash REGEXP '^[0-9a-f]{64}$'),
    CONSTRAINT chk_room_cost_status CHECK (status IN ('ACTIVE','RETIRED'))
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE aoo_game_help_content (
    game_id BIGINT UNSIGNED NOT NULL,
    play_version VARCHAR(64) NOT NULL,
    locale_code VARCHAR(16) NOT NULL,
    block_ordinal INT UNSIGNED NOT NULL,
    block_type VARCHAR(16) NOT NULL,
    title VARCHAR(256) NULL,
    body TEXT NULL,
    image_uri VARCHAR(512) NULL,
    content_hash CHAR(64) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    PRIMARY KEY (game_id,play_version,locale_code,block_ordinal),
    KEY idx_game_help_lookup (game_id,play_version,locale_code,status,block_ordinal),
    CONSTRAINT fk_game_help_play_version FOREIGN KEY (game_id,play_version)
        REFERENCES aoo_play_version(game_id,play_version) ON DELETE RESTRICT,
    CONSTRAINT chk_game_help_locale CHECK (locale_code REGEXP '^[a-z]{2}(-[A-Z]{2})?$'),
    CONSTRAINT chk_game_help_block_type CHECK (block_type IN ('TITLE','PARAGRAPH','IMAGE','LIST')),
    CONSTRAINT chk_game_help_content CHECK (title IS NOT NULL OR body IS NOT NULL OR image_uri IS NOT NULL),
    CONSTRAINT chk_game_help_hash CHECK (content_hash REGEXP '^[0-9a-f]{64}$'),
    CONSTRAINT chk_game_help_status CHECK (status IN ('ACTIVE','RETIRED'))
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE aoo_component_type (
    component_type VARCHAR(32) NOT NULL,
    expected_spi_type VARCHAR(192) NOT NULL,
    required_for_active_game TINYINT UNSIGNED NOT NULL,
    lifecycle_order SMALLINT UNSIGNED NOT NULL,
    description VARCHAR(500) NOT NULL,
    PRIMARY KEY (component_type),
    UNIQUE KEY uk_component_type_order (lifecycle_order),
    CONSTRAINT chk_component_type_code CHECK (component_type REGEXP '^[A-Z][A-Z0-9_]{0,31}$'),
    CONSTRAINT chk_component_type_required CHECK (required_for_active_game IN (0,1))
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE aoo_game_component (
    component_id BIGINT UNSIGNED NOT NULL,
    component_key VARCHAR(192) NOT NULL,
    component_version VARCHAR(64) NOT NULL,
    game_id BIGINT UNSIGNED NULL,
    component_type VARCHAR(32) NOT NULL,
    spi_type VARCHAR(192) NOT NULL,
    implementation_locator VARCHAR(512) NOT NULL,
    artifact_digest CHAR(64) NOT NULL,
    parameter_schema JSON NOT NULL,
    schema_version INT UNSIGNED NOT NULL,
    content_hash CHAR(64) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (component_id),
    UNIQUE KEY uk_game_component_key_version (component_key,component_version),
    KEY idx_game_component_lookup (game_id,component_type,status,component_version),
    CONSTRAINT fk_game_component_game FOREIGN KEY (game_id)
        REFERENCES aoo_game_catalog(game_id) ON DELETE RESTRICT,
    CONSTRAINT fk_game_component_type FOREIGN KEY (component_type)
        REFERENCES aoo_component_type(component_type) ON DELETE RESTRICT,
    CONSTRAINT chk_game_component_id CHECK (component_id > 0),
    CONSTRAINT chk_game_component_key CHECK (component_key REGEXP '^[a-zA-Z0-9][a-zA-Z0-9_.:-]{0,191}$'),
    CONSTRAINT chk_game_component_digest CHECK (artifact_digest REGEXP '^[0-9a-f]{64}$'),
    CONSTRAINT chk_game_component_hash CHECK (content_hash REGEXP '^[0-9a-f]{64}$'),
    CONSTRAINT chk_game_component_schema CHECK (schema_version > 0),
    CONSTRAINT chk_game_component_status CHECK (status IN ('DRAFT','VALIDATED','ACTIVE','RETIRED'))
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE aoo_component_dependency (
    component_id BIGINT UNSIGNED NOT NULL,
    required_component_id BIGINT UNSIGNED NOT NULL,
    version_constraint VARCHAR(128) NOT NULL,
    priority SMALLINT UNSIGNED NOT NULL DEFAULT 100,
    optional_dependency TINYINT UNSIGNED NOT NULL DEFAULT 0,
    PRIMARY KEY (component_id,required_component_id),
    KEY idx_component_dependency_reverse (required_component_id,component_id),
    CONSTRAINT fk_component_dependency_source FOREIGN KEY (component_id)
        REFERENCES aoo_game_component(component_id) ON DELETE RESTRICT,
    CONSTRAINT fk_component_dependency_target FOREIGN KEY (required_component_id)
        REFERENCES aoo_game_component(component_id) ON DELETE RESTRICT,
    CONSTRAINT chk_component_dependency_self CHECK (component_id <> required_component_id),
    CONSTRAINT chk_component_dependency_optional CHECK (optional_dependency IN (0,1))
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE aoo_component_conflict (
    component_id BIGINT UNSIGNED NOT NULL,
    conflicting_component_id BIGINT UNSIGNED NOT NULL,
    reason VARCHAR(500) NOT NULL,
    priority SMALLINT UNSIGNED NOT NULL DEFAULT 100,
    PRIMARY KEY (component_id,conflicting_component_id),
    KEY idx_component_conflict_reverse (conflicting_component_id,component_id),
    CONSTRAINT fk_component_conflict_source FOREIGN KEY (component_id)
        REFERENCES aoo_game_component(component_id) ON DELETE RESTRICT,
    CONSTRAINT fk_component_conflict_target FOREIGN KEY (conflicting_component_id)
        REFERENCES aoo_game_component(component_id) ON DELETE RESTRICT,
    CONSTRAINT chk_component_conflict_self CHECK (component_id <> conflicting_component_id),
    CONSTRAINT chk_component_conflict_order CHECK (component_id < conflicting_component_id)
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE aoo_play_component_binding (
    game_id BIGINT UNSIGNED NOT NULL,
    play_version VARCHAR(64) NOT NULL,
    component_type VARCHAR(32) NOT NULL,
    ordinal SMALLINT UNSIGNED NOT NULL,
    component_id BIGINT UNSIGNED NOT NULL,
    parameters JSON NOT NULL,
    priority SMALLINT UNSIGNED NOT NULL DEFAULT 100,
    PRIMARY KEY (game_id,play_version,component_type,ordinal),
    UNIQUE KEY uk_play_component_instance (game_id,play_version,component_id),
    KEY idx_play_component_component (component_id,game_id,play_version),
    CONSTRAINT fk_play_component_play_version FOREIGN KEY (game_id,play_version)
        REFERENCES aoo_play_version(game_id,play_version) ON DELETE RESTRICT,
    CONSTRAINT fk_play_component_type FOREIGN KEY (component_type)
        REFERENCES aoo_component_type(component_type) ON DELETE RESTRICT,
    CONSTRAINT fk_play_component_component FOREIGN KEY (component_id)
        REFERENCES aoo_game_component(component_id) ON DELETE RESTRICT
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO aoo_card_category(category_code,display_name) VALUES
('MAHJONG','麻将'),('POKER','扑克'),('LONG_CARD','长牌'),('WORD_CARD','字牌'),('OTHER','其他');

INSERT INTO aoo_game_family(family_code,category_code,display_name) VALUES
('MAHJONG_UNCLASSIFIED','MAHJONG','麻将·待细分'),
('POKER_UNCLASSIFIED','POKER','扑克·待细分'),
('LONG_CARD_UNCLASSIFIED','LONG_CARD','长牌·待细分'),
('WORD_CARD_UNCLASSIFIED','WORD_CARD','字牌·待细分'),
('OTHER_UNCLASSIFIED','OTHER','其他·待细分');

INSERT INTO aoo_region(region_code,parent_region_code,region_type,country_code,subdivision_code,display_name,path,depth) VALUES
('GLOBAL',NULL,'GLOBAL',NULL,NULL,'全国/全球','/GLOBAL',0),
('NONE',NULL,'NONE',NULL,NULL,'无地区','/NONE',0),
('CN',NULL,'COUNTRY','CN',NULL,'中国','/CN',0),
('JP',NULL,'COUNTRY','JP',NULL,'日本','/JP',0),
('IN',NULL,'COUNTRY','IN',NULL,'印度','/IN',0);

INSERT INTO aoo_region(region_code,parent_region_code,region_type,country_code,subdivision_code,display_name,path,depth) VALUES
('CN-11','CN','PROVINCE','CN','11','北京','/CN/CN-11',1),
('CN-12','CN','PROVINCE','CN','12','天津','/CN/CN-12',1),
('CN-13','CN','PROVINCE','CN','13','河北','/CN/CN-13',1),
('CN-14','CN','PROVINCE','CN','14','山西','/CN/CN-14',1),
('CN-15','CN','PROVINCE','CN','15','内蒙古','/CN/CN-15',1),
('CN-21','CN','PROVINCE','CN','21','辽宁','/CN/CN-21',1),
('CN-22','CN','PROVINCE','CN','22','吉林','/CN/CN-22',1),
('CN-23','CN','PROVINCE','CN','23','黑龙江','/CN/CN-23',1),
('CN-31','CN','PROVINCE','CN','31','上海','/CN/CN-31',1),
('CN-32','CN','PROVINCE','CN','32','江苏','/CN/CN-32',1),
('CN-33','CN','PROVINCE','CN','33','浙江','/CN/CN-33',1),
('CN-34','CN','PROVINCE','CN','34','安徽','/CN/CN-34',1),
('CN-35','CN','PROVINCE','CN','35','福建','/CN/CN-35',1),
('CN-36','CN','PROVINCE','CN','36','江西','/CN/CN-36',1),
('CN-37','CN','PROVINCE','CN','37','山东','/CN/CN-37',1),
('CN-41','CN','PROVINCE','CN','41','河南','/CN/CN-41',1),
('CN-42','CN','PROVINCE','CN','42','湖北','/CN/CN-42',1),
('CN-43','CN','PROVINCE','CN','43','湖南','/CN/CN-43',1),
('CN-44','CN','PROVINCE','CN','44','广东','/CN/CN-44',1),
('CN-45','CN','PROVINCE','CN','45','广西','/CN/CN-45',1),
('CN-46','CN','PROVINCE','CN','46','海南','/CN/CN-46',1),
('CN-50','CN','PROVINCE','CN','50','重庆','/CN/CN-50',1),
('CN-51','CN','PROVINCE','CN','51','四川','/CN/CN-51',1),
('CN-52','CN','PROVINCE','CN','52','贵州','/CN/CN-52',1),
('CN-53','CN','PROVINCE','CN','53','云南','/CN/CN-53',1),
('CN-54','CN','PROVINCE','CN','54','西藏','/CN/CN-54',1),
('CN-61','CN','PROVINCE','CN','61','陕西','/CN/CN-61',1),
('CN-62','CN','PROVINCE','CN','62','甘肃','/CN/CN-62',1),
('CN-64','CN','PROVINCE','CN','64','宁夏','/CN/CN-64',1),
('CN-65','CN','PROVINCE','CN','65','新疆','/CN/CN-65',1),
('CN-71','CN','PROVINCE','CN','71','台湾','/CN/CN-71',1);

INSERT INTO aoo_region(region_code,parent_region_code,region_type,country_code,subdivision_code,display_name,path,depth) VALUES
('CN-23-01','CN-23','CITY','CN','2301','哈尔滨','/CN/CN-23/CN-23-01',2),
('CN-37-03','CN-37','CITY','CN','3703','淄博','/CN/CN-37/CN-37-03',2),
('CN-37-03-02','CN-37-03','DISTRICT','CN','370302','淄川','/CN/CN-37/CN-37-03/CN-37-03-02',3);

INSERT INTO aoo_region_alias(source_system,source_region_code,region_code,mapping_status,verified_at) VALUES
('legacy-gamelist','all','GLOBAL','VERIFIED',CURRENT_TIMESTAMP(3)),
('legacy-gamelist','anhui','CN-34','VERIFIED',CURRENT_TIMESTAMP(3)),
('legacy-gamelist','chongqing','CN-50','VERIFIED',CURRENT_TIMESTAMP(3)),
('legacy-gamelist','fujian','CN-35','VERIFIED',CURRENT_TIMESTAMP(3)),
('legacy-gamelist','fujiang','CN-35','VERIFIED',CURRENT_TIMESTAMP(3)),
('legacy-gamelist','gansu','CN-62','VERIFIED',CURRENT_TIMESTAMP(3)),
('legacy-gamelist','guangdong','CN-44','VERIFIED',CURRENT_TIMESTAMP(3)),
('legacy-gamelist','guangxi','CN-45','VERIFIED',CURRENT_TIMESTAMP(3)),
('legacy-gamelist','guizhou','CN-52','VERIFIED',CURRENT_TIMESTAMP(3)),
('legacy-gamelist','haerbin','CN-23-01','VERIFIED',CURRENT_TIMESTAMP(3)),
('legacy-gamelist','hainan','CN-46','VERIFIED',CURRENT_TIMESTAMP(3)),
('legacy-gamelist','hebei','CN-13','VERIFIED',CURRENT_TIMESTAMP(3)),
('legacy-gamelist','heilongjiang','CN-23','VERIFIED',CURRENT_TIMESTAMP(3)),
('legacy-gamelist','henan','CN-41','VERIFIED',CURRENT_TIMESTAMP(3)),
('legacy-gamelist','henna','CN-41','VERIFIED',CURRENT_TIMESTAMP(3)),
('legacy-gamelist','hubei','CN-42','VERIFIED',CURRENT_TIMESTAMP(3)),
('legacy-gamelist','hunan','CN-43','VERIFIED',CURRENT_TIMESTAMP(3)),
('legacy-gamelist','jiangsu','CN-32','VERIFIED',CURRENT_TIMESTAMP(3)),
('legacy-gamelist','jiangxi','CN-36','VERIFIED',CURRENT_TIMESTAMP(3)),
('legacy-gamelist','jilin','CN-22','VERIFIED',CURRENT_TIMESTAMP(3)),
('legacy-gamelist','liaoning','CN-21','VERIFIED',CURRENT_TIMESTAMP(3)),
('legacy-gamelist','neimenggu','CN-15','VERIFIED',CURRENT_TIMESTAMP(3)),
('legacy-gamelist','ningxia','CN-64','VERIFIED',CURRENT_TIMESTAMP(3)),
('legacy-gamelist','riben','JP','VERIFIED',CURRENT_TIMESTAMP(3)),
('legacy-gamelist','shandong','CN-37','VERIFIED',CURRENT_TIMESTAMP(3)),
('legacy-gamelist','shanghai','CN-31','VERIFIED',CURRENT_TIMESTAMP(3)),
('legacy-gamelist','shanxi','CN-14','VERIFIED',CURRENT_TIMESTAMP(3)),
('legacy-gamelist','shanxisheng','CN-61','VERIFIED',CURRENT_TIMESTAMP(3)),
('legacy-gamelist','sichaun','CN-51','VERIFIED',CURRENT_TIMESTAMP(3)),
('legacy-gamelist','sichuan','CN-51','VERIFIED',CURRENT_TIMESTAMP(3)),
('legacy-gamelist','taiwan','CN-71','VERIFIED',CURRENT_TIMESTAMP(3)),
('legacy-gamelist','tianjin','CN-12','VERIFIED',CURRENT_TIMESTAMP(3)),
('legacy-gamelist','xinjiang','CN-65','VERIFIED',CURRENT_TIMESTAMP(3)),
('legacy-gamelist','xizang','CN-54','VERIFIED',CURRENT_TIMESTAMP(3)),
('legacy-gamelist','yindu','IN','VERIFIED',CURRENT_TIMESTAMP(3)),
('legacy-gamelist','yunnan','CN-53','VERIFIED',CURRENT_TIMESTAMP(3)),
('legacy-gamelist','zhejiang','CN-33','VERIFIED',CURRENT_TIMESTAMP(3)),
('legacy-gamelist','zichuan','CN-37-03-02','VERIFIED',CURRENT_TIMESTAMP(3));

INSERT INTO aoo_component_type(component_type,expected_spi_type,required_for_active_game,lifecycle_order,description) VALUES
('PROVIDER','com.aoo.bcg.gamespi.GameProvider',1,10,'玩法入口与实例化'),
('LIFECYCLE','com.aoo.bcg.gamespi.RuleComponent',1,20,'房间生命周期'),
('RULE','com.aoo.bcg.gamespi.RuleComponent',1,30,'规则与合法性判断'),
('ACTION','com.aoo.bcg.gamespi.RuleComponent',1,40,'玩家动作处理'),
('FLOW','com.aoo.bcg.gamespi.RuleComponent',1,50,'局/盘流程'),
('SCORING','com.aoo.bcg.gamespi.RuleComponent',1,60,'算分'),
('SETTLEMENT','com.aoo.bcg.gamespi.RuleComponent',1,70,'结算'),
('SNAPSHOT','com.aoo.bcg.gamespi.RuleComponent',1,80,'权威快照与恢复'),
('REPLAY','com.aoo.bcg.gamespi.RuleComponent',1,90,'历史事件解释与回放'),
('UI','com.aoo.bcg.gamespi.RuleComponent',1,100,'创建界面与规则映射');
