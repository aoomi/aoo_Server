#!/usr/bin/env ruby
# frozen_string_literal: true

require 'json'
require 'time'

ROOT = File.expand_path('..', __dir__)
SOURCE = File.join(ROOT, 'protocol/aoo-protocol-v2.json')
OUTPUT = File.join(ROOT, 'docs/generated/proto01-authority.json')
TASKS = File.join(ROOT, 'docs/Aoo-前后端全框架功能通信审计任务清单.md')
MANIFEST = File.join(ROOT, 'docs/generated/generated-artifact-manifest.json')

protocol = JSON.parse(File.read(SOURCE, encoding: 'UTF-8'))
messages = protocol.fetch('messages')
required_message_fields = %w[msgId kind direction transport version stage auth write idempotency request response errors]
message_ids = messages.map { |message| message.fetch('msgId') }
checks = {
  singleCanonicalSource: Dir[File.join(ROOT, 'protocol/*protocol*.json')].reject { |path| path.end_with?('.baseline.json') }.map { |path| File.basename(path) } == ['aoo-protocol-v2.json'],
  schemaIdentity: protocol.fetch('$schema').include?('json-schema.org') && protocol.fetch('$id') == 'aoo-protocol-v2',
  versionDeclared: protocol.fetch('protocolVersion').to_s.match?(/\A\d+\.\d+\z/),
  envelopeFieldsDeclared: %w[protocolVersion msgId kind requestId seq timestamp traceId body].all? { |field| protocol.fetch('requiredRequestFields').include?(field) },
  errorRangesDeclared: !protocol.fetch('errorCodeRanges').empty?,
  messagesComplete: !messages.empty? && messages.all? { |message| (required_message_fields - message.keys).empty? },
  messageIdsUnique: message_ids.uniq.size == message_ids.size,
  governanceNamesSource: File.read(File.join(ROOT, 'protocol/README.md'), encoding: 'UTF-8').include?('唯一机器源')
}
report = {
  schemaVersion: 1,
  generatedAt: Time.now.utc.iso8601,
  task: 'PROTO01',
  canonicalSource: 'protocol/aoo-protocol-v2.json',
  protocolVersion: protocol.fetch('protocolVersion'),
  messageCount: messages.size,
  errorCodeRanges: protocol.fetch('errorCodeRanges'),
  checks: checks,
  passed: checks.values.all?
}
File.write(OUTPUT, JSON.pretty_generate(report) + "\n", mode: 'w:UTF-8')

tasks = File.read(TASKS, encoding: 'UTF-8')
status = report[:passed] ? '已完成' : '失败跳过'
result = report[:passed] ? '通过' : '未完成'
detail = report[:passed] ? "已确认 protocol/aoo-protocol-v2.json 为唯一非基线机器源，覆盖统一信封、#{messages.size} 个消息的方向/传输/字段/权限/幂等/错误码及协议版本 #{report[:protocolVersion]}；README 与治理规则禁止第二权威源。" : "协议权威源门禁未通过：#{checks.reject { |_name, passed| passed }.keys.join('、')}，已记录后跳过。"
tasks.sub!(/^\| PROTO01 \|.*$/, "| PROTO01 | #{status} | `protocol` 权威源 | #{result} | #{detail} 证据：docs/generated/proto01-authority.json |") or abort 'PROTO01 row not found'
File.write(TASKS, tasks, mode: 'w:UTF-8')

manifest = JSON.parse(File.read(MANIFEST, encoding: 'UTF-8'))
path = 'docs/generated/proto01-authority.json'
unless manifest.fetch('artifacts').any? { |artifact| artifact['path'] == path }
  manifest.fetch('artifacts') << {
    'path' => path,
    'kind' => 'generated-protocol-evidence',
    'authoritativeInputs' => ['protocol/aoo-protocol-v2.json', 'protocol/README.md', 'protocol/PROTOCOL_GOVERNANCE.md'],
    'generator' => 'scripts/audit-proto01-authority.rb',
    'rebuild' => 'ruby scripts/audit-proto01-authority.rb',
    'owner' => 'protocol-governance',
    'editPolicy' => 'generated-do-not-edit'
  }
end
File.write(MANIFEST, JSON.pretty_generate(manifest) + "\n", mode: 'w:UTF-8')
puts JSON.generate(report.slice(:task, :protocolVersion, :messageCount, :checks, :passed))
exit 1 unless report[:passed]
