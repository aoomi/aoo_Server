#!/usr/bin/env ruby
require 'json'; require 'fileutils'
root = File.expand_path('..', __dir__)
claims = []
Dir.glob(File.join(root, 'docs/**/*.md')).sort.each do |path|
  File.foreach(path).with_index(1) do |line, number|
    next unless line.start_with?('|') && line.include?('已完成')
    evidence = line.scan(%r{(?:docs|work|server|database|protocol|deploy|scripts|tools)/[A-Za-z0-9_./\-]+}).map { |value| value.sub(/[、，。；:：`|]+\z/, '') }.uniq
    claims << {
      file: path.delete_prefix(root + '/'), line: number,
      evidence: evidence,
      missingEvidence: evidence.reject { |value| File.exist?(File.join(root, value)) },
      historicalTargetEvidence: evidence.select { |value| value.include?('/target/') },
      noEvidenceDeclared: evidence.empty? && !line.include?('无需证据')
    }
  end
end
issues = claims.select { |claim| claim[:missingEvidence].any? || claim[:historicalTargetEvidence].any? || claim[:noEvidenceDeclared] }
report = { schemaVersion: 1, completedClaims: claims.length, issueClaims: issues.length, issues: issues, checks: { noMissingEvidence: issues.none? { |c| c[:missingEvidence].any? }, noTargetEvidence: issues.none? { |c| c[:historicalTargetEvidence].any? }, everyClaimHasEvidence: issues.none? { |c| c[:noEvidenceDeclared] } }, verdict: issues.empty? ? 'passed' : 'blocked-ledger-remediation' }
out = File.join(root, 'docs/generated/drift09-ledger-evidence-audit.json'); FileUtils.mkdir_p(File.dirname(out)); File.write(out, JSON.pretty_generate(report) + "\n")
puts "DRIFT09 AUDITED: #{claims.length} claims, #{issues.length} issues"
