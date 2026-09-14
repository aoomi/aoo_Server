#!/usr/bin/env ruby
# frozen_string_literal: true

require "json"
require "fileutils"

root = File.expand_path("..", __dir__)
audit_dir = File.join(root, "work", "audit")
FileUtils.mkdir_p(audit_dir)

required = {
  "STAGE01" => "stage01-scjymj.json",
  "STAGE02" => "stage02-xcpdk.json",
  "STAGE03" => "stage03-directory-classification.json",
  "STAGE05" => "stage05-disposition-ledger.json",
  "STAGE06" => "stage06-batch-conservation.json",
  "STAGE07" => "stage07-build-source-drift.json",
  "UNUSED28" => "unused28-full-chain.json"
}

evidence = required.transform_values do |name|
  path = File.join(audit_dir, name)
  begin
    { "present" => true, "content" => JSON.parse(File.read(path)) }
  rescue Errno::ENOENT, JSON::ParserError => error
    { "present" => false, "error" => error.message }
  end
end

stage07_counts = evidence.dig("STAGE07", "content", "counts") || {}
build_only = stage07_counts.fetch("build-only", stage07_counts.fetch("buildOnly", 0)).to_i
drifted = stage07_counts.fetch("drifted", 0).to_i
missing = evidence.each_with_object([]) { |(task, item), result| result << task unless item["present"] }
full_chain_passed = evidence.dig("UNUSED28", "content", "status") == "passed"

blockers = []
blockers << "missing prerequisite evidence: #{missing.join(', ')}" unless missing.empty?
blockers << "STAGE07 reports #{build_only} build-only files" if build_only.positive?
blockers << "STAGE07 reports #{drifted} drifted files" if drifted.positive?
blockers << "UNUSED28 full-chain evidence is not passing" unless full_chain_passed

report = {
  task: "STAGE08",
  status: blockers.empty? ? "passed" : "blocked",
  cleanupApproved: blockers.empty?,
  policy: "Destructive cleanup requires complete conservation, disposition, drift and full-chain evidence.",
  evidencePresent: evidence.transform_values { |item| item["present"] },
  stage07Counts: { buildOnly: build_only, drifted: drifted },
  blockers: blockers,
  destructiveActionTaken: false
}

File.write(File.join(audit_dir, "stage08-cleanup-readiness.json"), JSON.pretty_generate(report) + "\n")
puts JSON.generate(report)
exit(report[:cleanupApproved] ? 0 : 2)
