#!/usr/bin/env ruby
require 'json'
require 'digest'
require 'fileutils'

root = File.expand_path('..', __dir__)
canonical = 'docs/Aoo-当前剩余问题与处理任务清单.md'
obsolete = ['docs/Aoo-当前未完成任务清单.md', 'docs/未完成任务清单.md']

read_json = lambda do |relative|
  path = File.join(root, relative)
  File.exist?(path) ? JSON.parse(File.read(path)) : nil
end

ledger = File.read(File.join(root, canonical))
active = ledger.scan(/^\| (68|77|87|88) \| ([^|]+) \|/).to_h
drift02 = read_json.call('docs/generated/drift02-version-build-reconciliation.json')
drift03 = read_json.call('docs/generated/drift03-interface-routing-reconciliation.json')
drift04 = read_json.call('docs/generated/drift04-schema-dictionary.json')
drift05 = read_json.call('docs/generated/drift05-play-documentation-reconciliation.json')
drift09 = read_json.call('docs/generated/drift09-ledger-evidence-audit.json')
classification = read_json.call('work/audit/classification-conflict-ledger.json')

checks = {
  'canonicalLedgerPresent' => File.exist?(File.join(root, canonical)),
  'obsoleteCurrentLedgersRemoved' => obsolete.none? { |path| File.exist?(File.join(root, path)) },
  'activeStatusesNotSelfAccepted' => active.keys.sort == %w[68 77 87 88] && active.values.all? { |value| value.strip == '处理中' },
  'interfaceEvidenceCurrent' => drift03&.dig('checks')&.values&.all?,
  'databaseEvidenceCurrent' => drift04&.dig('checks')&.values&.all?,
  'dependencyEvidenceCurrent' => drift02&.dig('checks')&.values&.all?,
  'gameplayGapHonestlyRecorded' => drift05&.dig('verdict') == 'blocked-profile-publication',
  'regionAndGameplayConflictsNotAccepted' => classification&.dig('summary', 'conflictedGames').to_i.positive? && classification&.dig('summary', 'conflictFree').to_i.zero?,
  'completedClaimsRequireEvidence' => drift09&.dig('issueClaims').to_i.zero?
}

report = {
  'schemaVersion' => 1,
  'canonicalLedger' => canonical,
  'obsoleteLedgers' => obsolete,
  'activeStatuses' => active,
  'evidence' => {
    'interfaces' => { 'contracts' => drift03&.dig('count'), 'checks' => drift03&.dig('checks') },
    'gameplay' => { 'verdict' => drift05&.dig('verdict'), 'catalogCoverage' => drift05&.dig('catalogCoverage') },
    'regions' => classification&.dig('summary'),
    'database' => { 'sqlFiles' => drift04&.dig('sqlFiles'), 'summary' => drift04&.dig('summary'), 'checks' => drift04&.dig('checks') },
    'dependencies' => drift02&.dig('checks'),
    'testClaims' => { 'completedClaims' => drift09&.dig('completedClaims'), 'issueClaims' => drift09&.dig('issueClaims'), 'verdict' => drift09&.dig('verdict') }
  },
  'checks' => checks,
  'verdict' => checks.values.all? ? 'passed' : 'blocked'
}

out = File.join(root, 'docs/generated/v13-doc-ledger-consistency.json')
FileUtils.mkdir_p(File.dirname(out))
File.write(out, JSON.pretty_generate(report) + "\n")
puts "V13 #{report['verdict'].upcase}: #{checks.count { |_key, value| value }}/#{checks.length} checks"
exit 2 unless checks.values.all?
