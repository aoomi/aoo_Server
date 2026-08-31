#!/usr/bin/env ruby
# frozen_string_literal: true

require 'json'
require 'time'
require 'fileutils'
require 'benchmark'

root = File.expand_path('..', __dir__)
fixtures_dir = ENV.fetch('AOO_SERIALIZATION_FIXTURES', File.join(root, 'work/fixtures/serialization'))
output = File.join(root, 'work/audit/room-serialization-benchmark.json')
iterations = Integer(ENV.fetch('AOO_SERIALIZATION_ITERATIONS', '200'))

rows = Dir.glob(File.join(fixtures_dir, '*.json')).map do |path|
  value = JSON.parse(File.read(path, encoding: 'UTF-8'))
  encoded = JSON.generate(value)
  serialize_samples = []
  parse_samples = []
  GC.start
  allocations_before = GC.stat[:total_allocated_objects]
  iterations.times do
    serialize_samples << Benchmark.realtime { JSON.generate(value) } * 1_000_000
    parse_samples << Benchmark.realtime { JSON.parse(encoded) } * 1_000_000
  end
  allocations = GC.stat[:total_allocated_objects] - allocations_before
  percentile = lambda do |samples, p|
    sorted = samples.sort
    sorted[[((sorted.size - 1) * p).round, sorted.size - 1].min]
  end
  {fixture: path.delete_prefix(root + '/'), bytes: encoded.bytesize, iterations: iterations,
   serializeOpsPerSecond: iterations / (serialize_samples.sum / 1_000_000.0),
   parseOpsPerSecond: iterations / (parse_samples.sum / 1_000_000.0),
   serializeP50Micros: percentile.call(serialize_samples, 0.50), serializeP99Micros: percentile.call(serialize_samples, 0.99),
   parseP50Micros: percentile.call(parse_samples, 0.50), parseP99Micros: percentile.call(parse_samples, 0.99),
   allocatedObjectsPerRoundTrip: allocations.to_f / iterations}
end

compatibility_pairs = rows.group_by { |row| File.basename(row[:fixture]).sub(/-(?:legacy|current)\.json$/, '') }
                         .count { |_key, group| group.size >= 2 }
summary = {fixtures: rows.size, compatibilityPairs: compatibility_pairs,
           throughputMeasured: rows.any?, allocationMeasured: rows.any?, latencyMeasured: rows.any?,
           passed: rows.any? && compatibility_pairs.positive?}
report = {schemaVersion: 1, generatedAt: Time.now.utc.iso8601,
          invariant: 'Real snapshot/event fixtures have throughput, allocation and p50/p99 latency baselines plus legacy/current compatibility pairs.',
          summary: summary, results: rows, runtime: RUBY_DESCRIPTION,
          limitation: 'Ruby harness validates fixture availability and comparison workflow; final release gate must run equivalent Java 26/JMH codecs.'}
FileUtils.mkdir_p(File.dirname(output))
File.write(output, JSON.pretty_generate(report) + "\n", mode: 'w:UTF-8')
puts JSON.generate(summary)
exit(summary[:passed] ? 0 : 2) if ENV['AOO_ENFORCE_SERIALIZATION_GATE'] == '1'
