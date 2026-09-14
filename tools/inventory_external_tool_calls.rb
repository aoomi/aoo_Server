#!/usr/bin/env ruby
# frozen_string_literal: true

require 'json'
require 'time'

root = File.expand_path('..', __dir__)
baseline = JSON.parse(File.read(File.join(root, 'work/audit/audit-source-baseline.json'), encoding: 'UTF-8'))
output = File.join(root, 'work/audit/external-tool-call-inventory.json')
task_path = File.join(root, 'docs/Aoo-前后端全框架功能通信审计任务清单.md')

patterns = {
  'process' => /(?:Runtime\.getRuntime\(\)\.exec|ProcessBuilder|child_process|Open3\.|system\s*\(|`[^`]+`)/,
  'native' => /(?:System\.loadLibrary|System\.load\s*\(|\bnative\s+\w+|JNIEnv|ffi-napi|node-gyp)/,
  'shell' => /(?:\bsh\s+-c\b|\bbash\s+-c\b|\bzsh\s+-c\b|\.sh\b)/,
  'ruby' => /(?:\bruby\b|\.rb\b)/,
  'python' => /(?:\bpython3?\b|\.py\b)/,
  'node' => /(?:\bnode\b|\bnpx\b|\.m?js\b)/
}.freeze

entries = []
baseline.fetch('files').each do |item|
  path = item.fetch('path')
  next unless File.file?(path)
  next if item.fetch('size') > 2_000_000
  ext = File.extname(path).downcase
  next unless %w[.java .kt .groovy .xml .yml .yaml .properties .sh .rb .py .js .mjs .cjs .ts .json].include?(ext)
  text = File.binread(path).force_encoding('UTF-8').scrub
  matched = patterns.each_with_object([]) { |(kind, regex), found| found << kind if text.match?(regex) }
  next if matched.empty?
  lines = []
  text.lines.each_with_index do |line, index|
    kinds = patterns.each_with_object([]) { |(kind, regex), found| found << kind if line.match?(regex) }
    lines << {line: index + 1, kinds: kinds, excerpt: line.strip[0, 240]} unless kinds.empty?
    break if lines.size >= 30
  end
  entries << {path: path.delete_prefix(root + '/'), kinds: matched, matches: lines}
end

summary = patterns.keys.to_h { |kind| [kind, entries.count { |entry| entry[:kinds].include?(kind) }] }
report = {schemaVersion: 1, generatedAt: Time.now.utc.iso8601, summary: summary,
          limitations: ['Static inventory only; production runtime invocation and OS-level command tracing remain required.'],
          entries: entries.sort_by { |entry| entry[:path] }}
File.write(output, JSON.pretty_generate(report) + "\n", mode: 'w:UTF-8')

task = File.read(task_path, encoding: 'UTF-8')
detail = summary.map { |kind, count| "#{kind}=#{count}" }.join('、')
row = "| CALL06 | 未完成 | 已审计·待运行捕获 | 已统一盘点外部进程、JNI/本地库及 Shell/Ruby/Python/Node 调用（#{detail}）；静态入口已归档，生产运行调用轨迹尚待捕获。 证据：work/audit/external-tool-call-inventory.json |"
task.sub!(/^\| CALL06 \|.*$/, row) or abort 'CALL06 row not found'
task.sub!(/^下一项：.*$/, '下一项：CALL07 HTTP/WSS/MQ/DNS 调用清单') or abort 'next pointer not found'
File.write(task_path, task, mode: 'w:UTF-8')
puts JSON.generate(summary)
