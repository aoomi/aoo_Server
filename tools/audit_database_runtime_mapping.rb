#!/usr/bin/env ruby
# frozen_string_literal: true

require 'json'
require 'time'
require 'fileutils'

root = File.expand_path('..', __dir__)
catalog_path = File.join(root, 'work/backend-baseline-528.tsv')
game_list_path = File.join(root, 'server/LegacyCommon/conf/jsonData/gamelist.json')
output_path = File.join(root, 'work/audit/database-runtime-mapping.json')
task_path = File.join(root, 'docs/Aoo-前后端全框架功能通信审计任务清单.md')

abort "missing catalog: #{catalog_path}" unless File.file?(catalog_path)
abort "missing game list: #{game_list_path}" unless File.file?(game_list_path)

def tsv_rows(path)
  lines = File.readlines(path, encoding: 'UTF-8', chomp: true)
  header = lines.shift.split("\t", -1)
  lines.reject(&:empty?).map { |line| header.zip(line.split("\t", -1)).to_h }
end

catalog = tsv_rows(catalog_path)
game_list = JSON.parse(File.read(game_list_path, encoding: 'UTF-8')).values
db_by_code = game_list.group_by { |row| row.fetch('gameName', '').downcase }
codes = catalog.map { |row| row.fetch('module').downcase }.uniq.sort
code_set = codes.to_h { |code| [code, true] }

scan_roots = %w[modules server].map { |name| File.join(root, name) }
extensions = %w[.java .kt .xml .json .yml .yaml .properties .sql]
ignored_parts = %w[target build .gradle node_modules generated-sources]
evidence = Hash.new { |hash, key| hash[key] = [] }
generic_entries = []

scan_roots.each do |scan_root|
  Dir.glob(File.join(scan_root, '**', '*'), File::FNM_DOTMATCH).sort.each do |path|
    next unless File.file?(path)
    next unless extensions.include?(File.extname(path).downcase)
    relative = path.delete_prefix(root + '/').encode('UTF-8', invalid: :replace, undef: :replace, replace: '')
    next if relative.split('/').any? { |part| ignored_parts.include?(part) }
    next if File.size(path) > 2_000_000

    text = File.binread(path).force_encoding('UTF-8').scrub
    tokens = (relative.downcase.scan(/[a-z][a-z0-9_]{1,31}/) +
              text.downcase.scan(/["']([a-z][a-z0-9_]{1,31})["']/).flatten).uniq
    (tokens & codes).each do |code|
      evidence[code] << relative unless evidence[code].include?(relative)
    end
    if relative.match?(/(?:Provider|Router|Handler|GameCatalogLoader|RegionalGameCatalog)\.(?:java|kt)$/)
      generic_entries << relative
    end
  rescue Errno::ENOENT, Errno::EACCES
    next
  end
end

mappings = catalog.map do |row|
  code = row.fetch('module').downcase
  db_records = db_by_code.fetch(code, [])
  paths = evidence.fetch(code, []).sort
  runtime_paths = paths.select { |path| path.match?(/(?:Provider|Router|Handler|Bootstrap|Game|Room|Config)/i) }
  {
    gameId: db_records.first&.fetch('id', nil),
    code: code,
    category: row['category'],
    family: row['family'],
    region: db_records.first&.fetch('region', nil),
    enabled: db_records.first&.fetch('isOpen', nil),
    databaseRecords: db_records.map { |record| record.slice('id', 'gameName', 'gameType', 'region', 'isOpen') },
    sourceEvidence: paths.first(20),
    runtimeEntryEvidence: runtime_paths.first(20),
    databaseMapped: !db_records.empty?,
    runtimeMapped: !runtime_paths.empty?
  }
end

summary = {
  catalogCount: mappings.size,
  databaseMapped: mappings.count { |row| row[:databaseMapped] },
  runtimeMapped: mappings.count { |row| row[:runtimeMapped] },
  fullyMapped: mappings.count { |row| row[:databaseMapped] && row[:runtimeMapped] },
  missingDatabaseRecord: mappings.count { |row| !row[:databaseMapped] },
  missingRuntimeEntry: mappings.count { |row| !row[:runtimeMapped] }
}

report = {
  schemaVersion: 1,
  generatedAt: Time.now.utc.iso8601,
  catalogSource: catalog_path.delete_prefix(root + '/'),
  databaseSource: game_list_path.delete_prefix(root + '/'),
  scanRoots: scan_roots.map { |path| path.delete_prefix(root + '/') },
  summary: summary,
  genericRuntimeEntries: generic_entries.uniq.sort,
  mappings: mappings
}
FileUtils.mkdir_p(File.dirname(output_path)) unless Dir.exist?(File.dirname(output_path))
File.write(output_path, JSON.pretty_generate(report) + "\n", mode: 'w:UTF-8')

task = File.read(task_path, encoding: 'UTF-8')
result = "已建立 #{summary[:catalogCount]} 项数据库记录到运行入口的可重复映射；数据库已映射 #{summary[:databaseMapped]} 项，运行入口已映射 #{summary[:runtimeMapped]} 项，双向闭合 #{summary[:fullyMapped]} 项；未闭合项保持未完成并进入后续整改。 证据：work/audit/database-runtime-mapping.json"
replacement = "| CALL05 | 未完成 | 已审计·待补齐 | #{result} |"
task.sub!(/^\| CALL05 \|.*$/, replacement) or abort 'CALL05 row not found'
task.sub!(/^\u4e0b\u4e00\u9879：.*$/, '下一项：CALL06 外部脚本与本地工具调用清单') or abort 'next pointer not found'
File.write(task_path, task, mode: 'w:UTF-8')

puts JSON.generate(summary)
