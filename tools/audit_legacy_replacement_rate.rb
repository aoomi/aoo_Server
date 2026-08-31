#!/usr/bin/env ruby
# frozen_string_literal: true

require 'json'
require 'time'
require 'fileutils'

root = File.expand_path('..', __dir__)
source_path = File.join(root, 'work/audit/legacy-entry-equivalence.json')
output = File.join(root, 'work/audit/legacy-replacement-rate.json')
source = JSON.parse(File.read(source_path, encoding: 'UTF-8'))

rows = source.fetch('matrix').map do |entry|
  targets = entry.fetch('targets', [])
  production_targets = targets.reject do |target|
    path = target.fetch('path', '')
    path.include?('CompatibilityApp/') || File.basename(path).start_with?('Legacy') || path.include?('/Legacy')
  end
  status = if production_targets.any?
             'NEW_CLOSURE_CANDIDATE'
           elsif targets.any?
             'COMPATIBILITY_ONLY'
           else
             'UNMAPPED'
           end
  entry.merge('replacementStatus' => status, 'productionTargets' => production_targets,
              'approvedDeprecated' => false)
end

counts = rows.group_by { |row| row['replacementStatus'] }.transform_values(&:size)
total = rows.size
closed = counts.fetch('NEW_CLOSURE_CANDIDATE', 0)
summary = {
  legacyEntries: total,
  newClosureCandidates: closed,
  compatibilityOnly: counts.fetch('COMPATIBILITY_ONLY', 0),
  unmapped: counts.fetch('UNMAPPED', 0),
  approvedDeprecated: 0,
  replacementRate: total.zero? ? 0.0 : (closed.to_f / total).round(6),
  passed: total.positive? && closed == total
}
report = {
  schemaVersion: 1,
  generatedAt: Time.now.utc.iso8601,
  invariant: 'Every 2.22 function maps to a proven new end-to-end closure or an explicitly approved deprecation.',
  summary: summary,
  matrix: rows,
  limitation: 'A source target is only a closure candidate; runtime request/state/persistence/broadcast evidence is still required before final approval.'
}
FileUtils.mkdir_p(File.dirname(output))
File.write(output, JSON.pretty_generate(report) + "\n", mode: 'w:UTF-8')
puts JSON.generate(summary)
exit(summary[:passed] ? 0 : 2) if ENV['AOO_ENFORCE_REPLACEMENT_GATE'] == '1'
