#!/usr/bin/env ruby
# frozen_string_literal: true

require 'csv'
require 'json'
require 'time'

root = File.expand_path('..', __dir__)
client_root = File.expand_path('../Client/assets', root)
baseline = JSON.parse(File.read(File.join(root, 'work/audit/audit-source-baseline.json'), encoding: 'UTF-8'))
interaction_path = File.join(root, 'work/audit/cocos-interactions.tsv')
output_path = File.join(root, 'work/audit/ui-backend-closure-audit.json')
task_path = File.join(root, 'docs/Aoo-前后端全框架功能通信审计任务清单.md')

interaction_rows = CSV.read(interaction_path, headers: true, col_sep: "\t")
click_pattern = /(?:Button\.EventType\.CLICK|\.on\s*\(\s*['"]click|@click\s*=|\bon(?:Btn|Button|Click)[A-Za-z0-9_$]*\s*\()/i
backend_pattern = /(?:\.send\s*\(|\.request\s*\(|\.call\s*\(|\.emit\s*\(|fetch\s*\(|axios\.|Http|WebSocket|ProtocolClient|Gateway|NetworkAdapter)/i
fake_pattern = /(?:\bmock\b|\bfake\b|dummy|stub|simulate|simulation|Promise\.resolve\s*\(|return\s+\{\s*(?:code|status)\s*:\s*0|setTimeout\s*\([^,]+,\s*(?:0|[1-9]\d{0,2})\s*\))/i
success_pattern = /(?:success|succeed|completed|created|saved|成功|已保存|已创建)/i

files = []
baseline.fetch('files').each do |item|
  path = item.fetch('path')
  next unless path.start_with?(client_root + '/') && File.file?(path)
  next unless %w[.ts .js .mjs .cjs .vue].include?(File.extname(path).downcase) && item.fetch('size') <= 2_000_000
  text = File.binread(path).force_encoding('UTF-8').scrub
  next unless text.match?(click_pattern) || text.match?(fake_pattern)
  click_sites = []
  fake_sites = []
  text.lines.each_with_index do |line, index|
    click_sites << {line: index + 1, excerpt: line.strip[0, 300]} if line.match?(click_pattern)
    fake_sites << {line: index + 1, excerpt: line.strip[0, 300], successLike: line.match?(success_pattern)} if line.match?(fake_pattern)
  end
  files << {
    path: path.delete_prefix(client_root + '/'),
    clickSites: click_sites,
    backendCallPresent: text.match?(backend_pattern),
    fakeSites: fake_sites,
    candidateNoBackendClosure: !click_sites.empty? && !text.match?(backend_pattern),
    candidateFixedSuccess: fake_sites.any? { |site| site[:successLike] }
  }
end

summary = {
  interactiveComponents: interaction_rows.size,
  serializedBindings: interaction_rows.count { |row| row['serializedEventCount'].to_i.positive? },
  scriptFilesWithClickSites: files.count { |file| !file[:clickSites].empty? },
  clickSites: files.sum { |file| file[:clickSites].size },
  candidateFilesWithoutBackendClosure: files.count { |file| file[:candidateNoBackendClosure] },
  fakeSites: files.sum { |file| file[:fakeSites].size },
  candidateFixedSuccessFiles: files.count { |file| file[:candidateFixedSuccess] }
}
report = {
  schemaVersion: 1, generatedAt: Time.now.utc.iso8601, summary: summary,
  policy: 'A business UI action must have request, authoritative state mutation/persistence/broadcast, response rendering and failure rendering evidence; local-only controls require explicit classification.',
  files: files.sort_by { |file| file[:path] },
  limitations: ['File-level network presence is candidate evidence only; runtime request/response correlation is required per serialized or dynamic button binding.']
}
File.write(output_path, JSON.pretty_generate(report) + "\n", mode: 'w:UTF-8')

task = File.read(task_path, encoding: 'UTF-8')
row = "| EQ11 | 未完成 | 未通过·已建门禁 | 已建界面假实现候选门禁；交互组件=#{summary[:interactiveComponents]}、序列化绑定=#{summary[:serializedBindings]}、点击站点=#{summary[:clickSites]}、文件级无后端闭环候选=#{summary[:candidateFilesWithoutBackendClosure]}、模拟/固定成功候选文件=#{summary[:candidateFixedSuccessFiles]}；尚需每个业务按钮的运行请求-权威状态-持久化-广播-失败闭环。 证据：work/audit/ui-backend-closure-audit.json；工具：tools/audit_ui_backend_closure.rb |"
task.sub!(/^\| EQ11 \|.*$/, row) or abort 'EQ11 row not found'
task.sub!(/^下一项：.*$/, '下一项：EQ12 后端空实现门禁') or abort 'next pointer not found'
File.write(task_path, task, mode: 'w:UTF-8')
puts JSON.generate(summary)
