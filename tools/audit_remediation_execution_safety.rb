#!/usr/bin/env ruby
# frozen_string_literal: true

require 'fileutils'
require 'digest'
require 'json'
require 'time'

ROOT = File.expand_path('..', __dir__)
AUDIT = File.join(ROOT, 'work', 'remediation-audit.jsonl')
OUTPUT = File.join(ROOT, 'work', 'audit', 'remediation-execution-safety.json')
FORBIDDEN = [
  /git\s+reset\s+--hard/,
  /git\s+checkout\s+--/,
  /rm\s+-[^\s]*r[^\s]*f\s+\/(?:\s|$)/,
  /DROP\s+(?:DATABASE|SCHEMA|TABLE)/i,
  /TRUNCATE\s+TABLE/i,
  /kubectl\s+delete\s+(?:namespace|pvc)/,
  /terraform\s+destroy/
].freeze

records = File.file?(AUDIT) ? File.readlines(AUDIT, encoding: 'UTF-8').map do |line|
  JSON.parse(line)
rescue JSON::ParserError
  { 'invalidRecord' => true, 'rawSha256' => Digest::SHA256.hexdigest(line) }
end : []

unsafe = records.select do |record|
  command = Array(record['command']).join(' ')
  FORBIDDEN.any? { |pattern| command.match?(pattern) }
end
failures = records.reject { |record| record['success'] == true }
report = {
  generatedAt: Time.now.utc.iso8601,
  source: AUDIT.delete_prefix(ROOT + '/'),
  policy: {
    ordinaryFailure: 'record failure and continue with the next serial task',
    humanCheck: 'defer to the final manual queue',
    irreversibleOperation: 'reject before execution',
    concurrentChange: 'stop only when externally modified files overlap the current write set'
  },
  executionRecords: records.length,
  recordedFailures: failures.length,
  unsafeExecutions: unsafe.length,
  passed: unsafe.empty?,
  failures: failures,
  unsafe: unsafe
}
FileUtils.mkdir_p(File.dirname(OUTPUT))
File.write(OUTPUT, JSON.pretty_generate(report) + "\n")
puts JSON.generate(records: records.length, failures: failures.length,
                   unsafeExecutions: unsafe.length, passed: report[:passed])
exit(report[:passed] ? 0 : 1)
