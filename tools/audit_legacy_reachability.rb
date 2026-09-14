#!/usr/bin/env ruby
# frozen_string_literal: true

require 'json'
require 'time'

root = File.expand_path('..', __dir__)
client_root = File.expand_path('../Client', root)
baseline = JSON.parse(File.read(File.join(root, 'work/audit/audit-source-baseline.json'), encoding: 'UTF-8'))
output_path = File.join(root, 'work/audit/legacy-production-reachability.json')
task_path = File.join(root, 'docs/Aoo-前后端全框架功能通信审计任务清单.md')

production_roots = [
  File.join(root, 'modules'),
  File.join(root, 'server'),
  File.join(client_root, 'assets')
].freeze
source_extensions = %w[.java .kt .xml .yml .yaml .properties .ts .js .mjs .cjs .json].freeze
hard_patterns = {
  'legacyMavenCoordinate' => /com\.aoo\.legacy|<id>aoo-legacy<\/id>/,
  'compatibilityRuntimeImport' => /(?:from\s+['"][^'"]*CompatibilityApp|import\s+['"][^'"]*CompatibilityApp)/,
  'legacyTransport' => /(?:LegacyWebSocket|LegacyHttp|ProtocolMigrationRouter|login_compat)/,
  'legacyBootstrap' => /(?:LegacyApplicationRuntime|LegacyLoginScreen|LegacyLobbyScreen|LegacyNJPdkSwitchCoordinator)/
}.freeze
soft_patterns = {
  'legacyBridgeModel' => /(?:LegacyCompatibleRoom|legacyRoom|BridgedGameRoom)/,
  'legacyNamedCode' => /\bLegacy[A-Z][A-Za-z0-9_$]+/,
  'legacyAssetPath' => /legacy-ui\//
}.freeze

findings = []
baseline.fetch('files').each do |item|
  path = item.fetch('path')
  next unless production_roots.any? { |prefix| path.start_with?(prefix + '/') } || path == File.join(root, 'pom.xml')
  next unless File.file?(path) && item.fetch('size') <= 3_000_000
  next unless source_extensions.include?(File.extname(path).downcase) || File.basename(path) == 'pom.xml'
  text = File.binread(path).force_encoding('UTF-8').scrub
  matches = []
  text.lines.each_with_index do |line, index|
    hard_patterns.each { |kind, regex| matches << {severity: 'hard', kind: kind, line: index + 1, excerpt: line.strip[0, 300]} if line.match?(regex) }
    soft_patterns.each { |kind, regex| matches << {severity: 'soft', kind: kind, line: index + 1, excerpt: line.strip[0, 300]} if line.match?(regex) }
    break if matches.size >= 100
  end
  findings << {path: path.delete_prefix(root + '/'), findings: matches} unless matches.empty?
end

hard_count = findings.sum { |entry| entry[:findings].count { |finding| finding[:severity] == 'hard' } }
soft_count = findings.sum { |entry| entry[:findings].count { |finding| finding[:severity] == 'soft' } }
hard_files = findings.count { |entry| entry[:findings].any? { |finding| finding[:severity] == 'hard' } }
summary = {files: findings.size, hardFiles: hard_files, hardReferences: hard_count, softReferences: soft_count,
           productionLegacyReachable: hard_count.positive?}
report = {
  schemaVersion: 1,
  generatedAt: Time.now.utc.iso8601,
  summary: summary,
  policy: {
    hard: 'Build coordinates, production imports, transports and bootstrap paths must reach zero.',
    soft: 'Bridge model names and imported asset paths require explicit migration classification before removal.'
  },
  findings: findings.sort_by { |entry| entry[:path] }
}
File.write(output_path, JSON.pretty_generate(report) + "\n", mode: 'w:UTF-8')

task = File.read(task_path, encoding: 'UTF-8')
row = "| CALL11 | 未完成 | 未通过·已建门禁 | 已建立新生产路径 Legacy 可达性可重复门禁；当前硬可达文件=#{hard_files}、硬引用=#{hard_count}、软迁移引用=#{soft_count}，仍可达旧 Maven 坐标、CompatibilityApp、Legacy 传输/启动路径，不得闭合。 证据：work/audit/legacy-production-reachability.json；工具：tools/audit_legacy_reachability.rb |"
task.sub!(/^\| CALL11 \|.*$/, row) or abort 'CALL11 row not found'
task.sub!(/^下一项：.*$/, '下一项：CALL12 注册守恒门禁') or abort 'next pointer not found'
File.write(task_path, task, mode: 'w:UTF-8')
puts JSON.generate(summary)
exit(hard_count.zero? ? 0 : 2) if ENV['AOO_ENFORCE_LEGACY_GATE'] == '1'
