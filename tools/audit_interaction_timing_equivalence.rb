#!/usr/bin/env ruby
# frozen_string_literal: true

require 'json'
require 'time'
require 'digest'

root = File.expand_path('..', __dir__)
preconditions_path = File.join(root, 'work/audit/precondition-equivalence.json')
effects_path = File.join(root, 'work/audit/side-effect-equivalence.json')
output_path = File.join(root, 'work/audit/interaction-timing-equivalence.json')
task_path = File.join(root, 'docs/Aoo-前后端全框架功能通信审计任务清单.md')
preconditions = JSON.parse(File.read(preconditions_path, encoding: 'UTF-8'))
effects = JSON.parse(File.read(effects_path, encoding: 'UTF-8'))

categories = {
  'buttonWindow' => /(?:button|click|interactable|enabled|candidate|operationWindow|按钮|候选)/i,
  'countdown' => /(?:countdown|timer|timeout|deadline|schedule|setTimeout|倒计时|超时)/i,
  'animation' => /(?:animation|animate|tween|transition|delayTime|sequence|动画)/i,
  'broadcast' => /(?:broadcast|push|emit|sendTo|publish|notify|广播|推送)/i,
  'ordering' => /(?:seq|sequence|order|before|after|then|await|priority|顺序|先后)/i
}.freeze

def collect(rows, categories)
  rows.each_with_object([]) do |row, result|
    excerpt = row.fetch('excerpt', '')
    kinds = categories.each_with_object([]) { |(kind, regex), found| found << kind if excerpt.match?(regex) }
    next if kinds.empty?
    normalized = excerpt.downcase.gsub(/\s+/, ' ').gsub(/\d+/, '#').strip
    result << {path: row['path'], line: row['line'], categories: kinds,
               expressionHash: Digest::SHA256.hexdigest(normalized), excerpt: excerpt}
  end
end

legacy_source = preconditions.fetch('legacyConditions') + effects.fetch('legacyEffects')
current_source = preconditions.fetch('currentConditions') + effects.fetch('currentEffects')
legacy = collect(legacy_source, categories).uniq { |row| [row[:path], row[:line], row[:expressionHash]] }
current = collect(current_source, categories).uniq { |row| [row[:path], row[:line], row[:expressionHash]] }
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
  legacyTimingSites: legacy.size, currentTimingSites: current.size,
  exactCandidates: legacy.count { |row| row[:candidateExactMatch] },
  unmatchedLegacy: legacy.count { |row| !row[:candidateExactMatch] }, byCategory: by_category
}
report = {
  schemaVersion: 1, generatedAt: Time.now.utc.iso8601,
  derivedFrom: [preconditions_path.delete_prefix(root + '/'), effects_path.delete_prefix(root + '/')],
  summary: summary, legacyTimingSites: legacy, currentTimingSites: current,
  limitations: ['Static candidates cannot prove rendered button windows, countdown values, animation completion or cross-client broadcast order; timestamped multi-client traces remain required.']
}
File.write(output_path, JSON.pretty_generate(report) + "\n", mode: 'w:UTF-8')

task = File.read(task_path, encoding: 'UTF-8')
detail = by_category.map { |name, values| "#{name}=#{values[:legacy]}/#{values[:current]}/未配#{values[:unmatchedLegacy]}" }.join('、')
row = "| EQ07 | 未完成 | 已建矩阵·未闭合 | 已按按钮/候选窗口、倒计时、动画、广播及顺序抽取旧新时序候选（#{detail}）；旧候选=#{summary[:legacyTimingSites]}、新候选=#{summary[:currentTimingSites]}、未匹配=#{summary[:unmatchedLegacy]}，尚无带时戳的真实多端交互/广播轨迹。 证据：work/audit/interaction-timing-equivalence.json；工具：tools/audit_interaction_timing_equivalence.rb |"
task.sub!(/^\| EQ07 \|.*$/, row) or abort 'EQ07 row not found'
task.sub!(/^下一项：.*$/, '下一项：EQ08 规则计算等价') or abort 'next pointer not found'
File.write(task_path, task, mode: 'w:UTF-8')
puts JSON.generate(summary)
