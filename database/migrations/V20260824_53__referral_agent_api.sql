-- Durable commission command/status only. Relationships and performance remain authoritative in legacy dbClubmember/PlayerRoomAlone.
CREATE TABLE promoter_commission_instruction (
 command_id VARCHAR(128) NOT NULL,club_id BIGINT NOT NULL,promoter_id BIGINT NOT NULL,
 from_date INT NOT NULL,to_date INT NOT NULL,base_amount DECIMAL(20,2) NOT NULL,
 rate DECIMAL(9,6) NOT NULL,commission_amount BIGINT NOT NULL,currency VARCHAR(32) NOT NULL,
 status ENUM('CALCULATED','SETTLED') NOT NULL,billing_reference VARCHAR(160) NULL,
 created_at DATETIME(3) NOT NULL,updated_at DATETIME(3) NOT NULL,
 PRIMARY KEY(command_id),KEY idx_commission_promoter_status(promoter_id,status,created_at),
 CONSTRAINT chk_commission_period CHECK(from_date<=to_date),CONSTRAINT chk_commission_rate CHECK(rate>=0 AND rate<=1),
 CONSTRAINT chk_commission_amount CHECK(base_amount>=0 AND commission_amount>=0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE referral_invite_reward_policy(kind ENUM('ROOM','CLUB') NOT NULL,currency VARCHAR(32) NOT NULL,reward_amount DECIMAL(20,2) NOT NULL,enabled TINYINT NOT NULL,PRIMARY KEY(kind),CONSTRAINT chk_invite_reward_amount CHECK(reward_amount>0)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE referral_invite_reward_instruction(idempotency_key VARCHAR(128) NOT NULL,consumption_id BIGINT NOT NULL,invite_id BIGINT NOT NULL,issuer_account_id BIGINT NOT NULL,consumer_account_id BIGINT NOT NULL,kind ENUM('ROOM','CLUB') NOT NULL,target_id BIGINT NOT NULL,currency VARCHAR(32) NOT NULL,reward_amount DECIMAL(20,2) NOT NULL,status ENUM('RESERVED','GRANTED') NOT NULL,billing_reference VARCHAR(160) NULL,created_at DATETIME(3) NOT NULL,updated_at DATETIME(3) NOT NULL,PRIMARY KEY(idempotency_key),UNIQUE KEY uk_referral_consumption(consumption_id),KEY ix_referral_consumer(consumer_account_id,created_at)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
