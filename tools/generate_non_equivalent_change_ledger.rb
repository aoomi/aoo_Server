#!/usr/bin/env ruby
# frozen_string_literal: true

require 'json'
require 'time'
require 'digest'

root = File.expand_path('..', __dir__)
docs_root = File.join(root, 'docs')
output_json = File.join(root, 'work/audit/non-equivalent-change-ledger.json')
output_md = File.join(docs_root, 'Aoo-非等价改进台账.md')
task_path = File.join(docs_root, 'Aoo-前后端全框架功能通信审计任务清单.md')

candidate_pattern = /(?:旧|新|兼容|废弃|隔离|替代|迁移|升级|安全|架构|版本|legacy|deprecated|compatib|replac|migrat|upgrad|security)/i
reason_patterns = {
  'security' => /(?:安全|鉴权|防作弊|防重放|敏感|security|auth)/i,
  'architecture' => /(?:架构|分层|解耦|公共层|模块|architecture)/i,
  'version' => /(?:版本|升级|Java|Maven|Creator|dependency|plugin)/i,
  'operations' => /(?:运维|可观测|监控|部署|回滚|observability)/i
}.freeze

entries = []
Dir.glob(File.join(docs_root, '*.md')).sort.each do |path|
  next if path == output_md
  File.readlines(path, encoding: 'UTF-8', invalid: :replace, undef: :replace).each_with_index do |line, index|
    text = line.strip
    next if text.empty? || !text.match?(candidate_pattern)
    reasons = reason_patterns.each_with_object([]) { |(kind, regex), found| found << kind if text.match?(regex) }
    has_old = text.match?(/(?:旧行为|原行为|遗留|legacy|2\.22)/i)
    has_new = text.match?(/(?:新行为|新接口|Aoo|替代|升级|迁移)/i)
    has_acceptance = text.match?(/(?:验收|必须|不得|禁止|通过|失败|门禁)/i)
    id = Digest::SHA256.hexdigest("#{path}:#{index + 1}:#{text}")[0, 16]
    entries << {
      id: id, source: path.delete_prefix(root + '/'), line: index + 1, statement: text[0, 1000],
      reasons: reasons, oldBehaviorDocumented: has_old, newBehaviorDocumented: has_new,
      acceptanceDocumented: has_acceptance,
      complete: has_old && has_new && !reasons.empty? && has_acceptance
    }
  end
end

summary = {
  candidates: entries.size,
  complete: entries.count { |entry| entry[:complete] },
  missingOldBehavior: entries.count { |entry| !entry[:oldBehaviorDocumented] },
  missingNewBehavior: entries.count { |entry| !entry[:newBehaviorDocumented] },
  missingReason: entries.count { |entry| entry[:reasons].empty? },
  missingAcceptance: entries.count { |entry| !entry[:acceptanceDocumented] }
}
report = {schemaVersion: 1, generatedAt: Time.now.utc.iso8601, summary: summary, entries: entries}
File.write(output_json, JSON.pretty_generate(report) + "\n", mode: 'w:UTF-8')

markdown = +<<~MD
  # Aoo 非等价改进台账

  > 由 `tools/generate_non_equivalent_change_ledger.rb` 生成。仅记录由安全、架构、版本或运维升级导致的有意非等价变更。每项必须同时具备旧行为、新行为、变更理由和新验收标准才能闭合。

  - 生成时间：#{report[:generatedAt]}
  - 候选：#{summary[:candidates]}
  - 完整：#{summary[:complete]}
  - 缺旧行为：#{summary[:missingOldBehavior]}
  - 缺新行为：#{summary[:missingNewBehavior]}
  - 缺理由：#{summary[:missingReason]}
  - 缺验收：#{summary[:missingAcceptance]}

  ## 待补齐项

  | ID | 来源 | 旧行为 | 新行为 | 理由 | 验收 | 记录 |
  |---|---|---:|---:|---:|---:|---|
MD
entries.reject { |entry| entry[:complete] }.first(500).each do |entry|
  statement = entry[:statement].gsub('|', '\\|').gsub(/\s+/, ' ')[0, 260]
  markdown << "| #{entry[:id]} | #{entry[:source]}:#{entry[:line]} | #{entry[:oldBehaviorDocumented] ? '有' : '缺'} | #{entry[:newBehaviorDocumented] ? '有' : '缺'} | #{entry[:reasons].empty? ? '缺' : entry[:reasons].join(',')} | #{entry[:acceptanceDocumented] ? '有' : '缺'} | #{statement} |\n"
end
File.write(output_md, markdown, mode: 'w:UTF-8')

task = File.read(task_path, encoding: 'UTF-8')
row = "| EQ10 | 未完成 | 已集中建账·待补齐 | 已生成安全/架构/版本/运维非等价改进中央台账；候选=#{summary[:candidates]}、四要素完整=#{summary[:complete]}、缺旧行为=#{summary[:missingOldBehavior]}、缺新行为=#{summary[:missingNewBehavior]}、缺理由=#{summary[:missingReason]}、缺验收=#{summary[:missingAcceptance]}，未补齐项不得闭合。 证据：docs/Aoo-非等价改进台账.md、work/audit/non-equivalent-change-ledger.json |"
task.sub!(/^\| EQ10 \|.*$/, row) or abort 'EQ10 row not found'
task.sub!(/^下一项：.*$/, '下一项：EQ11 界面假实现门禁') or abort 'next pointer not found'
File.write(task_path, task, mode: 'w:UTF-8')
puts JSON.generate(summary)
