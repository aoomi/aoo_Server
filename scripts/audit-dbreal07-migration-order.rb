#!/usr/bin/env ruby
# frozen_string_literal: true

require 'json'
require 'open3'
require 'pathname'

root = Pathname.new(__dir__).parent
tool = root / 'tools/list_migrations.rb'
stdout, stderr, status = Open3.capture3('ruby', tool.to_s)
abort "DBREAL07 failed: #{stderr}" unless status.success?
paths = stdout.lines.map(&:strip).reject(&:empty?)
names = paths.map { |path| File.basename(path) }
expected_patch_order = %w[
  V20260822_04__admin_permissions.sql
  V20260822_04_1__room_recovery.sql
  V20260822_04_2__currency_balance.sql
  V20260822_04_3__outbox_retry.sql
]
indexes = expected_patch_order.map { |name| names.index(name) }
checks = {
  'all_sql_migrations_listed' => paths.length == Dir[(root / 'database/migrations/*.sql').to_s].length,
  'all_paths_exist' => paths.all? { |path| File.file?(path) },
  'versions_unique' => names.uniq.length == names.length,
  'patch_series_ordered_after_base' => indexes.none?(&:nil?) && indexes == indexes.sort && indexes.each_cons(2).all? { |a, b| b == a + 1 },
  'fresh_runner_uses_authoritative_order_tool' =>
    (root / 'tools/verify_fresh_migrations.sh').read.include?('tools/apply_migrations.sh') &&
    (root / 'tools/apply_migrations.sh').read.include?('FLYWAY_VALIDATE_MIGRATION_NAMING="true"') &&
    (root / 'tools/apply_migrations.sh').read.match?(/FLYWAY_LOCATIONS=.*filesystem:\$\{ROOT\}\/database\/migrations/)
}
report = {
  'task' => 'DBREAL07',
  'passed' => checks.values.all?,
  'migrationCount' => paths.length,
  'filenameGrammar' => 'V<YYYYMMDD>_<sequence>[ _<patch>]__<description>.sql (spaces omitted)',
  'orderingKey' => ['date', 'sequence', 'patch; base=0'],
  'orderedMigrations' => names,
  'checks' => checks
}
(root / 'work/audit').mkpath
(root / 'work/audit/dbreal07-migration-order.json').write(JSON.pretty_generate(report) + "\n")
abort JSON.generate(report) unless report['passed']
puts "DBREAL07 passed: #{paths.length} unique migrations have deterministic tool-enforced order"
