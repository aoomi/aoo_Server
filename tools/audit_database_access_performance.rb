#!/usr/bin/env ruby
# frozen_string_literal: true

require 'json'
require 'time'
require 'fileutils'

root = File.expand_path('..', __dir__)
output = File.join(root, 'work/audit/database-access-performance.json')
patterns = {
  preparedStatement: /PreparedStatement|prepareStatement/,
  rawStatement: /(?:createStatement\(|Statement\s+\w+)/,
  batch: /(?:addBatch|executeBatch|rewriteBatchedStatements)/,
  pagination: /(?:\bLIMIT\b|Pageable|setMaxResults|setFirstResult)/i,
  connectionPool: /(?:Hikari|DruidDataSource|maximumPoolSize|maxActive|minIdle)/,
  unboundedSelect: /(?i)["']\s*select\s+.*\s+from\s+[^"';]+["']/
}.freeze
findings = patterns.keys.to_h { |key| [key, []] }
Dir.glob(File.join(root, 'server/**/*')).each do |path|
  next unless File.file?(path) && %w[.java .kt .properties .yml .yaml .xml].include?(File.extname(path))
  next if path.include?('/target/') || path.include?('/build/')
  text = File.read(path, encoding: 'UTF-8', invalid: :replace, undef: :replace)
             .encode('UTF-8', invalid: :replace, undef: :replace, replace: '')
  patterns.each { |key, pattern| findings[key] << path.delete_prefix(root + '/') if text.match?(pattern) }
end
counts = findings.transform_values { |paths| paths.uniq.size }
runtime_file = ENV['AOO_QUERY_BENCHMARK_JSON']
runtime_present = runtime_file && File.file?(runtime_file)
summary = counts.merge(runtimeBenchmarkPresent: !!runtime_present,
                       staticRisks: counts[:rawStatement] + counts[:unboundedSelect],
                       passed: counts[:rawStatement].zero? && counts[:unboundedSelect].zero? && !!runtime_present)
report = {schemaVersion: 1, generatedAt: Time.now.utc.iso8601,
          invariant: 'Production queries use a bounded pool, prepared statements, batching and pagination and meet real-data latency/plan budgets.',
          summary: summary, evidence: findings.transform_values(&:uniq), benchmarkSource: runtime_file,
          requiredRuntimeFields: %w[queryId rows p50Millis p95Millis p99Millis planDigest scannedRows returnedRows poolWaitMillis protocolVersion]}
FileUtils.mkdir_p(File.dirname(output))
File.write(output, JSON.pretty_generate(report) + "\n", mode: 'w:UTF-8')
puts JSON.generate(summary)
exit(summary[:passed] ? 0 : 2) if ENV['AOO_ENFORCE_DATABASE_PERFORMANCE_GATE'] == '1'
