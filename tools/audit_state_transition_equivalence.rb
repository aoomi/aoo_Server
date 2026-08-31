#!/usr/bin/env ruby
# frozen_string_literal: true

require 'json'
require 'time'
require 'digest'

root = File.expand_path('..', __dir__)
source_path = File.join(root, 'work/audit/precondition-equivalence.json')
output_path = File.join(root, 'work/audit/state-transition-equivalence.json')
task_path = File.join(root, 'docs/Aoo-前后端全框架功能通信审计任务清单.md')
source = JSON.parse(File.read(source_path, encoding: 'UTF-8'))

scenarios = {
  'normal' => /(?:accept|success|complete|finish|ready|start|return\s+true)/i,
  'reject' => /(?:throw|reject|deny|forbid|illegal|error|fail|return\s+false)/i,
  'timeout' => /(?:timeout|expired|deadline|elapsed|超时)/i,
  'cancel' => /(?:cancel|abort|dismiss|取消)/i,
  'disconnect' => /(?:disconnect|reconnect|offline|heartbeat|close\s*\(|断线|重连)/i,
  'duplicate' => /(?:duplicate|idempot|requestId|already|replay|seq|重复|重放)/i
}.freeze

def classify(rows, scenarios)
  rows.each_with_object([]) do |row, result|
    excerpt = row.fetch('excerpt', '')
    kinds = scenarios.each_with_object([]) { |(kind, regex), found| found << kind if excerpt.match?(regex) }
    next if kinds.empty?
    normalized = excerpt.downcase.gsub(/\s+/, ' ').gsub(/\d+/, '#').strip
    result << {
      path: row['path'], line: row['line'], scenarios: kinds,
      expressionHash: Digest::SHA256.hexdigest(normalized), excerpt: excerpt
    }
  end
end

legacy = classify(source.fetch('legacyConditions'), scenarios)
current = classify(source.fetch('currentConditions'), scenarios)
current_hashes = current.map { |row| row[:expressionHash] }.to_h { |hash| [hash, true] }
legacy.each { |row| row[:candidateExactMatch] = current_hashes.key?(row[:expressionHash]) }
by_scenario = scenarios.keys.to_h do |scenario|
  old_rows = legacy.select { |row| row[:scenarios].include?(scenario) }
  new_rows = current.select { |row| row[:scenarios].include?(scenario) }
  [scenario, {legacy: old_rows.size, current: new_rows.size,
              exactCandidates: old_rows.count { |row| row[:candidateExactMatch] },
              unmatchedLegacy: old_rows.count { |row| !row[:candidateExactMatch] }}]
end
summary = {
  legacyTransitions: legacy.size,
  currentTransitions: current.size,
  exactCandidates: legacy.count { |row| row[:candidateExactMatch] },
  unmatchedLegacy: legacy.count { |row| !row[:candidateExactMatch] },
  byScenario: by_scenario
}
report = {
  schemaVersion: 1,
  generatedAt: Time.now.utc.iso8601,
  derivedFrom: source_path.delete_prefix(root + '/'),
  summary: summary,
  legacyTransitions: legacy,
  currentTransitions: current,
  limitations: ['Static transition candidates do not prove before/after authoritative state; differential scenario execution remains mandatory.']
}
File.write(output_path, JSON.pretty_generate(report) + "\n", mode: 'w:UTF-8')

task = File.read(task_path, encoding: 'UTF-8')
detail = by_scenario.map { |name, values| "#{name}=#{values[:legacy]}/#{values[:current]}/未配#{values[:unmatchedLegacy]}" }.join('、')
row = "| EQ03 | 未完成 | 已建矩阵·未闭合 | 已按正常/拒绝/超时/取消/断线/重复操作抽取旧新状态转换候选（#{detail}）；旧候选=#{summary[:legacyTransitions]}、新候选=#{summary[:currentTransitions]}、未匹配=#{summary[:unmatchedLegacy]}，尚无权威前后状态差分执行证据。 证据：work/audit/state-transition-equivalence.json；工具：tools/audit_state_transition_equivalence.rb |"
task.sub!(/^\| EQ03 \|.*$/, row) or abort 'EQ03 row not found'
task.sub!(/^下一项：.*$/, '下一项：EQ04 数据副作用等价') or abort 'next pointer not found'
File.write(task_path, task, mode: 'w:UTF-8')
puts JSON.generate(summary)
