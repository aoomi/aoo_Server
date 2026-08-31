#!/usr/bin/env ruby
# frozen_string_literal: true

require 'json'
require 'time'

root = File.expand_path('..', __dir__)
source_path = File.join(root, 'work/audit/state-transition-equivalence.json')
output_path = File.join(root, 'work/audit/error-behavior-equivalence.json')
task_path = File.join(root, 'docs/Aoo-前后端全框架功能通信审计任务清单.md')
source = JSON.parse(File.read(source_path, encoding: 'UTF-8'))

error_pattern = /(?:throw|reject|deny|forbid|illegal|error|fail|exception|code|msg|message|错误|失败|拒绝)/i
unsafe_pattern = /(?:SQLException|NullPointerException|stackTrace|\.getStackTrace|jdbc:|\b(?:\d{1,3}\.){3}\d{1,3}:\d+\b|at\s+[a-z0-9_.]+\([A-Za-z0-9_.]+:\d+\))/i
symbol_pattern = /['"]([A-Z][A-Z0-9_]{2,80})['"]/
number_pattern = /(?:code|errorCode)\s*[:=(,]\s*(-?\d{1,6})/i

def collect(rows, error_pattern, unsafe_pattern, symbol_pattern, number_pattern)
  rows.each_with_object([]) do |row, result|
    excerpt = row.fetch('excerpt', '')
    next unless excerpt.match?(error_pattern)
    symbols = excerpt.scan(symbol_pattern).flatten.uniq
    numbers = excerpt.scan(number_pattern).flatten.map(&:to_i).uniq
    result << {
      path: row['path'], line: row['line'], symbols: symbols, numericCodes: numbers,
      unsafeDetail: excerpt.match?(unsafe_pattern), excerpt: excerpt
    }
  end
end

legacy = collect(source.fetch('legacyTransitions'), error_pattern, unsafe_pattern, symbol_pattern, number_pattern)
current = collect(source.fetch('currentTransitions'), error_pattern, unsafe_pattern, symbol_pattern, number_pattern)
legacy_symbols = legacy.flat_map { |row| row[:symbols] }.uniq.sort
current_symbols = current.flat_map { |row| row[:symbols] }.uniq.sort
legacy_numbers = legacy.flat_map { |row| row[:numericCodes] }.uniq.sort
current_numbers = current.flat_map { |row| row[:numericCodes] }.uniq.sort
summary = {
  legacyErrorSites: legacy.size, currentErrorSites: current.size,
  legacySymbolicCodes: legacy_symbols.size, currentSymbolicCodes: current_symbols.size,
  mappedSymbolicCodes: (legacy_symbols & current_symbols).size,
  unmappedLegacySymbolicCodes: (legacy_symbols - current_symbols).size,
  legacyNumericCodes: legacy_numbers.size, currentNumericCodes: current_numbers.size,
  unsafeLegacySites: legacy.count { |row| row[:unsafeDetail] },
  unsafeCurrentSites: current.count { |row| row[:unsafeDetail] }
}
report = {
  schemaVersion: 1, generatedAt: Time.now.utc.iso8601,
  derivedFrom: source_path.delete_prefix(root + '/'), summary: summary,
  symbolicCodeDiff: {legacyOnly: legacy_symbols - current_symbols, common: legacy_symbols & current_symbols, currentOnly: current_symbols - legacy_symbols},
  numericCodeDiff: {legacyOnly: legacy_numbers - current_numbers, common: legacy_numbers & current_numbers, currentOnly: current_numbers - legacy_numbers},
  legacyErrors: legacy, currentErrors: current,
  limitations: ['Static extraction cannot prove user-facing semantic equivalence; request/response fixtures must assert code, safe message and state invariants.']
}
File.write(output_path, JSON.pretty_generate(report) + "\n", mode: 'w:UTF-8')

task = File.read(task_path, encoding: 'UTF-8')
row = "| EQ06 | 未完成 | 已建差异·待语义闭合 | 已抽取旧新错误站点、符号/数字错误码及不安全细节；旧/新错误站点=#{summary[:legacyErrorSites]}/#{summary[:currentErrorSites]}、旧/新符号码=#{summary[:legacySymbolicCodes]}/#{summary[:currentSymbolicCodes]}、旧未映射符号码=#{summary[:unmappedLegacySymbolicCodes]}、当前不安全细节候选=#{summary[:unsafeCurrentSites]}；尚需逐接口断言新错误码与安全提示保留用户可理解语义。 证据：work/audit/error-behavior-equivalence.json；工具：tools/audit_error_behavior_equivalence.rb |"
task.sub!(/^\| EQ06 \|.*$/, row) or abort 'EQ06 row not found'
task.sub!(/^下一项：.*$/, '下一项：EQ07 交互与广播时序等价') or abort 'next pointer not found'
File.write(task_path, task, mode: 'w:UTF-8')
puts JSON.generate(summary)
