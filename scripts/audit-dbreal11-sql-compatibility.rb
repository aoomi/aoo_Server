#!/usr/bin/env ruby
# frozen_string_literal: true

require 'json'
require 'open3'
require 'pathname'

root = Pathname.new(__dir__).parent
output = root / 'docs/generated/dbreal11-sql-compatibility.json'
migrations = Dir[(root / 'database/migrations/*.sql').to_s].sort
invalid_utf8 = migrations.reject { |path| File.binread(path).force_encoding('UTF-8').valid_encoding? }
bom_files = migrations.select { |path| File.binread(path, 3) == "\xEF\xBB\xBF".b }
delimiter_errors = migrations.select do |path|
  lines = File.readlines(path, encoding: 'UTF-8')
  delimiter_lines = lines.count { |line| line.match?(/^DELIMITER\s+/i) }
  delimiter_lines.odd?
end
runner = (root / 'tools/apply_migrations.sh').read
previous = output.file? ? JSON.parse(output.read) : {}
integration = previous.fetch('integrationEvidence', { 'passed' => false })
if ENV['AOO_DBREAL11_INTEGRATION'] == '1'
  password = ENV.fetch('AOO_MIGRATION_MYSQL_PASSWORD')
  verify_out, verify_err, verify_status = Open3.capture3({ 'AOO_MIGRATION_MYSQL_PASSWORD' => password }, (root / 'tools/verify_fresh_migrations.sh').to_s)
  query = "SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci; SET time_zone='+00:00'; SET SESSION sql_mode='STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION'; SELECT VERSION(),@@character_set_connection,@@collation_connection,@@session.time_zone,@@session.sql_mode,CONVERT('中文🙂' USING utf8mb4);"
  query_out, query_err, query_status = Open3.capture3('docker', 'exec', '-e', "MYSQL_PWD=#{password}", 'aoo-mysql', 'mysql', '--default-character-set=utf8mb4', '-uroot', '-Nse', query)
  fields = query_out.strip.split("\t")
  integration = {
    'passed' => verify_status.success? && query_status.success? && fields.length >= 6 && fields[1] == 'utf8mb4' && fields[2] == 'utf8mb4_0900_ai_ci' && fields[3] == '+00:00' && fields[4].include?('STRICT_TRANS_TABLES') && fields[5] == '中文🙂',
    'targetVersion' => fields[0],
    'characterSet' => fields[1],
    'collation' => fields[2],
    'sessionTimeZone' => fields[3],
    'sqlMode' => fields[4],
    'unicodeRoundTrip' => fields[5] == '中文🙂',
    'migrationOutputTail' => (verify_out + verify_err).lines.last(5).join.strip,
    'queryError' => query_err.strip
  }
end
checks = {
  'all_migrations_valid_utf8' => invalid_utf8.empty?,
  'no_utf8_bom' => bom_files.empty?,
  'delimiter_pairs_balanced' => delimiter_errors.empty?,
  'runner_forces_utf8mb4' => runner.include?('FLYWAY_ENCODING="UTF-8"') && runner.include?('SET NAMES utf8mb4'),
  'runner_forces_utc_session' => runner.include?('connectionTimeZone=UTC') && runner.include?("time_zone = '+00:00'"),
  'runner_forces_strict_sql_mode' => runner.include?('STRICT_TRANS_TABLES') && runner.include?('NO_ZERO_DATE'),
  'real_target_mysql_full_execution' => integration['passed'] == true
}
report = {
  'schemaVersion' => 1, 'task' => 'DBREAL11', 'passed' => checks.values.all?,
  'migrationCount' => migrations.length,
  'invalidUtf8Files' => invalid_utf8.map { |path| File.basename(path) },
  'bomFiles' => bom_files.map { |path| File.basename(path) },
  'delimiterErrors' => delimiter_errors.map { |path| File.basename(path) },
  'integrationEvidence' => integration,
  'checks' => checks
}
output.dirname.mkpath
output.write(JSON.pretty_generate(report) + "\n")
abort JSON.generate(report) unless report['passed']
puts "DBREAL11 passed: #{migrations.length} migrations execute under strict UTC utf8mb4 on MySQL #{integration['targetVersion']}"
