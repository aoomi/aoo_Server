-- Inventory/Item/Store/Redemption authority. Money remains exclusively in Billing.
CREATE TABLE aoo_item_catalog (
 item_code VARCHAR(64) PRIMARY KEY,item_name VARCHAR(120) NOT NULL,item_type VARCHAR(32) NOT NULL,
 consumable BOOLEAN NOT NULL DEFAULT FALSE,active_from TIMESTAMP(3) NOT NULL,active_until TIMESTAMP(3) NULL,
 attributes_json JSON NOT NULL,CHECK(active_until IS NULL OR active_until>active_from)
);
CREATE TABLE aoo_store_offer (
 offer_code VARCHAR(64) PRIMARY KEY,item_code VARCHAR(64) NOT NULL,item_quantity BIGINT NOT NULL,
 currency_code VARCHAR(32) NOT NULL,price_minor BIGINT NOT NULL,active_from TIMESTAMP(3) NOT NULL,active_until TIMESTAMP(3) NULL,
 FOREIGN KEY(item_code) REFERENCES aoo_item_catalog(item_code),CHECK(item_quantity>0),CHECK(price_minor>0)
);
CREATE TABLE aoo_inventory_command (
 command_id VARCHAR(128) PRIMARY KEY,command_kind VARCHAR(24) NOT NULL,command_state VARCHAR(32) NOT NULL,
 player_id BIGINT NOT NULL,reference_code VARCHAR(128) NOT NULL,error_message VARCHAR(500) NULL,
 created_at TIMESTAMP(3) NOT NULL,updated_at TIMESTAMP(3) NOT NULL,INDEX ix_inventory_command_player(player_id,created_at)
);
CREATE TABLE aoo_inventory_lot (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,player_id BIGINT NOT NULL,item_code VARCHAR(64) NOT NULL,quantity BIGINT NOT NULL,
 expires_at TIMESTAMP(3) NULL,row_version BIGINT NOT NULL,created_at TIMESTAMP(3) NOT NULL,
 FOREIGN KEY(item_code) REFERENCES aoo_item_catalog(item_code),CHECK(quantity>=0),INDEX ix_inventory_lot_consume(player_id,item_code,expires_at,id)
);
CREATE TABLE aoo_inventory_movement (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,command_id VARCHAR(128) NOT NULL,player_id BIGINT NOT NULL,item_code VARCHAR(64) NOT NULL,
 quantity_delta BIGINT NOT NULL,movement_type VARCHAR(24) NOT NULL,source_reference VARCHAR(128) NOT NULL,created_at TIMESTAMP(3) NOT NULL,
 FOREIGN KEY(command_id) REFERENCES aoo_inventory_command(command_id),FOREIGN KEY(item_code) REFERENCES aoo_item_catalog(item_code),
 INDEX ix_inventory_movement_player(player_id,id)
);
CREATE TABLE aoo_redemption_campaign (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,campaign_code VARCHAR(64) NOT NULL UNIQUE,item_code VARCHAR(64) NOT NULL,item_quantity BIGINT NOT NULL,
 grant_expires_at TIMESTAMP(3) NULL,active_from TIMESTAMP(3) NOT NULL,active_until TIMESTAMP(3) NOT NULL,
 FOREIGN KEY(item_code) REFERENCES aoo_item_catalog(item_code),CHECK(item_quantity>0),CHECK(active_until>active_from)
);
CREATE TABLE aoo_redemption_code (
 code_hash CHAR(64) PRIMARY KEY,campaign_id BIGINT NOT NULL,status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
 FOREIGN KEY(campaign_id) REFERENCES aoo_redemption_campaign(id)
);
CREATE TABLE aoo_redemption_claim (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,campaign_id BIGINT NOT NULL,code_hash CHAR(64) NOT NULL,player_id BIGINT NOT NULL,
 command_id VARCHAR(128) NOT NULL,claimed_at TIMESTAMP(3) NOT NULL,
 UNIQUE(code_hash),UNIQUE(campaign_id,player_id),UNIQUE(command_id),
 FOREIGN KEY(campaign_id) REFERENCES aoo_redemption_campaign(id),FOREIGN KEY(code_hash) REFERENCES aoo_redemption_code(code_hash),
 FOREIGN KEY(command_id) REFERENCES aoo_inventory_command(command_id)
);
