#!/usr/bin/env ruby
# frozen_string_literal: true

require 'json'
require 'time'

root = File.expand_path('..', __dir__)
mapping = JSON.parse(File.read(File.join(root, 'work/audit/database-runtime-mapping.json'), encoding: 'UTF-8')).fetch('mappings')
game_list = JSON.parse(File.read(File.join(root, 'server/LegacyCommon/conf/jsonData/gamelist.json'), encoding: 'UTF-8')).values
enum_path = File.join(root, 'server/GameSPI/src/main/java/com/aoo/bcg/gamespi/GameCategory.java')
enum_text = File.read(enum_path, encoding: 'UTF-8')
output_path = File.join(root, 'work/audit/primary-game-category-audit.json')
task_path = File.join(root, 'docs/Aoo-前后端全框架功能通信审计任务清单.md')

required = %w[MAHJONG POKER LONG_CARD WORD_CARD CHESS DICE_BETTING OTHER].freeze
declared = enum_text.scan(/^\s*([A-Z][A-Z0-9_]*)\s*,?\s*$/).flatten
by_code = game_list.group_by { |row| row.fetch('gameName', '').downcase }
heuristics = {
  'CHESS' => /(?:chess|xiangqi|gomoku|wzq|weiqi|junqi|douqi|xq)/i,
  'DICE_BETTING' => /(?:dice|sicbo|shaibao|shai|baccarat|baijiale|roulette|lunpan)/i
}.freeze

rows = mapping.map do |game|
  code = game.fetch('code')
  metadata = by_code.fetch(code, [])
  text = ([code, game['family'], game['category']] + metadata.flat_map { |row| row.values.map(&:to_s) }).join(' ')
  candidates = heuristics.each_with_object([]) { |(category, regex), found| found << category if text.match?(regex) }
  {
    code: code, declaredCategory: game['category'], family: game['family'],
    sourceGameTypes: metadata.map { |row| row['gameType'] }.compact.uniq,
    heuristicAdditionalCategories: candidates,
    infrastructure: game['category'] == 'INFRASTRUCTURE'
  }
end
game_rows = rows.reject { |row| row[:infrastructure] }
summary = {
  catalogRows: rows.size,
  gameplayRows: game_rows.size,
  declaredEnumCategories: declared,
  requiredCategories: required,
  missingEnumCategories: required - declared,
  categoryCounts: game_rows.group_by { |row| row[:declaredCategory] }.transform_values(&:size),
  sourceGameTypeCounts: game_rows.flat_map { |row| row[:sourceGameTypes] }.group_by(&:itself).transform_values(&:size),
  chessCandidates: game_rows.count { |row| row[:heuristicAdditionalCategories].include?('CHESS') },
  diceBettingCandidates: game_rows.count { |row| row[:heuristicAdditionalCategories].include?('DICE_BETTING') },
  unclassifiedOrOther: game_rows.count { |row| row[:declaredCategory].nil? || row[:declaredCategory] == 'OTHER' }
}
report = {
  schemaVersion: 1, generatedAt: Time.now.utc.iso8601, summary: summary, rows: rows,
  limitations: ['Name heuristics are discovery candidates only; category assignment must use card set, action set, FSM and scoring semantics.']
}
File.write(output_path, JSON.pretty_generate(report) + "\n", mode: 'w:UTF-8')

task = File.read(task_path, encoding: 'UTF-8')
row = "| CLASS01 | 未完成 | 未通过·已建清册 | 已建一级分类完整性清册；玩法目录=#{summary[:gameplayRows]}、当前分类=#{summary[:categoryCounts]}、枚举缺失=#{summary[:missingEnumCategories].join(',')}、棋类候选=#{summary[:chessCandidates]}、骰子/押注候选=#{summary[:diceBettingCandidates]}；源 gameType 仅有麻将/扑克，棋类、骰子/押注及其他类尚未依语义定类。 证据：work/audit/primary-game-category-audit.json；工具：tools/audit_primary_game_categories.rb |"
task.sub!(/^\| CLASS01 \|.*$/, row) or abort 'CLASS01 row not found'
task.sub!(/^下一项：.*$/, '下一项：CLASS02 玩法族判定依据') or abort 'next pointer not found'
File.write(task_path, task, mode: 'w:UTF-8')
puts JSON.generate(summary)
