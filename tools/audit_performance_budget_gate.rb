#!/usr/bin/env ruby
# frozen_string_literal: true

require 'json'
require 'time'
require 'fileutils'

root = File.expand_path('..', __dir__)
budget_path = File.join(root, 'docs/performance-budgets.json')
candidate_path = ENV['AOO_CANDIDATE_PERFORMANCE_JSON']
output = File.join(root, 'work/audit/performance-budget-gate.json')
budgets = JSON.parse(File.read(budget_path, encoding: 'UTF-8'))
candidate = candidate_path && File.file?(candidate_path) ? JSON.parse(File.read(candidate_path, encoding: 'UTF-8')) : {}
rows = budgets.fetch('metrics').map do |metric, rule|
  value = candidate[metric]
  limit_kind = rule.key?('max') ? 'max' : 'min'
  limit = rule[limit_kind]
  passed = value.is_a?(Numeric) && limit.is_a?(Numeric) && (limit_kind == 'max' ? value <= limit : value >= limit)
  {metric: metric, value: value, limitKind: limit_kind, limit: limit, passed: passed}
end
summary = {approvedBudgets: budgets['approved'] == true, candidatePresent: !candidate.empty?,
           metrics: rows.size, passedMetrics: rows.count { |row| row[:passed] },
           failedMetrics: rows.count { |row| !row[:passed] },
           passed: budgets['approved'] == true && !candidate.empty? && rows.all? { |row| row[:passed] }}
report = {schemaVersion: 1, generatedAt: Time.now.utc.iso8601,
          invariant: 'A dependency upgrade cannot ship unless startup, memory, throughput, p99 latency and package size remain inside approved budgets.',
          summary: summary, candidateSource: candidate_path, results: rows}
FileUtils.mkdir_p(File.dirname(output))
File.write(output, JSON.pretty_generate(report) + "\n", mode: 'w:UTF-8')
puts JSON.generate(summary)
exit(summary[:passed] ? 0 : 2) if ENV['AOO_ENFORCE_PERFORMANCE_BUDGET_GATE'] == '1'
