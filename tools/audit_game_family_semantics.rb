#!/usr/bin/env ruby
# frozen_string_literal: true

require 'json'
require 'time'

root = File.expand_path('..', __dir__)
mapping = JSON.parse(File.read(File.join(root, 'work/audit/database-runtime-mapping.json'), encoding: 'UTF-8')).fetch('mappings')
baseline = JSON.parse(File.read(File.join(root, 'work/audit/audit-source-baseline.json'), encoding: 'UTF-8'))
output_path = File.join(root, 'work/audit/game-family-semantic-evidence.json')
task_path = File.join(root, 'docs/Aoo-前后端全框架功能通信审计任务清单.md')

semantic_patterns = {
  'cardSet' => /(?:Card|Tile|Pai|Poker|Mahjong|Deck|Wall)/i,
  'actions' => /(?:Action|Operation|Command|Play|Chi|Peng|Gang|Hu|Pass|Bet)/i,
  'stateMachine' => /(?:State|FSM|Phase|Flow|Round|Turn|Session)/i,
  'scoring' => /(?:Score|Settlement|Result|Calc|Fan|Point|Billing)/i
}.freeze

source_paths = baseline.fetch('files').map { |item| item.fetch('path') }.select do |path|
  path.start_with?(File.join(root, 'modules') + '/', File.join(root, 'server') + '/') &&
    %w[.java .kt].include?(File.extname(path).downcase) && !path.include?('/target/')
end

rows = mapping.reject { |game| game['category'] == 'INFRASTRUCTURE' }.map do |game|
  code = game.fetch('code').downcase
  token = /(?<![a-z0-9_])#{Regexp.escape(code)}(?![a-z0-9_])/i
  direct_paths = source_paths.select { |path| path.downcase.match?(token) }
  evidence = semantic_patterns.to_h do |kind, regex|
    [kind, direct_paths.select { |path| File.basename(path).match?(regex) }.map { |path| path.delete_prefix(root + '/') }.first(50)]
  end
  semantic_complete = semantic_patterns.keys.all? { |kind| !evidence.fetch(kind).empty? }
  {
    code: code, category: game['category'], declaredFamily: game['family'],
    directSourceFiles: direct_paths.size, semanticEvidence: evidence,
    semanticComplete: semantic_complete,
    nameOnlyCandidate: !game['family'].to_s.empty? && !semantic_complete
  }
end

families = rows.group_by { |row| row[:declaredFamily] }.map do |family, games|
  {family: family, gameCount: games.size, semanticCompleteGames: games.count { |game| game[:semanticComplete] },
   nameOnlyCandidates: games.count { |game| game[:nameOnlyCandidate] }, games: games.map { |game| game[:code] }}
end.sort_by { |family| family[:family].to_s }
summary = {
  gameCount: rows.size, familyCount: families.size,
  semanticCompleteGames: rows.count { |row| row[:semanticComplete] },
  nameOnlyCandidates: rows.count { |row| row[:nameOnlyCandidate] },
  familiesWithNoCompleteGame: families.count { |family| family[:semanticCompleteGames].zero? },
  evidenceCounts: semantic_patterns.keys.to_h { |kind| [kind, rows.count { |row| !row[:semanticEvidence].fetch(kind).empty? }] }
}
report = {
  schemaVersion: 1, generatedAt: Time.now.utc.iso8601, summary: summary,
  invariant: 'A family assignment requires evidence for card/tile set, action set, FSM/flow and scoring semantics.',
  families: families, games: rows,
  limitations: ['Filename/path evidence is conservative; semantic parser and fixed-fact differential tests are required to close each family assignment.']
}
File.write(output_path, JSON.pretty_generate(report) + "\n", mode: 'w:UTF-8')

task = File.read(task_path, encoding: 'UTF-8')
row = "| CLASS02 | 未完成 | 未通过·已建语义门禁 | 已建玩法族的牌集/动作集/状态机/算分四类语义证据门禁；玩法=#{summary[:gameCount]}、玩法族=#{summary[:familyCount]}、四类证据完整玩法=#{summary[:semanticCompleteGames]}、仅名称/元数据候选=#{summary[:nameOnlyCandidates]}、无任一完整玩法的族=#{summary[:familiesWithNoCompleteGame]}，未证明项不得按名称定族。 证据：work/audit/game-family-semantic-evidence.json；工具：tools/audit_game_family_semantics.rb |"
task.sub!(/^\| CLASS02 \|.*$/, row) or abort 'CLASS02 row not found'
task.sub!(/^下一项：.*$/, '下一项：CLASS03 地区变体判定') or abort 'next pointer not found'
File.write(task_path, task, mode: 'w:UTF-8')
puts JSON.generate(summary)
