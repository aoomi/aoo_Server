#!/usr/bin/env ruby
# frozen_string_literal: true

require 'fileutils'
require 'json'
require 'time'

ROOT = File.expand_path('..', __dir__)
ROOT_CAUSES = File.join(ROOT, 'work', 'audit', 'issue-root-cause-registry.json')
SEVERITY = File.join(ROOT, 'work', 'audit', 'issue-severity-gate.json')
OUTPUT = File.join(ROOT, 'work', 'audit', 'shared-root-remediation-plan.json')

abort 'run FIX03 first' unless File.file?(ROOT_CAUSES) && File.file?(SEVERITY)

root_report = JSON.parse(File.read(ROOT_CAUSES, encoding: 'UTF-8'))
severity_report = JSON.parse(File.read(SEVERITY, encoding: 'UTF-8'))
blocker_levels = severity_report.fetch('blockers').to_h { |issue| [issue.fetch('issueId'), issue.fetch('severity')] }
rank = { 'P0' => 0, 'P1' => 1, 'P2' => 2, 'P3' => 3 }

groups = root_report.fetch('issues').group_by { |issue| issue.fetch('rootCauseCategory') }.map do |category, issues|
  levels = issues.map { |issue| blocker_levels.fetch(issue.fetch('issueId'), 'P2') }
  highest = levels.min_by { |level| rank.fetch(level) }
  shared = issues.length > 1
  {
    rootCauseCategory: category,
    highestSeverity: highest,
    affectedIssueCount: issues.length,
    affectedDomains: issues.map { |issue| issue.fetch('domain') }.uniq.sort,
    implementationScope: shared ? 'SHARED_BOUNDARY' : 'LOCAL',
    localPatchAllowed: !shared,
    requiredOrder: shared ? %w[shared_contract shared_implementation representative_tests affected_matrix] :
      %w[local_implementation focused_test],
    issueIds: issues.map { |issue| issue.fetch('issueId') }
  }
end.sort_by { |group| [rank.fetch(group[:highestSeverity]), -group[:affectedIssueCount]] }

violations = groups.select { |group| group[:affectedIssueCount] > 1 && group[:implementationScope] != 'SHARED_BOUNDARY' }
report = {
  generatedAt: Time.now.utc.iso8601,
  source: ROOT_CAUSES.delete_prefix(ROOT + '/'),
  policy: 'A root cause affecting multiple issues must be removed at a shared boundary before any per-game patch.',
  rootCauseGroups: groups.length,
  sharedBoundaryGroups: groups.count { |group| group[:implementationScope] == 'SHARED_BOUNDARY' },
  violations: violations,
  passed: violations.empty?,
  plan: groups
}
FileUtils.mkdir_p(File.dirname(OUTPUT))
File.write(OUTPUT, JSON.pretty_generate(report) + "\n")
puts JSON.generate(groups: groups.length, sharedBoundaryGroups: report[:sharedBoundaryGroups],
                   violations: violations.length, passed: report[:passed])
exit(report[:passed] ? 0 : 1)
