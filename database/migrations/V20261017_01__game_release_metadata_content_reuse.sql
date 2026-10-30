-- Local isolated metadata-content reuse. User call28bc0700040a495686c831551b119875.
-- DDL implicitly commits; preserve all rows, real content hashes and release identity/version uniqueness.
-- Scope deliberately qualified to the approved local schema; no historical migration rewrite.
ALTER TABLE `aoo_local_dev_login_20261016_r1`.`aoo_game_release`
  DROP INDEX `uk_game_release_bundle`,
  ADD INDEX `ix_game_release_bundle` (`game_id`,`bundle_hash`),
  ALGORITHM=INPLACE, LOCK=NONE;
