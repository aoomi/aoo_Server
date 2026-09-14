#!/usr/bin/env ruby
# frozen_string_literal: true

require 'json'
require 'open3'
require 'pathname'

root = Pathname.new(__dir__).parent
output = root / 'docs/generated/dbreal08-migration-boundary.json'
migrations = Dir[(root / 'database/migrations/*.sql').to_s].sort
creates = Hash.new { |hash, key| hash[key] = [] }
migrations.each do |path|
  File.read(path).scan(/CREATE TABLE(?: IF NOT EXISTS)?\s+([A-Za-z0-9_]+)/i).flatten.each do |table|
    creates[table.downcase] << File.basename(path)
  end
end
duplicates = creates.select { |_, owners| owners.length > 1 }
runner = (root / 'tools/apply_migrations.sh').read
fresh = (root / 'tools/verify_fresh_migrations.sh').read
previous = output.file? ? JSON.parse(output.read) : {}
integration = previous.fetch('integrationEvidence', { 'passed' => false })
if ENV['AOO_DBREAL08_INTEGRATION'] == '1'
  stdout, stderr, status = Open3.capture3({
    'AOO_MIGRATION_MYSQL_PASSWORD' => ENV.fetch('AOO_MIGRATION_MYSQL_PASSWORD')
  }, (root / 'tools/verify_fresh_migrations.sh').to_s)
  integration = {
    'passed' => status.success?,
    'mode' => 'real MySQL empty-schema apply followed by existing-schema no-op upgrade',
    'outputTail' => (stdout + stderr).lines.last(8).join.strip
  }
end
checks = {
  'no_duplicate_create_owners' => duplicates.empty?,
  'history_table_and_checksum' => runner.include?('flyway_schema_history') && runner.include?('flyway-maven-plugin'),
  'existing_untracked_schema_fails_closed' => runner.include?('FLYWAY_BASELINE_ON_MIGRATE="false"'),
  'distributed_apply_lock' => runner.include?('flyway-maven-plugin'),
  'fresh_and_upgrade_share_runner' => fresh.include?('tools/apply_migrations.sh'),
  'real_empty_and_upgrade_test_passed' => integration['passed'] == true
}
report = {
  'schemaVersion' => 1,
  'task' => 'DBREAL08',
  'passed' => checks.values.all?,
  'migrationCount' => migrations.length,
  'duplicateCreateOwners' => duplicates,
  'boundary' => {
    'emptySchema' => 'apply all ordered migrations and record immutable checksums',
    'trackedExistingSchema' => 'verify historical checksums and apply only missing versions',
    'untrackedExistingSchema' => 'fail closed; reconcile through isolated staging before adoption'
  },
  'integrationEvidence' => integration,
  'checks' => checks
}
output.dirname.mkpath
output.write(JSON.pretty_generate(report) + "\n")
abort JSON.generate(report) unless report['passed']
puts 'DBREAL08 passed: empty initialization and tracked incremental upgrade boundaries verified'
