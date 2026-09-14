#!/usr/bin/env ruby
# frozen_string_literal: true

require 'json'
require 'time'

root = File.expand_path('..', __dir__)
client_root = File.expand_path('../Client', root)
baseline = JSON.parse(File.read(File.join(root, 'work/audit/audit-source-baseline.json'), encoding: 'UTF-8'))
output_path = File.join(root, 'work/audit/client-dynamic-load-graph.json')
task_path = File.join(root, 'docs/Aoo-前后端全框架功能通信审计任务清单.md')

patterns = {
  'bundle' => /(?:assetManager\.)?loadBundle\s*\(\s*['"]([^'"]+)['"]/,
  'resource' => /(?:resources|bundle)\.load(?:Dir|Any)?\s*\(\s*['"]([^'"]+)['"]/,
  'scene' => /(?:director\.)?loadScene\s*\(\s*['"]([^'"]+)['"]/,
  'prefab' => /(?:instantiate|Prefab|NodePool)\b/,
  'dynamicImport' => /import\s*\(\s*['"]([^'"]+)['"]\s*\)/,
  'componentLookup' => /getComponent(?:InChildren)?\s*\(\s*['"]([^'"]+)['"]/
}.freeze

client_files = baseline.fetch('files').select { |item| item.fetch('path').start_with?(client_root + '/') }
bundle_roots = []
client_files.select { |item| item.fetch('path').end_with?('.meta') && item.fetch('size') <= 1_000_000 }.each do |item|
  text = File.binread(item.fetch('path')).force_encoding('UTF-8').scrub
  next unless text.match?(/"isBundle"\s*:\s*true/)
  bundle_roots << item.fetch('path').delete_suffix('.meta').delete_prefix(client_root + '/')
end

nodes = []
unresolved_dynamic = []
dynamic_call_patterns = {
  'bundle' => /(?:assetManager\.)?loadBundle\s*\(/,
  'resource' => /(?:resources|bundle)\.load(?:Dir|Any)?\s*\(/,
  'scene' => /(?:director\.)?loadScene\s*\(/,
  'dynamicImport' => /import\s*\(/,
  'componentLookup' => /getComponent(?:InChildren)?\s*\(/
}.freeze
client_files.each do |item|
  path = item.fetch('path')
  next unless %w[.ts .js .mjs .cjs].include?(File.extname(path).downcase)
  next unless File.file?(path) && item.fetch('size') <= 2_000_000
  text = File.binread(path).force_encoding('UTF-8').scrub
  edges = []
  text.lines.each_with_index do |line, index|
    dynamic_call_patterns.each do |kind, call_regex|
      next unless line.match?(call_regex)
      literal_regex = patterns.fetch(kind)
      next if line.match?(literal_regex)
      unresolved_dynamic << {path: path.delete_prefix(client_root + '/'), line: index + 1, kind: kind,
                             excerpt: line.strip[0, 300]}
    end
    patterns.each do |kind, regex|
      match = line.match(regex)
      next unless match
      target = match.captures.compact.first
      edge = {kind: kind, line: index + 1, target: target, excerpt: line.strip[0, 300]}
      edges << edge
    end
  end
  nodes << {path: path.delete_prefix(client_root + '/'), edges: edges} unless edges.empty?
end

summary = patterns.keys.to_h do |kind|
  [kind, nodes.sum { |node| node[:edges].count { |edge| edge[:kind] == kind } }]
end
summary['bundleRoots'] = bundle_roots.size
summary['filesWithLoadEdges'] = nodes.size
summary['unresolvedDynamicExpressions'] = unresolved_dynamic.size
report = {
  schemaVersion: 1,
  generatedAt: Time.now.utc.iso8601,
  summary: summary,
  bundleRoots: bundle_roots.sort,
  nodes: nodes.sort_by { |node| node[:path] },
  unresolvedDynamicExpressions: unresolved_dynamic,
  limitations: [
    'Static graph cannot resolve computed bundle/resource/component names.',
    'Successful and failed runtime load paths still require browser and Creator instrumentation.'
  ]
}
File.write(output_path, JSON.pretty_generate(report) + "\n", mode: 'w:UTF-8')

task = File.read(task_path, encoding: 'UTF-8')
detail = summary.map { |kind, count| "#{kind}=#{count}" }.join('、')
row = "| CALL08 | 未完成 | 已审计·待运行捕获 | 已建立前端 Bundle/Resource/Scene/Prefab/动态 import/组件查找静态加载图（#{detail}）；计算路径与成功/失败运行轨迹尚待 Creator 及网页仪表化闭合。 证据：work/audit/client-dynamic-load-graph.json |"
task.sub!(/^\| CALL08 \|.*$/, row) or abort 'CALL08 row not found'
task.sub!(/^下一项：.*$/, '下一项：CALL09 JDBC/Redis/存储调用归属映射') or abort 'next pointer not found'
File.write(task_path, task, mode: 'w:UTF-8')
puts JSON.generate(summary)
