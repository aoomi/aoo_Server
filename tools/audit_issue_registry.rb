#!/usr/bin/env ruby
# frozen_string_literal: true

require 'fileutils'
require 'json'
require 'time'

ROOT = File.expand_path('..', __dir__)
TASK_FILE = File.join(ROOT, 'docs', 'Aoo-前后端全框架功能通信审计任务清单.md')
OUTPUT = File.join(ROOT, 'work', 'audit', 'issue-registry.json')

rows = File.readlines(TASK_FILE, encoding: 'UTF-8').each_with_index.map do |line, index|
  columns = line.strip.split('|').map(&:strip)
  next unless columns.length >= 5
  task_id = columns[1]
  next unless task_id.match?(/\A[A-Z][A-Z0-9]*\d{2}\z/)

  status = columns[2]
  summary = columns[3]
  acceptance = columns[4]
  next if status == '已完成' && !summary.match?(/未通过|未闭环|待|失败|部分/)

  evidence = (summary + ' ' + acceptance).scan(%r{(?:/tmp/|work/|docs/|tools/)[^；，。\s|]+}).uniq
  source_line = index + 1
  evidence.unshift("docs/Aoo-前后端全框架功能通信审计任务清单.md#L#{source_line}")
  {
    issueId: "#{task_id}-001",
    domain: task_id.sub(/\d+\z/, ''),
    taskId: task_id,
    taskStatus: status,
    sourceLine: source_line,
    summary: summary,
    acceptance: acceptance,
    evidence: evidence,
    lifecycle: 'OPEN'
  }
end.compact

duplicate_ids = rows.group_by { |row| row[:issueId] }.select { |_id, values| values.length > 1 }.keys
missing_links = rows.select { |row| row[:taskId].empty? || row[:evidence].empty? }.map { |row| row[:issueId] }
report = {
  generatedAt: Time.now.utc.iso8601,
  source: TASK_FILE.delete_prefix(ROOT + '/'),
  policy: {
    issueId: '<DOMAIN><TASK_NO>-001',
    requiredLinks: %w[taskId sourceLine evidence],
    lifecycle: %w[OPEN FIXED VERIFIED DEFERRED REJECTED]
  },
  issueCount: rows.length,
  duplicateIssueIds: duplicate_ids,
  issuesMissingRequiredLinks: missing_links,
  passed: duplicate_ids.empty? && missing_links.empty?,
  issues: rows
}

FileUtils.mkdir_p(File.dirname(OUTPUT))
File.write(OUTPUT, JSON.pretty_generate(report) + "\n")
puts JSON.generate(issueCount: rows.length, duplicateIssueIds: duplicate_ids.length,
                   missingRequiredLinks: missing_links.length, passed: report[:passed])
exit(report[:passed] ? 0 : 1)
