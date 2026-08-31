#!/usr/bin/env ruby
# frozen_string_literal: true

require 'json'
require 'time'

root = File.expand_path('..', __dir__)
client_root = File.expand_path('../Client', root)
baseline = JSON.parse(File.read(File.join(root, 'work/audit/audit-source-baseline.json'), encoding: 'UTF-8'))
mapping = JSON.parse(File.read(File.join(root, 'work/audit/database-runtime-mapping.json'), encoding: 'UTF-8')).fetch('mappings')
output_path = File.join(root, 'work/audit/card-category-placement.json')
task_path = File.join(root, 'docs/Aoo-前后端全框架功能通信审计任务清单.md')

category_codes = mapping.reject { |game| game['category'] == 'INFRASTRUCTURE' }.group_by { |game| game['category'] }
                        .transform_values { |games| games.map { |game| game.fetch('code').downcase }.uniq }
capability_patterns = {
  'encoding' => /(?:CardCodec|TileCodec|encodeCard|decodeCard|cardValue|tileValue|cardType|tileType)/i,
  'combination' => /(?:CardCombination|HandPattern|HuPattern|Meld|PokerType|WinType)/i,
  'action' => /(?:PlayCard|Discard|DrawTile|Chi|Peng|Gang|Hu|Bet|Fold|PassAction)/i,
  'display' => /(?:CardRenderer|TileRenderer|SpriteFrame|cardSprite|tileSprite)/i
}.freeze

def inferred_category(relative, category_codes)
  return 'MAHJONG' if relative.match?(%r{/(?:Mahjong|麻将)/}i)
  return 'POKER' if relative.match?(%r{/(?:Poker|扑克)/}i)
  return 'LONG_CARD' if relative.match?(%r{/(?:LongCard|长牌)/}i)
  return 'WORD_CARD' if relative.match?(%r{/(?:WordCard|字牌)/}i)
  lower = relative.downcase
  matches = category_codes.each_with_object([]) do |(category, codes), found|
    found << category if codes.any? { |code| lower.match?(/(?<![a-z0-9_])#{Regexp.escape(code)}(?![a-z0-9_])/) }
  end
  matches.size == 1 ? matches.first : nil
end

files = []
baseline.fetch('files').each do |item|
  path = item.fetch('path')
  next unless path.start_with?(File.join(root, 'server') + '/', File.join(root, 'modules') + '/', File.join(client_root, 'assets') + '/')
  next unless File.file?(path) && %w[.java .kt .ts .js].include?(File.extname(path).downcase) && item.fetch('size') <= 2_000_000
  text = File.binread(path).force_encoding('UTF-8').scrub
  capabilities = capability_patterns.each_with_object([]) { |(kind, regex), found| found << kind if text.match?(regex) }
  next if capabilities.empty?
  relative = path.delete_prefix(root + '/')
  category = inferred_category(relative, category_codes)
  shared = relative.match?(%r{server/(?:GameCommon|GameSPI|Families)/|modules/common/|Client/assets/Common/}i)
  cross_references = %w[MAHJONG POKER LONG_CARD WORD_CARD].select do |other|
    next false if other == category
    token = other.split('_').map(&:capitalize).join
    text.match?(/(?:com\.aoo\.bcg\.)?#{Regexp.escape(token)}|GameCategory\.#{other}/i)
  end
  files << {path: relative, inferredCategory: category, sharedLayer: shared,
            capabilities: capabilities, crossCategoryReferences: cross_references,
            placementCandidateViolation: category.nil? && !shared,
            crossCategoryCandidateViolation: !cross_references.empty? && !shared}
end

summary = {
  capabilityFiles: files.size,
  sharedFiles: files.count { |file| file[:sharedLayer] },
  categoryFiles: files.count { |file| !file[:inferredCategory].nil? },
  unplacedFiles: files.count { |file| file[:placementCandidateViolation] },
  crossCategoryFiles: files.count { |file| file[:crossCategoryCandidateViolation] },
  capabilityCounts: capability_patterns.keys.to_h { |kind| [kind, files.count { |file| file[:capabilities].include?(kind) }] },
  categoryCounts: files.reject { |file| file[:inferredCategory].nil? }.group_by { |file| file[:inferredCategory] }.transform_values(&:size)
}
report = {
  schemaVersion: 1, generatedAt: Time.now.utc.iso8601, summary: summary,
  invariant: 'Card/tile encoding, combinations, actions and display adapters belong to their category public layer; cross-category dependencies use neutral GameSPI contracts only.',
  files: files.sort_by { |file| file[:path] },
  limitations: ['Static token inference is candidate evidence; package dependency and behavior tests must prove category isolation.']
}
File.write(output_path, JSON.pretty_generate(report) + "\n", mode: 'w:UTF-8')

task = File.read(task_path, encoding: 'UTF-8')
row = "| CLASS07 | 未完成 | 已建归位门禁·待整改 | 已盘点牌/牌组编码、组合、动作及展示适配的牌类归位；能力文件=#{summary[:capabilityFiles]}、公共层=#{summary[:sharedFiles]}、已归牌类=#{summary[:categoryFiles]}、未归位候选=#{summary[:unplacedFiles]}、跨牌类候选=#{summary[:crossCategoryFiles]}；需达零跨类具体依赖且只经 GameSPI 中性合同才能闭合。 证据：work/audit/card-category-placement.json；工具：tools/audit_card_category_placement.rb |"
task.sub!(/^\| CLASS07 \|.*$/, row) or abort 'CLASS07 row not found'
task.sub!(/^下一项：.*$/, '下一项：CLASS08 地区规则归位') or abort 'next pointer not found'
File.write(task_path, task, mode: 'w:UTF-8')
puts JSON.generate(summary)
