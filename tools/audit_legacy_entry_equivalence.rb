#!/usr/bin/env ruby
# frozen_string_literal: true

require 'json'
require 'time'

root = File.expand_path('..', __dir__)
legacy_client = ENV.fetch('AOO_LEGACY_CLIENT_ASSETS', File.expand_path('../../Test/QH_DFMJ/client-unified-3.8.6/assets', root))
new_client = File.expand_path('../Client/assets', root)
new_admin = File.expand_path('../Admin/src', root)
output_path = File.join(root, 'work/audit/legacy-entry-equivalence.json')
task_path = File.join(root, 'docs/Aoo-前后端全框架功能通信审计任务清单.md')
abort "missing legacy client proxy: #{legacy_client}" unless Dir.exist?(legacy_client)

def normalize(value)
  value.downcase.gsub(/(?:legacy|compatibility|native|runtime|controller|bootstrap|screen|view|panel|form|prefab|scene)/, '')
       .gsub(/[^a-z0-9\p{Han}]+/u, '')
end

def inventory(base, include_admin: false)
  entries = []
  Dir.glob(File.join(base, '**', '*'), File::FNM_DOTMATCH).sort.each do |path|
    next unless File.file?(path)
    relative = path.delete_prefix(base + '/')
    next if relative.split('/').any? { |part| %w[node_modules library temp build target .git].include?(part) }
    ext = File.extname(path).downcase
    if %w[.scene .prefab].include?(ext)
      entries << {kind: ext == '.scene' ? 'page' : 'prefab', key: File.basename(path, ext), path: relative}
    end
    next unless %w[.ts .js .vue .json].include?(ext) && File.size(path) <= 2_000_000
    text = File.binread(path).force_encoding('UTF-8').scrub
    text.lines.each_with_index do |line, index|
      if line.match?(/(?:Button\.EventType\.CLICK|\.on\s*\(\s*['"]click|clickEvents|@click\s*=)/)
        name = line[/['"]([A-Za-z0-9_.:-]{2,100})['"]/, 1] || File.basename(path, ext)
        entries << {kind: 'button', key: name, path: relative, line: index + 1}
      end
      line.scan(/['"]((?:common|mahjong|poker|account|hall|club|union|room|game|record|replay)\.[a-z0-9_.-]+)['"]/i).each do |match|
        entries << {kind: 'protocol', key: match.first, path: relative, line: index + 1}
      end
      if include_admin
        line.scan(/(?:path\s*:\s*|@RequestMapping\s*\(\s*)['"]([^'"]+)['"]/).each do |match|
          entries << {kind: 'admin', key: match.first, path: relative, line: index + 1}
        end
      end
    end
  end
  entries.uniq { |entry| [entry[:kind], entry[:key], entry[:path], entry[:line]] }
end

legacy = inventory(legacy_client)
current = inventory(new_client) + inventory(new_admin, include_admin: true)
current_index = current.group_by { |entry| [entry[:kind], normalize(entry[:key])] }
matrix = legacy.map do |entry|
  matches = current_index.fetch([entry[:kind], normalize(entry[:key])], [])
  {kind: entry[:kind], legacyKey: entry[:key], legacyPath: entry[:path], legacyLine: entry[:line],
   decision: matches.empty? ? 'UNMAPPED' : 'MAPPED', targets: matches.first(20)}
end
summary = {
  legacyEntries: matrix.size,
  mapped: matrix.count { |row| row[:decision] == 'MAPPED' },
  unmapped: matrix.count { |row| row[:decision] == 'UNMAPPED' },
  byKind: matrix.group_by { |row| row[:kind] }.transform_values do |rows|
    {total: rows.size, mapped: rows.count { |row| row[:decision] == 'MAPPED' }, unmapped: rows.count { |row| row[:decision] == 'UNMAPPED' }}
  end
}
report = {
  schemaVersion: 1,
  generatedAt: Time.now.utc.iso8601,
  legacyProxySource: legacy_client,
  currentSources: [new_client, new_admin],
  summary: summary,
  matrix: matrix,
  limitations: [
    'The available consolidated Creator 3.8.6 migration is used as a 2.22-derived proxy; direct immutable 2.22 client provenance remains required.',
    'Semantic decisions require explicit MAPPED/DEPRECATED/REPLACED review; normalized-name matches are candidate evidence only.'
  ]
}
File.write(output_path, JSON.pretty_generate(report) + "\n", mode: 'w:UTF-8')

task = File.read(task_path, encoding: 'UTF-8')
row = "| EQ01 | 未完成 | 已建矩阵·未闭合 | 已从 2.22 派生的统一客户端代理源盘点页面、Prefab、按钮、协议及后台入口并生成候选映射矩阵；总项=#{summary[:legacyEntries]}、候选已映射=#{summary[:mapped]}、未映射=#{summary[:unmapped]}，且缺直接不可变 2.22 客户端源证明，不得闭合。 证据：work/audit/legacy-entry-equivalence.json；工具：tools/audit_legacy_entry_equivalence.rb |"
task.sub!(/^\| EQ01 \|.*$/, row) or abort 'EQ01 row not found'
task.sub!(/^下一项：.*$/, '下一项：EQ02 前置条件等价') or abort 'next pointer not found'
File.write(task_path, task, mode: 'w:UTF-8')
puts JSON.generate(summary)
