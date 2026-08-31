#!/usr/bin/env ruby
# frozen_string_literal: true

require 'fileutils'
require 'json'
require 'set'
require 'time'

ROOT = File.expand_path('..', __dir__)
TASK_FILE = File.join(ROOT, 'docs', 'Aoo-前后端全框架功能通信审计任务清单.md')
REGISTRY = File.join(ROOT, 'work', 'audit', 'issue-registry.json')
OUTPUT = File.join(ROOT, 'work', 'audit', 'task-issue-conservation.json')

abort 'run issue registry first' unless File.file?(REGISTRY)

tasks = File.readlines(TASK_FILE, encoding: 'UTF-8').each_with_index.map do |line, index|
  columns = line.strip.split('|').map(&:strip)
  next unless columns.length >= 5
  next unless columns[1].match?(/\A[A-Z][A-Z0-9]*\d{2}\z/)
  { taskId: columns[1], status: columns[2], detail: columns[3], acceptance: columns[4], line: index + 1 }
end.compact

follow_up = tasks.select do |task|
  task[:status] != '已完成' || task[:detail].match?(/未通过|未闭环|待|失败|部分/)
end
registry = JSON.parse(File.read(REGISTRY, encoding: 'UTF-8'))
issues = registry.fetch('issues')
task_ids = tasks.map { |task| task[:taskId] }.to_set
issue_task_ids = issues.map { |issue| issue.fetch('taskId') }.to_set
expected_ids = follow_up.map { |task| task[:taskId] }.to_set

report = {
  generatedAt: Time.now.utc.iso8601,
  taskCount: tasks.length,
  followUpTaskCount: follow_up.length,
  issueCount: issues.length,
  duplicateTaskIds: tasks.group_by { |task| task[:taskId] }.select { |_id, rows| rows.length > 1 }.keys,
  duplicateIssueIds: issues.group_by { |issue| issue.fetch('issueId') }.select { |_id, rows| rows.length > 1 }.keys,
  followUpTasksWithoutIssue: (expected_ids - issue_task_ids).to_a.sort,
  issuesWithoutTask: (issue_task_ids - task_ids).to_a.sort,
  staleIssuesForClosedTasks: (issue_task_ids - expected_ids).to_a.sort,
  countConserved: follow_up.length == issues.length,
  passed: false
}
report[:passed] = report[:duplicateTaskIds].empty? && report[:duplicateIssueIds].empty? &&
                  report[:followUpTasksWithoutIssue].empty? && report[:issuesWithoutTask].empty? &&
                  report[:staleIssuesForClosedTasks].empty? && report[:countConserved]
FileUtils.mkdir_p(File.dirname(OUTPUT))
File.write(OUTPUT, JSON.pretty_generate(report) + "\n")
puts JSON.generate(tasks: tasks.length, followUpTasks: follow_up.length, issues: issues.length,
                   missing: report[:followUpTasksWithoutIssue].length,
                   stale: report[:staleIssuesForClosedTasks].length, passed: report[:passed])
exit(report[:passed] ? 0 : 1)
