#!/usr/bin/env ruby
# frozen_string_literal: true

require "digest"
require "json"

ROOT = File.expand_path("../..", __dir__)
baseline_path = ENV.fetch("R63_BASELINE", File.join(__dir__, "legacy_protocols.txt"))
ledger_path = ENV.fetch("R63_LEDGER", File.join(__dir__, "ledger.json"))
errors = []
baseline = File.readlines(baseline_path, chomp: true).reject(&:empty?)
ledger = JSON.parse(File.read(ledger_path))
rows = ledger.fetch("rows")

errors << "baseline must contain 283 protocols (got #{baseline.length})" unless baseline.length == 283
errors << "baseline contains duplicates" unless baseline.uniq.length == baseline.length
errors << "ledger must contain 283 rows (got #{rows.length})" unless rows.length == 283
errors << "baseline digest mismatch" unless Digest::SHA256.hexdigest(File.read(baseline_path)) == ledger["baselineSha256"]

mapped = rows.map { |row| row["legacyProtocol"] }
errors.concat((baseline - mapped).map { |p| "unmapped legacy protocol: #{p}" })
errors.concat(mapped.group_by(&:itself).select { |_p, values| values.length > 1 }.keys.map { |p| "duplicate legacy mapping: #{p}" })
entries = rows.map { |row| row["newEntry"] }
errors.concat(entries.group_by(&:itself).select { |_e, values| values.length > 1 }.keys.map { |e| "duplicate new entry: #{e}" })

rows.each do |row|
  %w[id legacyProtocol businessDomain newEntry status evidence reason assemblyMarker].each do |field|
    errors << "#{row['id'] || row['legacyProtocol']}: missing #{field}" if row[field].nil? || row[field].respond_to?(:empty?) && row[field].empty?
  end
  errors << "#{row['id']}: unsupported status #{row['status']}" unless %w[replaced retained deprecated].include?(row["status"])
  row.fetch("evidence", []).each do |relative|
    path = File.join(ROOT, relative)
    errors << "#{row['id']}: evidence missing: #{relative}" unless File.file?(path)
  end
  assembly = File.join(ROOT, row.fetch("evidence", []).last.to_s)
  marker = row["assemblyMarker"].to_s
  unless File.file?(assembly) && File.read(assembly).include?(marker)
    errors << "#{row['id']}: no production assembly marker #{marker.inspect} in #{row.fetch('evidence', []).last}"
  end
end

if errors.empty?
  puts JSON.generate("passed" => true, "mapped" => rows.length, "unmapped" => 0, "duplicates" => 0, "unassembled" => 0)
  exit 0
end
warn errors.join("\n")
warn JSON.generate("passed" => false, "errors" => errors.length)
exit 1
