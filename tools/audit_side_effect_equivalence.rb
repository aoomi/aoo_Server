#!/usr/bin/env ruby
# frozen_string_literal: true

require 'json'
require 'time'
require 'digest'

root = File.expand_path('..', __dir__)
legacy_roots = [
  File.expand_path('../../Test/QH_DFMJ/client-unified-3.8.6/assets', root),
  File.expand_path('../../Test/情怀后端原代码/Server_game_split', root)
].freeze
current_roots = [File.join(root, 'modules'), File.join(root, 'server'), File.expand_path('../Client/assets', root)].freeze
output_path = File.join(root, 'work/audit/side-effect-equivalence.json')
task_path = File.join(root, 'docs/Aoo-前后端全框架功能通信审计任务清单.md')

categories = {
  'database' => /(?:\bINSERT\b|\bUPDATE\b|\bDELETE\b|\.save\s*\(|\.persist\s*\(|\.executeUpdate\s*\(|Jdbc|Dao\b|Repository\b)/i,
  'cache' => /(?:Redis|Jedis|Redisson|Cache|putIfAbsent|expire|hset|setex)/i,
  'ledger' => /(?:ledger|billing|wallet|balance|currency|diamond|gold|roomCard|transaction|流水|账务)/i,
  'event' => /(?:publish|broadcast|emit\s*\(|sendTo|push|Producer|EventBus|MessageQueue)/i,
  'record' => /(?:gameRecord|battleRecord|matchRecord|history|recordService|战绩)/i,
  'replay' => /(?:replay|playBack|recordFrame|eventLog|回放)/i
}.freeze

def files(roots)
  roots.flat_map do |source_root|
    next [] unless Dir.exist?(source_root)
    Dir.glob(File.join(source_root, '**', '*'), File::FNM_DOTMATCH).select do |path|
      next false unless File.file?(path)
      relative = path.delete_prefix(source_root + '/')
      next false if relative.split('/').any? { |part| %w[bin target build library temp node_modules .git logs].include?(part) }
      %w[.java .kt .ts .js .mjs .cjs .xml .sql].include?(File.extname(path).downcase) && File.size(path) <= 2_000_000
    end
  end.uniq
end

def collect(roots, categories)
  rows = []
  files(roots).sort.each do |path|
    text = File.binread(path).force_encoding('UTF-8').scrub
    text.lines.each_with_index do |line, index|
      kinds = categories.each_with_object([]) { |(kind, regex), found| found << kind if line.match?(regex) }
      next if kinds.empty?
      normalized = line.downcase.gsub(/\s+/, ' ').gsub(/\d+/, '#').strip[0, 500]
      rows << {path: path, line: index + 1, categories: kinds,
               expressionHash: Digest::SHA256.hexdigest(normalized), excerpt: line.strip[0, 500]}
    end
  end
  rows
end

legacy = collect(legacy_roots, categories)
current = collect(current_roots, categories)
current_hashes = current.map { |row| row[:expressionHash] }.to_h { |hash| [hash, true] }
legacy.each { |row| row[:candidateExactMatch] = current_hashes.key?(row[:expressionHash]) }
by_category = categories.keys.to_h do |category|
  old_rows = legacy.select { |row| row[:categories].include?(category) }
  new_rows = current.select { |row| row[:categories].include?(category) }
  [category, {legacy: old_rows.size, current: new_rows.size,
              exactCandidates: old_rows.count { |row| row[:candidateExactMatch] },
              unmatchedLegacy: old_rows.count { |row| !row[:candidateExactMatch] }}]
end
summary = {
  legacyEffects: legacy.size, currentEffects: current.size,
  exactCandidates: legacy.count { |row| row[:candidateExactMatch] },
  unmatchedLegacy: legacy.count { |row| !row[:candidateExactMatch] }, byCategory: by_category
}
report = {
  schemaVersion: 1, generatedAt: Time.now.utc.iso8601,
  legacyRoots: legacy_roots, currentRoots: current_roots,
  summary: summary, legacyEffects: legacy, currentEffects: current,
  limitations: ['Static call candidates cannot prove committed table/cache/ledger/event/record/replay outcomes; dual-run state snapshots remain required.']
}
File.write(output_path, JSON.pretty_generate(report) + "\n", mode: 'w:UTF-8')

task = File.read(task_path, encoding: 'UTF-8')
detail = by_category.map { |name, values| "#{name}=#{values[:legacy]}/#{values[:current]}/未配#{values[:unmatchedLegacy]}" }.join('、')
row = "| EQ04 | 未完成 | 已建矩阵·未闭合 | 已按表/缓存/流水/事件/战绩/回放抽取旧新数据副作用候选（#{detail}）；旧候选=#{summary[:legacyEffects]}、新候选=#{summary[:currentEffects]}、未匹配=#{summary[:unmatchedLegacy]}，尚无旧新双跑提交后状态快照。 证据：work/audit/side-effect-equivalence.json；工具：tools/audit_side_effect_equivalence.rb |"
task.sub!(/^\| EQ04 \|.*$/, row) or abort 'EQ04 row not found'
task.sub!(/^下一项：.*$/, '下一项：EQ05 多玩家可见结果等价') or abort 'next pointer not found'
File.write(task_path, task, mode: 'w:UTF-8')
puts JSON.generate(summary)
