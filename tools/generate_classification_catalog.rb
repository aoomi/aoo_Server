#!/usr/bin/env ruby
# frozen_string_literal: true

require 'json'
require 'time'
require 'fileutils'

root = File.expand_path('..', __dir__)
mapping = JSON.parse(File.read(File.join(root, 'work/audit/database-runtime-mapping.json'), encoding: 'UTF-8')).fetch('mappings')
baseline = JSON.parse(File.read(File.join(root, 'work/audit/audit-source-baseline.json'), encoding: 'UTF-8'))
output_dir = File.join(root, 'work/generated/classification')
task_path = File.join(root, 'docs/Aoo-前后端全框架功能通信审计任务清单.md')
FileUtils.mkdir_p(output_dir)

providers = []
baseline.fetch('files').each do |item|
  path = item.fetch('path')
  next unless path.start_with?(File.join(root, 'server') + '/') && path.end_with?('GameProvider.java')
  next unless File.file?(path) && item.fetch('size') <= 2_000_000
  text = File.binread(path).force_encoding('UTF-8').scrub
  text.scan(/new\s+GameDescriptor\s*\(\s*(\d+)\s*,\s*"([^"]+)"\s*,\s*"([^"]+)"\s*,\s*GameCategory\.([A-Z_]+)\s*,\s*([^,\n]+)\s*,\s*RegionScope\.([A-Z_]+)/m).each do |id, code, display, category, family_expr, scope|
    family = family_expr[/"([^"]+)"/, 1] || family_expr.strip
    providers << {gameId: id.to_i, code: code.downcase, displayName: display, category: category,
                  family: family, regionScope: scope, sourceType: 'PROVIDER', source: path.delete_prefix(root + '/')}
  end
end
provider_by_code = providers.to_h { |row| [row[:code], row] }

rows = mapping.reject { |game| game['category'] == 'INFRASTRUCTURE' }.map do |game|
  provider_by_code.fetch(game.fetch('code')) do
    {
      gameId: game['gameId'], code: game.fetch('code'), displayName: game.fetch('code'),
      category: game['category'], family: game['family'],
      regionScope: game['region'].to_s == 'all' ? 'NATIONAL' : 'REGIONAL',
      region: game['region'], sourceType: 'CATALOG_METADATA', source: 'work/backend-baseline-528.tsv'
    }
  end
end
provider_only = providers.reject { |provider| rows.any? { |row| row[:code] == provider[:code] } }
rows.concat(provider_only)
rows.sort_by! { |row| [row[:category].to_s, row[:family].to_s, row[:code]] }

summary = {
  rows: rows.size,
  providerRows: rows.count { |row| row[:sourceType] == 'PROVIDER' },
  metadataFallbackRows: rows.count { |row| row[:sourceType] == 'CATALOG_METADATA' },
  providerOnlyRows: provider_only.size,
  duplicateCodes: rows.group_by { |row| row[:code] }.count { |_code, grouped| grouped.size > 1 },
  duplicateIds: rows.reject { |row| row[:gameId].nil? }.group_by { |row| row[:gameId] }.count { |_id, grouped| grouped.size > 1 },
  categoryCounts: rows.group_by { |row| row[:category] }.transform_values(&:size),
  familyCounts: rows.group_by { |row| row[:family] }.transform_values(&:size)
}
json_report = {schemaVersion: 1, generatedAt: Time.now.utc.iso8601, summary: summary, rows: rows}
File.write(File.join(output_dir, 'game-classification.json'), JSON.pretty_generate(json_report) + "\n", mode: 'w:UTF-8')

markdown = +"# Aoo 游戏分类目录\n\n"
markdown << "> 由 Provider 元数据优先生成；尚未原生化的玩法标记为 `CATALOG_METADATA`，不视为可运行实现。\n\n"
markdown << "- 生成时间：#{json_report[:generatedAt]}\n- 总项：#{summary[:rows]}\n- Provider：#{summary[:providerRows]}\n- 元数据回退：#{summary[:metadataFallbackRows]}\n\n"
markdown << "| ID | code | 分类 | 玩法族 | 地区 | 来源类型 | 来源 |\n|---:|---|---|---|---|---|---|\n"
rows.each do |row|
  markdown << "| #{row[:gameId]} | #{row[:code]} | #{row[:category]} | #{row[:family]} | #{row[:region] || row[:regionScope]} | #{row[:sourceType]} | #{row[:source]} |\n"
end
File.write(File.join(output_dir, 'game-classification.md'), markdown, mode: 'w:UTF-8')

sql = +"-- Generated classification seed candidate. Review schema migration before execution.\n"
sql << "-- generated_at=#{json_report[:generatedAt]}\n"
rows.each do |row|
  values = [row[:gameId], row[:code], row[:category], row[:family], row[:region], row[:sourceType]].map do |value|
    value.nil? ? 'NULL' : "'#{value.to_s.gsub("'", "''")}'"
  end
  sql << "INSERT INTO aoo_game_classification (game_id, game_code, category, family_code, region_code, source_type) VALUES (#{values.join(', ')});\n"
end
File.write(File.join(output_dir, 'game-classification.seed.sql'), sql, mode: 'w:UTF-8')

task = File.read(task_path, encoding: 'UTF-8')
row = "| CLASS11 | 未完成 | 已建自动生成·待 Provider 收敛 | 已从可解析 Provider 元数据优先生成 JSON/Markdown/数据库 seed 候选；总项=#{summary[:rows]}、Provider=#{summary[:providerRows]}、元数据回退=#{summary[:metadataFallbackRows]}、Provider 仅有=#{summary[:providerOnlyRows]}、重复 code=#{summary[:duplicateCodes]}、重复 ID=#{summary[:duplicateIds]}；所有回退项原生 Provider 化且 seed 经真实 schema 迁移验证前不得闭合。 证据：work/generated/classification/ |"
task.sub!(/^\| CLASS11 \|.*$/, row) or abort 'CLASS11 row not found'
task.sub!(/^下一项：.*$/, '下一项：CLASS12 新玩法演练') or abort 'next pointer not found'
File.write(task_path, task, mode: 'w:UTF-8')
puts JSON.generate(summary)
