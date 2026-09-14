CREATE TABLE aoo_luck_draw_campaign (
 id BIGINT NOT NULL AUTO_INCREMENT,campaign_code VARCHAR(64) NOT NULL,campaign_name VARCHAR(128) NOT NULL,
 starts_at TIMESTAMP(3) NOT NULL,ends_at TIMESTAMP(3) NOT NULL,enabled BOOLEAN NOT NULL,daily_free_count BIGINT NOT NULL,
 PRIMARY KEY(id),UNIQUE KEY uk_luck_draw_campaign_code(campaign_code),CONSTRAINT chk_luck_draw_window CHECK(ends_at>starts_at),CONSTRAINT chk_luck_draw_daily CHECK(daily_free_count>=0)
);
CREATE TABLE aoo_luck_draw_prize (
 id BIGINT NOT NULL AUTO_INCREMENT,campaign_id BIGINT NOT NULL,prize_code VARCHAR(64) NOT NULL,prize_name VARCHAR(128) NOT NULL,
 reward_kind VARCHAR(16) NOT NULL,reward_code VARCHAR(64) NOT NULL,reward_amount BIGINT NOT NULL,weight BIGINT NOT NULL,enabled BOOLEAN NOT NULL,
 PRIMARY KEY(id),UNIQUE KEY uk_luck_draw_prize(campaign_id,prize_code),CONSTRAINT fk_luck_draw_prize_campaign FOREIGN KEY(campaign_id) REFERENCES aoo_luck_draw_campaign(id),
 CONSTRAINT chk_luck_draw_reward_kind CHECK(reward_kind IN ('CURRENCY','ITEM')),CONSTRAINT chk_luck_draw_reward_amount CHECK(reward_amount>0),CONSTRAINT chk_luck_draw_weight CHECK(weight>0)
);
CREATE TABLE aoo_luck_draw_chance (
 player_id BIGINT NOT NULL,campaign_id BIGINT NOT NULL,business_date DATE NOT NULL,remaining_count BIGINT NOT NULL,version BIGINT NOT NULL,updated_at TIMESTAMP(3) NOT NULL,
 PRIMARY KEY(player_id,campaign_id,business_date),CONSTRAINT fk_luck_draw_chance_campaign FOREIGN KEY(campaign_id) REFERENCES aoo_luck_draw_campaign(id),CONSTRAINT chk_luck_draw_remaining CHECK(remaining_count>=0)
);
CREATE TABLE aoo_luck_draw (
 id BIGINT NOT NULL AUTO_INCREMENT,request_id VARCHAR(128) NOT NULL,request_fingerprint VARCHAR(160) NOT NULL,player_id BIGINT NOT NULL,campaign_id BIGINT NOT NULL,campaign_code VARCHAR(64) NOT NULL,
 prize_id BIGINT NOT NULL,prize_code VARCHAR(64) NOT NULL,prize_name VARCHAR(128) NOT NULL,reward_kind VARCHAR(16) NOT NULL,reward_code VARCHAR(64) NOT NULL,reward_amount BIGINT NOT NULL,remaining_count BIGINT NOT NULL,created_at TIMESTAMP(3) NOT NULL,
 PRIMARY KEY(id),UNIQUE KEY uk_luck_draw_request(request_id),KEY idx_luck_draw_player_time(player_id,created_at),CONSTRAINT fk_luck_draw_campaign FOREIGN KEY(campaign_id) REFERENCES aoo_luck_draw_campaign(id),CONSTRAINT fk_luck_draw_prize FOREIGN KEY(prize_id) REFERENCES aoo_luck_draw_prize(id)
);
CREATE TABLE aoo_luck_draw_outbox (
 draw_id BIGINT NOT NULL,event_key VARCHAR(160) NOT NULL,event_type VARCHAR(64) NOT NULL,payload_json JSON NOT NULL,status VARCHAR(16) NOT NULL,attempt_count INT NOT NULL,next_attempt_at TIMESTAMP(3) NOT NULL,last_error VARCHAR(500),created_at TIMESTAMP(3) NOT NULL,updated_at TIMESTAMP(3) NOT NULL,
 PRIMARY KEY(draw_id),UNIQUE KEY uk_luck_draw_outbox_event(event_key),KEY idx_luck_draw_outbox_dispatch(status,next_attempt_at),CONSTRAINT fk_luck_draw_outbox_draw FOREIGN KEY(draw_id) REFERENCES aoo_luck_draw(id),CONSTRAINT chk_luck_draw_outbox_status CHECK(status IN ('PENDING','DELIVERED','DEAD'))
);
