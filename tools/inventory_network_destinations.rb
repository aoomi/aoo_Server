#!/usr/bin/env ruby
# frozen_string_literal: true

require 'json'
require 'time'
require 'uri'

root = File.expand_path('..', __dir__)
baseline_path = File.join(root, 'work/audit/audit-source-baseline.json')
output_path = File.join(root, 'work/audit/network-destination-inventory.json')
task_path = File.join(root, 'docs/Aoo-前后端全框架功能通信审计任务清单.md')
baseline = JSON.parse(File.read(baseline_path, encoding: 'UTF-8'))

kind_patterns = {
  'http' => /(?:https?:\/\/|HttpClient|OkHttp|RestTemplate|WebClient|URLConnection|axios\.|fetch\s*\()/i,
  'websocket' => /(?:wss?:\/\/|WebSocket|SocketIO|Netty.*WebSocket)/i,
  'mq' => /(?:RocketMQ|Kafka|RabbitMQ|JmsTemplate|MessageQueue|Producer|Consumer|topic\b)/i,
  'dns' => /(?:InetAddress|getaddrinfo|dns\.|DnsResolver|resolveHost|lookup\s*\()/i
}.freeze
url_pattern = %r{\b(?:https?|wss?)://[^\s"'<>]+}i
host_pattern = /\b(?:localhost|(?:\d{1,3}\.){3}\d{1,3}|[a-z0-9](?:[a-z0-9.-]{0,251}[a-z0-9])?\.[a-z]{2,63})(?::\d{1,5})?\b/i

entries = []
baseline.fetch('files').each do |item|
  path = item.fetch('path')
  next unless File.file?(path)
  next if item.fetch('size') > 2_000_000
  next unless %w[.java .kt .xml .yml .yaml .properties .json .ts .js .mjs .cjs .sh .rb .py].include?(File.extname(path).downcase)

  text = File.binread(path).force_encoding('UTF-8').scrub
  kinds = kind_patterns.each_with_object([]) { |(kind, regex), found| found << kind if text.match?(regex) }
  next if kinds.empty?
  urls = text.scan(url_pattern).map { |url| url.sub(/[),;]+\z/, '') }.uniq.sort.first(100)
  hosts = text.scan(host_pattern).map { |match| match.is_a?(Array) ? match.first : match }.compact.uniq.sort.first(100)
  lines = []
  text.lines.each_with_index do |line, index|
    line_kinds = kind_patterns.each_with_object([]) { |(kind, regex), found| found << kind if line.match?(regex) }
    next if line_kinds.empty?
    lines << {line: index + 1, kinds: line_kinds, excerpt: line.strip[0, 300]}
    break if lines.size >= 40
  end
  entries << {path: path.delete_prefix(root + '/'), kinds: kinds, urls: urls, hosts: hosts, matches: lines}
end

summary = kind_patterns.keys.to_h { |kind| [kind, entries.count { |entry| entry[:kinds].include?(kind) }] }
summary['literalUrls'] = entries.sum { |entry| entry[:urls].size }
summary['literalHosts'] = entries.sum { |entry| entry[:hosts].size }
report = {
  schemaVersion: 1,
  generatedAt: Time.now.utc.iso8601,
  summary: summary,
  limitations: [
    'Static inventory cannot prove runtime DNS resolution or environment-injected destinations.',
    'Runtime egress tracing and allow-list comparison remain required before closure.'
  ],
  entries: entries.sort_by { |entry| entry[:path] }
}
File.write(output_path, JSON.pretty_generate(report) + "\n", mode: 'w:UTF-8')

task = File.read(task_path, encoding: 'UTF-8')
detail = summary.map { |kind, count| "#{kind}=#{count}" }.join('、')
row = "| CALL07 | 未完成 | 已审计·待运行捕获 | 已归档 HTTP/WSS/MQ/DNS 静态出站入口、字面 URL 与主机目标（#{detail}）；环境注入目标、运行时 DNS 解析及出站白名单差异尚待捕获。 证据：work/audit/network-destination-inventory.json |"
task.sub!(/^\| CALL07 \|.*$/, row) or abort 'CALL07 row not found'
task.sub!(/^下一项：.*$/, '下一项：CALL08 前端 Bundle/Prefab/动态加载图') or abort 'next pointer not found'
File.write(task_path, task, mode: 'w:UTF-8')
puts JSON.generate(summary)
