#!/usr/bin/env ruby
require 'json'
require 'digest'
require 'time'
require 'fileutils'

ROOT = File.expand_path('..', __dir__)
OUTPUT = File.join(ROOT, 'docs/generated/logreal05-repeated-errors.json')
RULES = [
  [/redis.*(?:timeout|time \d+ ms|connection)/i, ['redis-latency', 'platform-data', 'integration-test']],
  [/(?:jdbc|mysql|sql|database).*(?:error|exception|failed|timeout)/i, ['database-access', 'platform-data', 'integration-test']],
  [/(?:websocket|socket|connection).*(?:closed|refused|reset|failed|timeout)/i, ['network-lifecycle', 'gateway', 'gateway-test']],
  [/(?:authority|snapshot|fencing|eventseq).*(?:error|exception|failed|timeout)/i, ['authority-state', 'game-runtime', 'authority-regression']],
  [/(?:nullpointer|cannot read propert|indexoutofbounds)/i, ['programming-defect', 'module-owner', 'unit-test']],
  [/(?:error|exception|failed)/i, ['unclassified-runtime-error', 'observability', 'reproduction-required']]
].freeze

normalize = lambda do |line|
  line.gsub(/[0-9a-f]{8}-[0-9a-f-]{27,}/i, '<uuid>')
      .gsub(/\b\d{1,3}(?:\.\d{1,3}){3}(?::\d+)?\b/, '<ip>')
      .gsub(/\b\d{4}-\d{2}-\d{2}[T ][0-9:.+Z-]+/, '<time>')
      .gsub(/\b\d+\b/, '<n>').gsub(/\s+/, ' ').strip
end

rows = []
Dir.glob(File.join(ROOT, 'logs/**/*')).select { |path| File.file?(path) }.sort.each do |file|
  File.foreach(file, encoding: 'UTF-8', invalid: :replace, undef: :replace) do |line|
    next unless line.match?(/\b(?:fatal|error|exception|failed|severe)\b/i)
    signature = normalize.call(line)
    rule = RULES.find { |regex, _| signature.match?(regex) }
    classification = rule ? rule[1] : ['unclassified-runtime-error', 'observability', 'reproduction-required']
    rows << { signature: signature, fingerprint: Digest::SHA256.hexdigest(signature)[0, 16],
              file: file.delete_prefix(ROOT + '/'), rootCauseClass: classification[0],
              owner: classification[1], regressionGate: classification[2] }
  end
end

groups = rows.group_by { |row| row[:fingerprint] }.map do |fingerprint, items|
  first = items.first
  { fingerprint: fingerprint, count: items.length, signature: first[:signature],
    rootCauseClass: first[:rootCauseClass], owner: first[:owner], regressionGate: first[:regressionGate],
    files: items.map { |item| item[:file] }.uniq.sort }
end.sort_by { |row| [-row[:count], row[:fingerprint]] }

report = { schemaVersion: 1, generatedAt: Time.now.utc.iso8601,
  summary: { errorLines: rows.length, uniqueFingerprints: groups.length,
             repeatedFingerprints: groups.count { |row| row[:count] > 1 } },
  policy: 'Fingerprint aggregation is diagnostic. Every group is assigned a root-cause class, owner and regression gate; reproduction-required groups cannot close a defect by log evidence alone.',
  groups: groups }
FileUtils.mkdir_p(File.dirname(OUTPUT))
File.write(OUTPUT, JSON.pretty_generate(report) + "\n")
puts "LOGREAL05 PASS: #{groups.length} fingerprints, #{report[:summary][:repeatedFingerprints]} repeated"
