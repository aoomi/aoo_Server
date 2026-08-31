#!/usr/bin/env ruby
# frozen_string_literal: true

require 'json'
require 'time'

root = File.expand_path('..', __dir__)
mapping = JSON.parse(File.read(File.join(root, 'work/audit/database-runtime-mapping.json'), encoding: 'UTF-8')).fetch('mappings')
output_path = File.join(root, 'work/audit/region-reference-integrity.json')
task_path = File.join(root, 'docs/Aoo-前后端全框架功能通信审计任务清单.md')

sql_files = Dir.glob(File.join(root, '**', '*.sql')).select do |path|
  File.file?(path) && File.size(path) <= 100_000_000 && path.split('/').none? { |part| %w[target build .git work].include?(part) }
end.sort
schema = sql_files.each_with_object([]) do |path, found|
  text = File.binread(path).force_encoding('UTF-8').scrub
  next unless text.match?(/(?:region|province|city|district|area_code)/i)
  found << {
    path: path.delete_prefix(root + '/'),
    hierarchyTables: text.scan(/CREATE\s+TABLE\s+(?:IF\s+NOT\s+EXISTS\s+)?[`"]?([A-Za-z0-9_]*(?:region|province|city|district|area)[A-Za-z0-9_]*)/i).flatten.uniq.sort,
    parentForeignKey: text.match?(/FOREIGN\s+KEY\s*\([^)]*(?:parent|province|region|city)[^)]*\)\s+REFERENCES/i),
    gameRegionForeignKey: text.match?(/FOREIGN\s+KEY\s*\([^)]*(?:region|province|city)[^)]*\)\s+REFERENCES/i),
    uniqueRegionCode: text.match?(/UNIQUE[^\n]*(?:region_code|province_code|city_code|area_code)/i)
  }
end

code_pattern = /\A(?:all|national|[A-Z]{2}|\d{6}|[a-z][a-z0-9_-]{1,31})\z/i
games = mapping.reject { |game| game['category'] == 'INFRASTRUCTURE' }.map do |game|
  region = game['region']
  issues = []
  issues << 'REGION_MISSING' if region.nil? || region.to_s.empty?
  issues << 'REGION_CODE_NON_CANONICAL' if !region.nil? && !region.to_s.empty? && !region.to_s.match?(code_pattern)
  issues << 'DATABASE_RECORD_MISSING' unless game['databaseMapped']
  {code: game['code'], gameId: game['gameId'], region: region, issues: issues, valid: issues.empty?}
end
region_counts = games.group_by { |game| game[:region].to_s }.transform_values(&:size).sort.to_h
summary = {
  gameCount: games.size,
  validGames: games.count { |game| game[:valid] },
  invalidGames: games.count { |game| !game[:valid] },
  missingRegion: games.count { |game| game[:issues].include?('REGION_MISSING') },
  nonCanonicalRegion: games.count { |game| game[:issues].include?('REGION_CODE_NON_CANONICAL') },
  distinctRegions: region_counts.size,
  schemaFiles: schema.size,
  hierarchyTables: schema.flat_map { |row| row[:hierarchyTables] }.uniq.size,
  parentForeignKeyFiles: schema.count { |row| row[:parentForeignKey] },
  gameRegionForeignKeyFiles: schema.count { |row| row[:gameRegionForeignKey] },
  uniqueRegionCodeFiles: schema.count { |row| row[:uniqueRegionCode] }
}
report = {
  schemaVersion: 1, generatedAt: Time.now.utc.iso8601, summary: summary,
  invariant: 'Country/province/city/district codes use a canonical hierarchy, unique codes and enforced parent/game foreign keys; no enabled game references an orphan region.',
  regionCounts: region_counts, schemaEvidence: schema, games: games,
  limitations: ['Static files cannot prove deployed hierarchy rows or orphan absence; live anti-join checks against production-compatible snapshots remain required.']
}
File.write(output_path, JSON.pretty_generate(report) + "\n", mode: 'w:UTF-8')

task = File.read(task_path, encoding: 'UTF-8')
row = "| REF02 | 未完成 | 未通过·已建地区门禁 | 已审计地区代码规范、层级表、父级/玩法外键及唯一代码；玩法=#{summary[:gameCount]}、地区有效=#{summary[:validGames]}、缺地区=#{summary[:missingRegion]}、非规范代码=#{summary[:nonCanonicalRegion]}、层级表=#{summary[:hierarchyTables]}、含父级外键文件=#{summary[:parentForeignKeyFiles]}、含玩法地区外键=#{summary[:gameRegionForeignKeyFiles]}；尚需真实快照反连接孤儿检测。 证据：work/audit/region-reference-integrity.json |"
task.sub!(/^\| REF02 \|.*$/, row) or abort 'REF02 row not found'
task.sub!(/^下一项：.*$/, '下一项：REF03 真实玩法组件完整性') or abort 'next pointer not found'
File.write(task_path, task, mode: 'w:UTF-8')
puts JSON.generate(summary)
