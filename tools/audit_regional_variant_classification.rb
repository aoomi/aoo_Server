#!/usr/bin/env ruby
# frozen_string_literal: true

require 'json'
require 'time'

root = File.expand_path('..', __dir__)
mapping = JSON.parse(File.read(File.join(root, 'work/audit/database-runtime-mapping.json'), encoding: 'UTF-8')).fetch('mappings')
semantics = JSON.parse(File.read(File.join(root, 'work/audit/game-family-semantic-evidence.json'), encoding: 'UTF-8')).fetch('games')
semantic_by_code = semantics.to_h { |row| [row.fetch('code'), row] }
output_path = File.join(root, 'work/audit/regional-variant-classification.json')
task_path = File.join(root, 'docs/Aoo-前后端全框架功能通信审计任务清单.md')

games = mapping.reject { |game| game['category'] == 'INFRASTRUCTURE' }.map do |game|
  semantic = semantic_by_code.fetch(game.fetch('code'), {})
  {
    code: game.fetch('code'), category: game['category'], family: game['family'],
    region: game['region'], databaseMapped: game['databaseMapped'],
    semanticComplete: semantic.fetch('semanticComplete', false),
    semanticEvidence: semantic.fetch('semanticEvidence', {})
  }
end

families = games.group_by { |game| [game[:category], game[:family]] }.map do |(category, family), members|
  regions = members.map { |game| game[:region] }.compact.uniq.sort
  complete = members.select { |game| game[:semanticComplete] }
  {
    category: category, family: family, gameCount: members.size, regions: regions,
    multiRegion: regions.size > 1, semanticCompleteGames: complete.map { |game| game[:code] },
    unresolvedVariantDecision: members.size > 1 && complete.size < members.size,
    members: members
  }
end.sort_by { |row| [row[:category].to_s, row[:family].to_s] }

summary = {
  gameCount: games.size, familyCount: families.size,
  gamesWithoutRegion: games.count { |game| game[:region].nil? || game[:region].to_s.empty? },
  multiRegionFamilies: families.count { |family| family[:multiRegion] },
  unresolvedVariantFamilies: families.count { |family| family[:unresolvedVariantDecision] },
  unresolvedVariantGames: families.select { |family| family[:unresolvedVariantDecision] }.sum { |family| family[:gameCount] }
}
report = {
  schemaVersion: 1, generatedAt: Time.now.utc.iso8601, summary: summary,
  invariant: 'A regional variant may share a family only when card set, actions and FSM are equal and differences are confined to validated rule/scoring components; fundamental flow differences require a new family.',
  families: families,
  limitations: ['Region metadata plus filename semantics cannot prove flow equivalence; fixed-fact action/FSM/scoring comparisons are required per family member.']
}
File.write(output_path, JSON.pretty_generate(report) + "\n", mode: 'w:UTF-8')

task = File.read(task_path, encoding: 'UTF-8')
row = "| CLASS03 | 未完成 | 已建判定门禁·未闭合 | 已按分类+玩法族+地区分组建立地区变体判定清册；玩法=#{summary[:gameCount]}、族=#{summary[:familyCount]}、多地区族=#{summary[:multiRegionFamilies]}、未判定变体族=#{summary[:unresolvedVariantFamilies]}、涉及玩法=#{summary[:unresolvedVariantGames]}、缺地区=#{summary[:gamesWithoutRegion]}；未通过牌集/动作/FSM/算分事实对照前不得仅凭地区名称归变体。 证据：work/audit/regional-variant-classification.json；工具：tools/audit_regional_variant_classification.rb |"
task.sub!(/^\| CLASS03 \|.*$/, row) or abort 'CLASS03 row not found'
task.sub!(/^下一项：.*$/, '下一项：CLASS04 房间模式正交性') or abort 'next pointer not found'
File.write(task_path, task, mode: 'w:UTF-8')
puts JSON.generate(summary)
