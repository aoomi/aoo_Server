#!/usr/bin/env ruby
# frozen_string_literal: true

require 'json'
require 'time'
require 'fileutils'

root = File.expand_path('..', __dir__)
evidence_dir = ENV.fetch('AOO_SYNC_EVIDENCE_DIR', File.join(root, 'work/evidence/sync'))
output = File.join(root, 'work/audit/sync-evidence-package-audit.json')
required = %w[traceId roomId playVersion eventSeq eventType beforeDigest message afterDigest uiAssertions].freeze
rows = Dir.glob(File.join(evidence_dir, '*.json')).map do |path|
  begin
    data = JSON.parse(File.read(path, encoding: 'UTF-8'))
    missing = required.reject { |key| data.key?(key) }
    assertions = data['uiAssertions']
    assertions_valid = assertions.is_a?(Array) && !assertions.empty? && assertions.all? { |item| item.is_a?(Hash) && item['passed'] == true }
    digest_valid = %w[beforeDigest afterDigest].all? { |key| data[key].to_s.match?(/\A[a-f0-9]{64}\z/) }
    {path: path.delete_prefix(root + '/'), missing: missing, uiAssertionsValid: assertions_valid,
     digestsValid: digest_valid, passed: missing.empty? && assertions_valid && digest_valid}
  rescue JSON::ParserError => e
    {path: path.delete_prefix(root + '/'), parseError: e.class.name, passed: false}
  end
end
summary = {packages: rows.size, passedPackages: rows.count { |row| row[:passed] },
           failedPackages: rows.count { |row| !row[:passed] },
           schemaPresent: File.file?(File.join(root, 'docs/schemas/sync-evidence.schema.json')),
           passed: rows.any? && rows.all? { |row| row[:passed] }}
report = {schemaVersion: 1, generatedAt: Time.now.utc.iso8601,
          invariant: 'Every critical event has one evidence package linking before state, message, after state and passing UI assertions.',
          summary: summary, evidenceDirectory: evidence_dir, packages: rows}
FileUtils.mkdir_p(File.dirname(output))
File.write(output, JSON.pretty_generate(report) + "\n", mode: 'w:UTF-8')
puts JSON.generate(summary)
exit(summary[:passed] ? 0 : 2) if ENV['AOO_ENFORCE_SYNC_EVIDENCE_GATE'] == '1'
