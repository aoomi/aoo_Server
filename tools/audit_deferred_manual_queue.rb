#!/usr/bin/env ruby
# frozen_string_literal: true

require 'fileutils'
require 'json'
require 'time'

ROOT = File.expand_path('..', __dir__)
TASK_FILE = File.join(ROOT, 'docs', 'Aoo-前后端全框架功能通信审计任务清单.md')
OUTPUT = File.join(ROOT, 'work', 'audit', 'deferred-manual-verification-queue.json')

queue = File.readlines(TASK_FILE, encoding: 'UTF-8').each_with_index.map do |line, index|
  columns = line.strip.split('|').map(&:strip)
  next unless columns.length >= 5
  task_id, status, task, acceptance = columns[1], columns[2], columns[3], columns[4]
  next unless task_id.match?(/\A[A-Z][A-Z0-9]*\d{2}\z/)
  text = [status, task, acceptance].join(' ')
  next unless text.match?(/待人工核验|人工审核|真机|真实浏览器|生产环境|外部环境|视觉核验|体验核验/)
  reason = if text.match?(/生产环境|外部环境/)
             'EXTERNAL_ENVIRONMENT'
           elsif text.match?(/真机/)
             'PHYSICAL_DEVICE'
           elsif text.match?(/视觉|真实浏览器|体验/)
             'HUMAN_VISUAL_INTERACTION'
           else
             'HUMAN_APPROVAL'
           end
  {
    taskId: task_id,
    sourceLine: index + 1,
    reason: reason,
    state: 'DEFERRED_UNTIL_AUTOMATED_QUEUE_COMPLETE',
    task: task,
    acceptance: acceptance
  }
end.compact

report = {
  generatedAt: Time.now.utc.iso8601,
  source: TASK_FILE.delete_prefix(ROOT + '/'),
  policy: 'Manual, visual, device and production-environment checks are queued once and executed only after all automatable tasks.',
  queueCount: queue.length,
  reasonCounts: queue.group_by { |entry| entry[:reason] }.transform_values(&:length),
  duplicateTaskIds: queue.group_by { |entry| entry[:taskId] }.select { |_id, entries| entries.length > 1 }.keys,
  passed: queue.group_by { |entry| entry[:taskId] }.all? { |_id, entries| entries.length == 1 },
  queue: queue
}
FileUtils.mkdir_p(File.dirname(OUTPUT))
File.write(OUTPUT, JSON.pretty_generate(report) + "\n")
puts JSON.generate(queued: queue.length, reasons: report[:reasonCounts].length,
                   duplicates: report[:duplicateTaskIds].length, passed: report[:passed])
exit(report[:passed] ? 0 : 1)
