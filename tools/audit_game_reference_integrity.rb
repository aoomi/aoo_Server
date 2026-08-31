#!/usr/bin/env ruby
# frozen_string_literal: true

require 'json'
require 'time'

root = File.expand_path('..', __dir__)
registration = JSON.parse(File.read(File.join(root, 'work/audit/registration-conservation-audit.json'), encoding: 'UTF-8'))
classification = JSON.parse(File.read(File.join(root, 'work/generated/classification/game-classification.json'), encoding: 'UTF-8'))
output_path = File.join(root, 'work/audit/game-reference-integrity.json')
task_path = File.join(root, 'docs/Aoo-前后端全框架功能通信审计任务清单.md')

sql_files = Dir.glob(File.join(root, '**', '*.sql')).select do |path|
  File.file?(path) && File.size(path) <= 100_000_000 && path.split('/').none? { |part| %w[target build .git work].include?(part) }
end.sort
schema_evidence = []
sql_files.each do |path|
  text = File.binread(path).force_encoding('UTF-8').scrub
  next unless text.match?(/(?:game_id|game_code|game_catalog|game_classification|game_version)/i)
  schema_evidence << {
    path: path.delete_prefix(root + '/'),
    foreignKey: text.match?(/FOREIGN\s+KEY\s*\([^)]*game_(?:id|code)[^)]*\)\s+REFERENCES/i),
    enabledConstraint: text.match?(/(?:CHECK\s*\([^)]*(?:enabled|is_open)|(?:enabled|is_open)[^,\n]*(?:NOT\s+NULL|DEFAULT))/i),
    uniqueCode: text.match?(/(?:UNIQUE\s*(?:KEY|INDEX)?\s*[^\n]*(?:game_code|game_name)|UNIQUE\s*\([^)]*(?:game_code|game_name))/i),
    versionReference: text.match?(/(?:game_version|play_version)/i)
  }
end

classification_rows = classification.fetch('rows')
registration_by_code = registration.fetch('rows').to_h { |row| [row.fetch('code'), row] }
rows = classification_rows.reject { |row| row['category'] == 'INFRASTRUCTURE' }.map do |row|
  registered = registration_by_code.fetch(row.fetch('code'), {})
  violations = []
  violations << 'GAME_ID_MISSING' if row['gameId'].nil?
  violations << 'CATEGORY_MISSING' if row['category'].to_s.empty?
  violations << 'FAMILY_MISSING' if row['family'].to_s.empty?
  violations << 'DATABASE_REGISTRATION_MISSING' unless registered['databaseRegistered']
  violations << 'NATIVE_PROVIDER_MISSING' unless registered['nativeProviderRegistered']
  violations << 'ROUTE_REGISTRATION_MISSING' unless registered['routeRegistered']
  {gameId: row['gameId'], code: row['code'], category: row['category'], family: row['family'],
   sourceType: row['sourceType'], violations: violations, valid: violations.empty?}
end

summary = {
  gameCount: rows.size,
  validGames: rows.count { |row| row[:valid] },
  invalidGames: rows.count { |row| !row[:valid] },
  violationCounts: rows.flat_map { |row| row[:violations] }.group_by(&:itself).transform_values(&:size).sort.to_h,
  schemaFiles: schema_evidence.size,
  schemaFilesWithForeignKey: schema_evidence.count { |row| row[:foreignKey] },
  schemaFilesWithEnabledConstraint: schema_evidence.count { |row| row[:enabledConstraint] },
  schemaFilesWithUniqueCode: schema_evidence.count { |row| row[:uniqueCode] },
  schemaFilesWithVersionReference: schema_evidence.count { |row| row[:versionReference] }
}
report = {
  schemaVersion: 1, generatedAt: Time.now.utc.iso8601, summary: summary,
  invariant: 'Every enabled game row has unique id/code, valid category/family/version references and a closed native runtime registration; child records use enforced foreign keys.',
  schemaEvidence: schema_evidence, games: rows,
  limitations: ['Static DDL cannot prove constraints are deployed or enabled rows instantiate; live information_schema and startup registry reconciliation remain required.']
}
File.write(output_path, JSON.pretty_generate(report) + "\n", mode: 'w:UTF-8')

task = File.read(task_path, encoding: 'UTF-8')
row = "| REF01 | 未完成 | 未通过·已建引用门禁 | 已将分类/玩法族元数据、DB 登记、Provider/路由注册与 DDL 外键/启用/唯一约束聚合审计；玩法=#{summary[:gameCount]}、有效=#{summary[:validGames]}、无效=#{summary[:invalidGames]}、DDL 证据=#{summary[:schemaFiles]}、含外键=#{summary[:schemaFilesWithForeignKey]}、含启用约束=#{summary[:schemaFilesWithEnabledConstraint]}、含唯一 code=#{summary[:schemaFilesWithUniqueCode]}；尚需线上 information_schema 与启动实例化对账。 证据：work/audit/game-reference-integrity.json |"
task.sub!(/^\| REF01 \|.*$/, row) or abort 'REF01 row not found'
task.sub!(/^下一项：.*$/, '下一项：REF02 地区引用完整性') or abort 'next pointer not found'
File.write(task_path, task, mode: 'w:UTF-8')
puts JSON.generate(summary)
