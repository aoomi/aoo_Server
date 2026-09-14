#!/usr/bin/env ruby
# frozen_string_literal: true

require 'json'
require 'time'

root = File.expand_path('..', __dir__)
baseline = JSON.parse(File.read(File.join(root, 'work/audit/audit-source-baseline.json'), encoding: 'UTF-8'))
mapping = JSON.parse(File.read(File.join(root, 'work/audit/database-runtime-mapping.json'), encoding: 'UTF-8')).fetch('mappings')
output_path = File.join(root, 'work/audit/regional-rule-placement.json')
task_path = File.join(root, 'docs/Aoo-前后端全框架功能通信审计任务清单.md')

games = mapping.reject { |game| game['category'] == 'INFRASTRUCTURE' }
codes = games.map { |game| game.fetch('code').downcase }.uniq
concerns = {
  'option' => /(?:Config|Option|RuleParam|RoomCfg|CreateRoom|Validator)/i,
  'restriction' => /(?:Restrict|Constraint|Limit|Forbidden|Required|Validate)/i,
  'flow' => /(?:Flow|Phase|StateMachine|RoundProcess|TurnProcess)/i,
  'scoring' => /(?:Score|Settlement|Calc|Fan|Point|Result)/i
}.freeze

files = []
baseline.fetch('files').each do |item|
  path = item.fetch('path')
  next unless path.start_with?(File.join(root, 'modules') + '/', File.join(root, 'server') + '/')
  next unless File.file?(path) && %w[.java .kt].include?(File.extname(path).downcase) && item.fetch('size') <= 2_000_000
  relative = path.delete_prefix(root + '/')
  lower = relative.downcase
  matched_codes = codes.select { |code| lower.match?(/(?<![a-z0-9_])#{Regexp.escape(code)}(?![a-z0-9_])/) }.first(20)
  next if matched_codes.empty?
  text = File.binread(path).force_encoding('UTF-8').scrub
  kinds = concerns.each_with_object([]) { |(kind, regex), found| found << kind if File.basename(path).match?(regex) || text.match?(regex) }
  next if kinds.empty?
  componentized = relative.match?(%r{/(?:rule|rules|component|components|variant|region|config|flow|scoring|settlement)/}i) ||
                  File.basename(path).match?(/(?:Rule|Component|Config|Flow|Scor|Settlement)/i)
  room_embedded = relative.match?(%r{/(?:room|setroom|roomset)/}i) || File.basename(path).match?(/Room/i)
  files << {path: relative, gameCodes: matched_codes, concerns: kinds, componentized: componentized,
            roomEmbedded: room_embedded, placementViolationCandidate: room_embedded && !componentized}
end

games_with_files = files.flat_map { |file| file[:gameCodes] }.uniq
games_with_all_concerns = games_with_files.count do |code|
  game_files = files.select { |file| file[:gameCodes].include?(code) }
  concerns.keys.all? { |kind| game_files.any? { |file| file[:concerns].include?(kind) && file[:componentized] } }
end
summary = {
  gameCount: games.size,
  gamesWithRegionalSourceEvidence: games_with_files.size,
  gamesWithoutRegionalSourceEvidence: games.size - games_with_files.size,
  gamesWithAllConcernComponents: games_with_all_concerns,
  concernFiles: files.size,
  componentizedFiles: files.count { |file| file[:componentized] },
  roomEmbeddedViolationCandidates: files.count { |file| file[:placementViolationCandidate] },
  concernCounts: concerns.keys.to_h { |kind| [kind, files.count { |file| file[:concerns].include?(kind) }] }
}
report = {
  schemaVersion: 1, generatedAt: Time.now.utc.iso8601, summary: summary,
  invariant: 'Regional options, restrictions, flow deltas and scoring deltas reside in versioned game components, not room transport/lifecycle classes.',
  files: files.sort_by { |file| file[:path] },
  limitations: ['Static naming/placement is candidate evidence; component registration, version binding and fixed-fact execution are required for closure.']
}
File.write(output_path, JSON.pretty_generate(report) + "\n", mode: 'w:UTF-8')

task = File.read(task_path, encoding: 'UTF-8')
row = "| CLASS08 | 未完成 | 未通过·已建归位门禁 | 已按地区选项/限制/流程/算分建立版本化组件归位清册；玩法=#{summary[:gameCount]}、有具体源证据=#{summary[:gamesWithRegionalSourceEvidence]}、无证据=#{summary[:gamesWithoutRegionalSourceEvidence]}、四类组件齐全=#{summary[:gamesWithAllConcernComponents]}、嵌入 Room 候选=#{summary[:roomEmbeddedViolationCandidates]}；未注册版本组件及未通过固定事实的项不得闭合。 证据：work/audit/regional-rule-placement.json；工具：tools/audit_regional_rule_placement.rb |"
task.sub!(/^\| CLASS08 \|.*$/, row) or abort 'CLASS08 row not found'
task.sub!(/^下一项：.*$/, '下一项：CLASS09 横切能力归位') or abort 'next pointer not found'
File.write(task_path, task, mode: 'w:UTF-8')
puts JSON.generate(summary)
