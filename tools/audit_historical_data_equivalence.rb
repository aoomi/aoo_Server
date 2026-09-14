#!/usr/bin/env ruby
# frozen_string_literal: true

require 'json'
require 'time'

root = File.expand_path('..', __dir__)
legacy_db_roots = [
  File.expand_path('../../Test/QH_DFMJ/database/original', root),
  File.expand_path('../../Test/QH_DFMJ/database/backups', root)
].freeze
current_db_roots = [File.join(root, 'database'), File.join(root, 'server')].freeze
output_path = File.join(root, 'work/audit/historical-data-equivalence.json')
task_path = File.join(root, 'docs/Aoo-前后端全框架功能通信审计任务清单.md')

domains = {
  'account' => /(?:account|user|player|role|login)/i,
  'asset' => /(?:asset|wallet|currency|diamond|gold|room_card|roomcard|balance)/i,
  'club' => /(?:club|union|family|guild|member)/i,
  'template' => /(?:template|room_config|create_room|club_game)/i,
  'record' => /(?:record|history|battle|match_result|settlement)/i,
  'replay' => /(?:replay|playback|event_log|round_event)/i
}.freeze

def sql_files(roots)
  roots.flat_map do |source_root|
    next [] unless Dir.exist?(source_root)
    Dir.glob(File.join(source_root, '**', '*.sql')).select do |path|
      File.file?(path) && File.size(path) <= 100_000_000 &&
        path.split('/').none? { |part| %w[target build .git].include?(part) }
    end
  end.uniq.sort
end

def inventory(roots, domains)
  tables = {}
  sql_files(roots).each do |path|
    text = File.binread(path).force_encoding('UTF-8').scrub
    text.scan(/CREATE\s+TABLE\s+(?:IF\s+NOT\s+EXISTS\s+)?[`"\[]?([A-Za-z0-9_.$-]+)[`"\]]?\s*\((.*?)\)\s*(?:ENGINE|;)/im).each do |table, body|
      normalized_table = table.downcase
      columns = body.lines.each_with_object([]) do |line, found|
        match = line.strip.match(/\A[`"\[]?([A-Za-z_][A-Za-z0-9_]*)[`"\]]?\s+[A-Za-z]/)
        found << match[1].downcase if match
      end.uniq.sort
      kinds = domains.each_with_object([]) { |(domain, regex), found| found << domain if normalized_table.match?(regex) }
      kinds << 'unclassified' if kinds.empty?
      entry = (tables[normalized_table] ||= {table: normalized_table, domains: [], columns: [], sources: []})
      entry[:domains] |= kinds
      entry[:columns] |= columns
      entry[:sources] << path unless entry[:sources].include?(path)
    end
  end
  tables
end

legacy = inventory(legacy_db_roots, domains)
current = inventory(current_db_roots, domains)
all_tables = (legacy.keys | current.keys).sort
matrix = all_tables.map do |table|
  old = legacy[table]
  new_row = current[table]
  common_columns = old && new_row ? old[:columns] & new_row[:columns] : []
  {
    table: table,
    domains: ((old && old[:domains]) || []) | ((new_row && new_row[:domains]) || []),
    legacyPresent: !old.nil?, currentPresent: !new_row.nil?,
    legacyColumns: old ? old[:columns] : [], currentColumns: new_row ? new_row[:columns] : [],
    commonColumns: common_columns,
    missingCurrentColumns: old && new_row ? old[:columns] - new_row[:columns] : (old ? old[:columns] : []),
    legacySources: old ? old[:sources] : [], currentSources: new_row ? new_row[:sources] : []
  }
end
by_domain = (domains.keys + ['unclassified']).to_h do |domain|
  rows = matrix.select { |row| row[:domains].include?(domain) }
  [domain, {legacy: rows.count { |row| row[:legacyPresent] }, current: rows.count { |row| row[:currentPresent] },
            missingCurrent: rows.count { |row| row[:legacyPresent] && !row[:currentPresent] },
            columnDrift: rows.count { |row| row[:legacyPresent] && row[:currentPresent] && !row[:missingCurrentColumns].empty? }}]
end
summary = {
  legacyTables: legacy.size, currentTables: current.size,
  commonTables: (legacy.keys & current.keys).size,
  missingCurrentTables: (legacy.keys - current.keys).size,
  currentOnlyTables: (current.keys - legacy.keys).size,
  commonTablesWithMissingLegacyColumns: matrix.count { |row| row[:legacyPresent] && row[:currentPresent] && !row[:missingCurrentColumns].empty? },
  byDomain: by_domain
}
report = {
  schemaVersion: 1, generatedAt: Time.now.utc.iso8601,
  legacyRoots: legacy_db_roots, currentRoots: current_db_roots,
  summary: summary, matrix: matrix,
  limitations: ['DDL name/column comparison cannot prove migrated row continuity, value transforms, foreign keys, cache hydration or replay decoder compatibility; snapshot-backed migration tests remain required.']
}
File.write(output_path, JSON.pretty_generate(report) + "\n", mode: 'w:UTF-8')

task = File.read(task_path, encoding: 'UTF-8')
detail = by_domain.map { |name, values| "#{name}=旧#{values[:legacy]}/新#{values[:current]}/缺#{values[:missingCurrent]}/列漂#{values[:columnDrift]}" }.join('、')
row = "| EQ09 | 未完成 | 已建 DDL 矩阵·未闭合 | 已按账号/资产/亲友圈/模板/战绩/回放对照旧新表及列（#{detail}）；旧/新表=#{summary[:legacyTables]}/#{summary[:currentTables]}、旧表缺失=#{summary[:missingCurrentTables]}、列漂移表=#{summary[:commonTablesWithMissingLegacyColumns]}，尚需真实脱敏快照迁移、缓存回填与历史回放解码测试。 证据：work/audit/historical-data-equivalence.json；工具：tools/audit_historical_data_equivalence.rb |"
task.sub!(/^\| EQ09 \|.*$/, row) or abort 'EQ09 row not found'
task.sub!(/^下一项：.*$/, '下一项：EQ10 非等价改进记录') or abort 'next pointer not found'
File.write(task_path, task, mode: 'w:UTF-8')
puts JSON.generate(summary)
