#!/usr/bin/env ruby
# frozen_string_literal: true

require 'json'
require 'time'
require 'digest'

root = File.expand_path('..', __dir__)
preconditions_path = File.join(root, 'work/audit/precondition-equivalence.json')
effects_path = File.join(root, 'work/audit/side-effect-equivalence.json')
output_path = File.join(root, 'work/audit/player-visibility-equivalence.json')
task_path = File.join(root, 'docs/Aoo-前后端全框架功能通信审计任务清单.md')
preconditions = JSON.parse(File.read(preconditions_path, encoding: 'UTF-8'))
effects = JSON.parse(File.read(effects_path, encoding: 'UTF-8'))

actors = {
  'initiator' => /(?:self|owner|creator|currentUser|authenticatedUser|mySeat|发起者|自己)/i,
  'otherSeat' => /(?:other|opponent|nextPlayer|seatId|dPos|posID|其他|对手|上家|下家)/i,
  'spectator' => /(?:spectator|observer|watcher|audience|观战|旁观)/i,
  'reconnect' => /(?:reconnect|resume|restore|snapshot|lastSeq|断线|重连|恢复)/i
}.freeze
sensitive = /(?:hand|cards|tiles|wall|deck|seed|private|hidden|暗牌|手牌|牌墙|随机种子)/i
view = /(?:viewFor|visible|dto|snapshot|broadcast|push|serialize|response|body|view|show|hide)/i

def collect(rows, actors, sensitive, view)
  rows.each_with_object([]) do |row, result|
    excerpt = row.fetch('excerpt', '')
    next unless excerpt.match?(view) || excerpt.match?(sensitive)
    roles = actors.each_with_object([]) { |(actor, regex), found| found << actor if excerpt.match?(regex) }
    roles << 'unclassified' if roles.empty?
    normalized = excerpt.downcase.gsub(/\s+/, ' ').gsub(/\d+/, '#').strip
    result << {
      path: row['path'], line: row['line'], actors: roles,
      sensitive: excerpt.match?(sensitive), expressionHash: Digest::SHA256.hexdigest(normalized), excerpt: excerpt
    }
  end
end

legacy_source = preconditions.fetch('legacyConditions') + effects.fetch('legacyEffects')
current_source = preconditions.fetch('currentConditions') + effects.fetch('currentEffects')
legacy = collect(legacy_source, actors, sensitive, view).uniq { |row| [row[:path], row[:line], row[:expressionHash]] }
current = collect(current_source, actors, sensitive, view).uniq { |row| [row[:path], row[:line], row[:expressionHash]] }
current_hashes = current.map { |row| row[:expressionHash] }.to_h { |hash| [hash, true] }
legacy.each { |row| row[:candidateExactMatch] = current_hashes.key?(row[:expressionHash]) }
by_actor = (actors.keys + ['unclassified']).to_h do |actor|
  old_rows = legacy.select { |row| row[:actors].include?(actor) }
  new_rows = current.select { |row| row[:actors].include?(actor) }
  [actor, {legacy: old_rows.size, current: new_rows.size,
           sensitiveLegacy: old_rows.count { |row| row[:sensitive] },
           sensitiveCurrent: new_rows.count { |row| row[:sensitive] },
           unmatchedLegacy: old_rows.count { |row| !row[:candidateExactMatch] }}]
end
summary = {
  legacyCandidates: legacy.size, currentCandidates: current.size,
  sensitiveLegacy: legacy.count { |row| row[:sensitive] },
  sensitiveCurrent: current.count { |row| row[:sensitive] },
  exactCandidates: legacy.count { |row| row[:candidateExactMatch] },
  unmatchedLegacy: legacy.count { |row| !row[:candidateExactMatch] }, byActor: by_actor
}
report = {
  schemaVersion: 1, generatedAt: Time.now.utc.iso8601,
  derivedFrom: [preconditions_path.delete_prefix(root + '/'), effects_path.delete_prefix(root + '/')],
  summary: summary, legacyCandidates: legacy, currentCandidates: current,
  limitations: ['Static view candidates cannot prove per-connection payload redaction; multi-seat, spectator and reconnect packet captures remain required.']
}
File.write(output_path, JSON.pretty_generate(report) + "\n", mode: 'w:UTF-8')

task = File.read(task_path, encoding: 'UTF-8')
detail = by_actor.map { |name, values| "#{name}=#{values[:legacy]}/#{values[:current]}/未配#{values[:unmatchedLegacy]}" }.join('、')
row = "| EQ05 | 未完成 | 已建矩阵·未闭合 | 已按发起者/其他座位/观战者/重连者抽取旧新可见结果及敏感牌面候选（#{detail}）；旧候选=#{summary[:legacyCandidates]}、新候选=#{summary[:currentCandidates]}、未匹配=#{summary[:unmatchedLegacy]}，尚无逐连接抓包与暗牌裁剪证明。 证据：work/audit/player-visibility-equivalence.json；工具：tools/audit_visibility_equivalence.rb |"
task.sub!(/^\| EQ05 \|.*$/, row) or abort 'EQ05 row not found'
task.sub!(/^下一项：.*$/, '下一项：EQ06 错误行为等价') or abort 'next pointer not found'
File.write(task_path, task, mode: 'w:UTF-8')
puts JSON.generate(summary)
