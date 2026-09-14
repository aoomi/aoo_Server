CREATE TABLE social_friend_request (
  id BIGINT NOT NULL AUTO_INCREMENT,
  requester_id BIGINT NOT NULL,
  recipient_id BIGINT NOT NULL,
  status VARCHAR(16) NOT NULL,
  created_at TIMESTAMP(3) NOT NULL,
  decided_at TIMESTAMP(3) NULL,
  PRIMARY KEY (id),
  KEY ix_social_friend_request_recipient (recipient_id,status,id),
  KEY ix_social_friend_request_pair (requester_id,recipient_id,status),
  CONSTRAINT ck_social_friend_request_distinct CHECK (requester_id <> recipient_id),
  CONSTRAINT ck_social_friend_request_status CHECK (status IN ('PENDING','ACCEPTED','REJECTED','CANCELLED'))
) ENGINE=InnoDB;

CREATE TABLE social_friendship (
  user_low BIGINT NOT NULL,
  user_high BIGINT NOT NULL,
  created_at TIMESTAMP(3) NOT NULL,
  PRIMARY KEY (user_low,user_high),
  KEY ix_social_friendship_high (user_high,user_low),
  CONSTRAINT ck_social_friendship_order CHECK (user_low < user_high)
) ENGINE=InnoDB;

CREATE TABLE social_block (
  blocker_id BIGINT NOT NULL,
  blocked_id BIGINT NOT NULL,
  created_at TIMESTAMP(3) NOT NULL,
  PRIMARY KEY (blocker_id,blocked_id),
  KEY ix_social_block_reverse (blocked_id,blocker_id),
  CONSTRAINT ck_social_block_distinct CHECK (blocker_id <> blocked_id)
) ENGINE=InnoDB;

CREATE TABLE social_presence (
  account_id BIGINT NOT NULL,
  state VARCHAR(16) NOT NULL,
  room_id BIGINT NULL,
  visibility VARCHAR(16) NOT NULL,
  last_seen_at TIMESTAMP(3) NOT NULL,
  PRIMARY KEY (account_id),
  CONSTRAINT ck_social_presence_state CHECK (state IN ('ONLINE','AWAY','OFFLINE')),
  CONSTRAINT ck_social_presence_visibility CHECK (visibility IN ('FRIENDS','NOBODY'))
) ENGINE=InnoDB;

CREATE TABLE social_notification (
  id BIGINT NOT NULL AUTO_INCREMENT,
  recipient_id BIGINT NOT NULL,
  type VARCHAR(32) NOT NULL,
  dedupe_key VARCHAR(128) NOT NULL,
  payload TEXT NOT NULL,
  created_at TIMESTAMP(3) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_social_notification_dedupe (recipient_id,dedupe_key),
  KEY ix_social_notification_feed (recipient_id,id)
) ENGINE=InnoDB;

CREATE TABLE social_mail (
  id BIGINT NOT NULL AUTO_INCREMENT,
  recipient_id BIGINT NOT NULL,
  dedupe_key VARCHAR(128) NOT NULL,
  subject VARCHAR(256) NOT NULL,
  body TEXT NOT NULL,
  created_at TIMESTAMP(3) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_social_mail_dedupe (recipient_id,dedupe_key),
  KEY ix_social_mail_feed (recipient_id,id)
) ENGINE=InnoDB;

CREATE TABLE social_read_cursor (
  account_id BIGINT NOT NULL,
  stream VARCHAR(16) NOT NULL,
  cursor_id BIGINT NOT NULL,
  updated_at TIMESTAMP(3) NOT NULL,
  PRIMARY KEY (account_id,stream),
  CONSTRAINT ck_social_read_cursor_stream CHECK (stream IN ('NOTIFICATION','MAIL')),
  CONSTRAINT ck_social_read_cursor_nonnegative CHECK (cursor_id >= 0)
) ENGINE=InnoDB;
