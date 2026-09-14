#!/usr/bin/env ruby
# frozen_string_literal: true

require 'json'
require 'time'

root = File.expand_path('..', __dir__)
static_path = File.join(root, 'work/audit/static-dependency-graph.json')
output_path = File.join(root, 'work/audit/static-runtime-coverage-diff.json')
task_path = File.join(root, 'docs/Aoo-前后端全框架功能通信审计任务清单.md')
abort "missing static graph: #{static_path}" unless File.file?(static_path)

static_graph = JSON.parse(File.read(static_path, encoding: 'UTF-8'))
runtime_candidates = Dir.glob(File.join(root, 'work/audit/**/*'), File::FNM_DOTMATCH)
                        .select { |path| File.file?(path) && File.basename(path).match?(/(?:runtime|coverage|trace)/i) }
                        .reject { |path| [static_path, output_path].include?(path) }

def collect_strings(value, result = [])
  case value
  when Hash
    value.each_value { |child| collect_strings(child, result) }
  when Array
    value.each { |child| collect_strings(child, result) }
  when String
    result << value
  end
  result
end

static_symbols = collect_strings(static_graph).select do |value|
  value.match?(%r{(?:\A|[.$#/])(?:[A-Z][A-Za-z0-9_$]*|[a-z][A-Za-z0-9_$]*(?:\.[A-Za-z0-9_$]+)+)\z})
end.uniq.sort

runtime_symbols = []
runtime_sources = []
runtime_candidates.each do |path|
  next if File.size(path) > 50_000_000
  relative = path.delete_prefix(root + '/')
  begin
    content = File.binread(path).force_encoding('UTF-8').scrub
    values = if File.extname(path).downcase == '.json'
               collect_strings(JSON.parse(content))
             else
               content.scan(%r{[A-Za-z_$][A-Za-z0-9_$]*(?:[.$#/][A-Za-z_$][A-Za-z0-9_$]*)+})
             end
    runtime_symbols.concat(values)
    runtime_sources << relative
  rescue JSON::ParserError
    next
  end
end
runtime_symbols = runtime_symbols.uniq.sort

covered = static_symbols & runtime_symbols
static_only = static_symbols - runtime_symbols
runtime_only = runtime_symbols - static_symbols
runtime_trace_present = runtime_sources.any? { |path| path.match?(/(?:jacoco|jfr|opentelemetry|runtime-trace|coverage\.exec)/i) }
summary = {
  staticSymbols: static_symbols.size,
  runtimeSymbols: runtime_symbols.size,
  coveredStaticSymbols: covered.size,
  staticOnlySymbols: static_only.size,
  runtimeOnlySymbols: runtime_only.size,
  authoritativeRuntimeTracePresent: runtime_trace_present
}
report = {
  schemaVersion: 1,
  generatedAt: Time.now.utc.iso8601,
  summary: summary,
  runtimeSources: runtime_sources.sort,
  coveredStaticSymbols: covered.first(20_000),
  staticOnlySymbols: static_only.first(20_000),
  runtimeOnlySymbols: runtime_only.first(20_000),
  limitations: [
    'Only JaCoCo/JFR/OpenTelemetry/runtime-trace evidence is treated as authoritative runtime coverage.',
    'Inventory files can seed symbol comparison but cannot prove execution.'
  ]
}
File.write(output_path, JSON.pretty_generate(report) + "\n", mode: 'w:UTF-8')

task = File.read(task_path, encoding: 'UTF-8')
row = "| CALL10 | 未完成 | 已建设·待运行轨迹 | 已建立静态调用图与运行覆盖自动差异工具；当前静态符号=#{summary[:staticSymbols]}、覆盖=#{summary[:coveredStaticSymbols]}、仅静态=#{summary[:staticOnlySymbols]}、仅运行=#{summary[:runtimeOnlySymbols]}，尚无权威 JaCoCo/JFR/OTel 运行轨迹，不得闭合。 证据：work/audit/static-runtime-coverage-diff.json；工具：tools/compare_static_runtime_coverage.rb |"
task.sub!(/^\| CALL10 \|.*$/, row) or abort 'CALL10 row not found'
task.sub!(/^下一项：.*$/, '下一项：CALL11 新生产路径旧接口可达性整改') or abort 'next pointer not found'
File.write(task_path, task, mode: 'w:UTF-8')
puts JSON.generate(summary)
