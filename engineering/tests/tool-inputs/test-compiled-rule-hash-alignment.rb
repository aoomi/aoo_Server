#!/usr/bin/env ruby
require 'digest'

root = File.expand_path('../../..', __dir__)
migration_path = File.join(root, 'database/migrations/V20261012_04__repair_compiled_rule_hash_alignment.sql')
hall_path = File.join(root, 'server/Hall/src/main/java/com/aoo/bcg/lobby/JdbcLobbyRepository.java')
migration = File.read(migration_path, encoding: 'UTF-8')
hall = File.read(hall_path, encoding: 'UTF-8')

required = {
  'forward migration name' => migration.include?('repair_compiled_rule_hash_alignment_v20261012_04'),
  'compiled validator byte hash' => migration.include?('SHA2(CAST(i.rule_validator AS CHAR CHARACTER SET utf8mb4),256)'),
  'retry-safe procedure lifecycle' => migration.include?('DROP PROCEDURE IF EXISTS repair_compiled_rule_hash_alignment_v20261012_04'),
  'active pointer join' => migration.include?('FROM aoo_compiled_index_active a'),
  'active compiled row gate' => migration.include?("i.lifecycle_state='ACTIVE'"),
  'active release gate' => migration.include?("r.status='ACTIVE'"),
  'current catalog gate' => migration.include?("g.status='ACTIVE'"),
  'exact current cardinality' => migration.include?('expected_identity_count INT DEFAULT 32'),
  'transaction rollback' => migration.include?('ROLLBACK;') && migration.include?('COMMIT;'),
  'fail closed postcheck' => migration.include?('COMPILED_RULE_HASH_ALIGNMENT_POSTCHECK_FAILED'),
  'hall compares release and compiled hashes' => hall.include?('release rule hash is inconsistent with the compiled validator')
}
failed = required.select { |_name, passed| !passed }
abort("compiled rule hash alignment contract failed: #{failed.keys.join(', ')}") unless failed.empty?

forbidden = {
  'historical snapshot rewrite' => migration.match?(/SET\s+r\.rule_snapshot\s*=/i),
  'compiled validator rewrite' => migration.match?(/SET\s+i\.rule_validator\s*=/i),
  'workbook/default derivation' => migration.match?(/MAX\s*\(|DEFAULT\s*\(/i),
  'protocol change' => migration.match?(/protocol_version\s*=/i)
}
violations = forbidden.select { |_name, present| present }
abort("compiled rule hash alignment forbidden mutation: #{violations.keys.join(', ')}") unless violations.empty?

puts 'compiled rule hash alignment contract: PASS'
puts "migration_sha256=#{Digest::SHA256.file(migration_path).hexdigest}"
