CREATE TABLE social_notice (
  id BIGINT NOT NULL AUTO_INCREMENT,
  title VARCHAR(256) NOT NULL,
  content VARCHAR(4096) NOT NULL,
  starts_at TIMESTAMP NOT NULL,
  ends_at TIMESTAMP NOT NULL,
  published BOOLEAN NOT NULL DEFAULT FALSE,
  updated_at TIMESTAMP NOT NULL,
  PRIMARY KEY (id),
  KEY ix_social_notice_active (published,starts_at,ends_at,id),
  CONSTRAINT ck_social_notice_window CHECK (ends_at > starts_at)
);

ALTER TABLE social_read_cursor DROP CHECK ck_social_read_cursor_stream;
ALTER TABLE social_read_cursor ADD CONSTRAINT ck_social_read_cursor_stream CHECK (stream IN ('NOTIFICATION','MAIL','NOTICE'));
