#!/usr/bin/env ruby
# frozen_string_literal: true

require 'json'
require 'time'

root = File.expand_path('..', __dir__)
registration = JSON.parse(File.read(File.join(root, 'work/audit/auto-registration-inventory.json'), encoding: 'UTF-8'))
output_path = File.join(root, 'work/audit/backend-empty-success-audit.json')
task_path = File.join(root, 'docs/Aoo-前后端全框架功能通信审计任务清单.md')

candidate_kinds = %w[provider router handler controller].freeze
success_pattern = /(?:RuleResult\.accept\s*\(|GameCommandResult\s*\(|ResponseEntity\.ok\s*\(|return\s+(?:true|0|"success"|'success')|code\s*[,=:]\s*0|Success\s*\()/i
authority_pattern = /(?:authoritative|execute\s*\(|validate\w*\s*\(|state\s*=|transition|apply\w*\s*\(|roomRegistry|GameRegistry)/i
persistence_pattern = /(?:\.save\s*\(|\.persist\s*\(|Repository|Dao\b|Jdbc|Redis|Jedis|Redisson|ledger|billing|transaction)/i
broadcast_pattern = /(?:broadcast|publish|push|sendTo|emit\s*\(|Producer|EventBus|MessageQueue)/i
empty_pattern = /\{\s*(?:return\s+(?:null|true|false|0|List\.of\(\)|Map\.of\(\)|Optional\.empty\(\));?\s*)?\}/m

hits = registration.fetch('hits').select { |hit| candidate_kinds.include?(hit.fetch('kind')) }
by_path = hits.group_by { |hit| hit.fetch('path') }
files = by_path.map do |path, path_hits|
  next unless File.file?(path) && File.size(path) <= 3_000_000
  text = File.binread(path).force_encoding('UTF-8').scrub
  success = text.match?(success_pattern)
  authority = text.match?(authority_pattern)
  persistence = text.match?(persistence_pattern)
  broadcast = text.match?(broadcast_pattern)
  empty_methods = text.scan(empty_pattern).size
  generic_accept = File.basename(path) == 'CatalogGameProvider.java' || text.match?(/metadata-driven provider/i)
  {
    path: path.delete_prefix(root + '/'),
    kinds: path_hits.map { |hit| hit.fetch('kind') }.uniq.sort,
    registrationLines: path_hits.map { |hit| hit.fetch('line') }.uniq.sort,
    successReturnPresent: success,
    authorityMutationPresent: authority,
    persistencePresent: persistence,
    broadcastPresent: broadcast,
    emptyMethodCandidates: empty_methods,
    genericAcceptProvider: generic_accept,
    candidateEmptySuccess: success && (!authority || !persistence || !broadcast),
    missingClosureParts: [(!authority ? 'authority' : nil), (!persistence ? 'persistence' : nil), (!broadcast ? 'broadcast' : nil)].compact
  }
end.compact.sort_by { |file| file[:path] }

summary = {
  registrationFiles: files.size,
  successFiles: files.count { |file| file[:successReturnPresent] },
  candidateEmptySuccessFiles: files.count { |file| file[:candidateEmptySuccess] },
  genericAcceptProviders: files.count { |file| file[:genericAcceptProvider] },
  emptyMethodCandidates: files.sum { |file| file[:emptyMethodCandidates] },
  missingAuthority: files.count { |file| file[:candidateEmptySuccess] && file[:missingClosureParts].include?('authority') },
  missingPersistence: files.count { |file| file[:candidateEmptySuccess] && file[:missingClosureParts].include?('persistence') },
  missingBroadcast: files.count { |file| file[:candidateEmptySuccess] && file[:missingClosureParts].include?('broadcast') }
}
report = {
  schemaVersion: 1, generatedAt: Time.now.utc.iso8601, summary: summary,
  invariant: 'A successful business write must validate and mutate authoritative state, persist required effects and broadcast the committed result; metadata-only acceptance is forbidden.',
  files: files,
  limitations: ['File-level static presence is candidate evidence; method-level control/data flow and runtime state-diff assertions are required before closure.']
}
File.write(output_path, JSON.pretty_generate(report) + "\n", mode: 'w:UTF-8')

task = File.read(task_path, encoding: 'UTF-8')
row = "| EQ12 | 未完成 | 未通过·已建门禁 | 已建后端空成功候选门禁；注册文件=#{summary[:registrationFiles]}、返回成功文件=#{summary[:successFiles]}、缺权威状态/持久化/广播闭环候选=#{summary[:candidateEmptySuccessFiles]}、元数据通用接受 Provider=#{summary[:genericAcceptProviders]}、空方法候选=#{summary[:emptyMethodCandidates]}；需达零空成功且逐写操作运行状态差异通过才能闭合。 证据：work/audit/backend-empty-success-audit.json；工具：tools/audit_backend_empty_success.rb |"
task.sub!(/^\| EQ12 \|.*$/, row) or abort 'EQ12 row not found'
task.sub!(/^下一项：.*$/, '下一项：CLASS01 一级分类完整性') or abort 'next pointer not found'
File.write(task_path, task, mode: 'w:UTF-8')
puts JSON.generate(summary)
