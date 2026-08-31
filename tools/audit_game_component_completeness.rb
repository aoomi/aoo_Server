#!/usr/bin/env ruby
# frozen_string_literal: true

require 'json'
require 'time'

root = File.expand_path('..', __dir__)
classification = JSON.parse(File.read(File.join(root, 'work/generated/classification/game-classification.json'), encoding: 'UTF-8'))
semantics = JSON.parse(File.read(File.join(root, 'work/audit/game-family-semantic-evidence.json'), encoding: 'UTF-8'))
registration = JSON.parse(File.read(File.join(root, 'work/audit/registration-conservation-audit.json'), encoding: 'UTF-8'))
lifecycle = JSON.parse(File.read(File.join(root, 'work/audit/common-game-lifecycle-placement.json'), encoding: 'UTF-8'))
output_path = File.join(root, 'work/audit/game-component-completeness.json')
task_path = File.join(root, 'docs/Aoo-前后端全框架功能通信审计任务清单.md')

semantic_by_code = semantics.fetch('games').to_h { |row| [row.fetch('code'), row] }
registration_by_code = registration.fetch('rows').to_h { |row| [row.fetch('code'), row] }
lifecycle_files = lifecycle.fetch('files')

rows = classification.fetch('rows').map do |game|
  code = game.fetch('code')
  semantic = semantic_by_code.fetch(code, {})
  registered = registration_by_code.fetch(code, {})
  files = lifecycle_files.select { |file| file.fetch('gameCodes', []).include?(code) }
  semantic_evidence = semantic.fetch('semanticEvidence', {})
  components = {
    nativeProvider: registered.fetch('nativeProviderRegistered', false),
    lifecycle: game['sourceType'] == 'PROVIDER' && files.any? { |file| file.fetch('capabilities', []).include?('lifecycle') },
    rules: !semantic_evidence.fetch('actions', []).empty?,
    flow: !semantic_evidence.fetch('stateMachine', []).empty?,
    scoring: !semantic_evidence.fetch('scoring', []).empty?,
    snapshot: files.any? { |file| file.fetch('capabilities', []).include?('snapshot') }
  }
  missing = components.select { |_name, present| !present }.keys
  {gameId: game['gameId'], code: code, category: game['category'], family: game['family'],
   sourceType: game['sourceType'], components: components, missingComponents: missing, complete: missing.empty?}
end

summary = {
  gameCount: rows.size,
  completeGames: rows.count { |row| row[:complete] },
  incompleteGames: rows.count { |row| !row[:complete] },
  metadataOnlyGames: rows.count { |row| row[:sourceType] == 'CATALOG_METADATA' },
  missingComponentCounts: %i[nativeProvider lifecycle rules flow scoring snapshot].to_h do |name|
    [name, rows.count { |row| row[:missingComponents].include?(name) }]
  end
}
report = {
  schemaVersion: 1, generatedAt: Time.now.utc.iso8601, summary: summary,
  invariant: 'Every enabled game resolves to a native Provider with executable lifecycle, rule, flow, scoring and player-view snapshot components; metadata-only providers are forbidden.',
  games: rows,
  limitations: ['Static source evidence does not prove component execution; startup instantiation and fixed-fact lifecycle tests remain required.']
}
File.write(output_path, JSON.pretty_generate(report) + "\n", mode: 'w:UTF-8')

task = File.read(task_path, encoding: 'UTF-8')
row = "| REF03 | 未完成 | 未通过·已建组件门禁 | 已逐玩法聚合原生 Provider、生命周期、规则、流程、算分及玩家视角快照组件；玩法=#{summary[:gameCount]}、完整=#{summary[:completeGames]}、不完整=#{summary[:incompleteGames]}、元数据空实现=#{summary[:metadataOnlyGames]}、缺失分布=#{summary[:missingComponentCounts]}；未达逐启用玩法全组件实例化与行为测试通过不得闭合。 证据：work/audit/game-component-completeness.json |"
task.sub!(/^\| REF03 \|.*$/, row) or abort 'REF03 row not found'
task.sub!(/^下一项：.*$/, '下一项：REF04 组件 key 唯一性与类型门禁') or abort 'next pointer not found'
File.write(task_path, task, mode: 'w:UTF-8')
puts JSON.generate(summary)
