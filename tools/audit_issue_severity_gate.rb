#!/usr/bin/env ruby
# frozen_string_literal: true

require 'fileutils'
require 'json'
require 'time'

ROOT = File.expand_path('..', __dir__)
INPUT = File.join(ROOT, 'work', 'audit', 'issue-registry.json')
OUTPUT = File.join(ROOT, 'work', 'audit', 'issue-severity-gate.json')

abort "missing issue registry: #{INPUT}" unless File.file?(INPUT)

def severity(issue)
  text = [issue['domain'], issue['summary'], issue['acceptance']].join(' ')
  return 'P0' if text.match?(/暗牌|越权|账务.*错误|资金.*错误|密钥泄露|任意代码|数据丢失|结算.*错误/)
  return 'P1' if text.match?(/未通过|生产可达|权威|幂等|重放|乱序|持久化|旧接口|兼容.*生产|单写|鉴权|隔离|创建房间|状态恢复/)
  return 'P3' if text.match?(/文档|命名|格式|台账|说明/)
  'P2'
end

registry = JSON.parse(File.read(INPUT, encoding: 'UTF-8'))
issues = registry.fetch('issues').map do |issue|
  issue.merge('severity' => severity(issue),
              'releaseBlocking' => %w[P0 P1].include?(severity(issue)))
end
counts = %w[P0 P1 P2 P3].to_h { |level| [level, issues.count { |issue| issue['severity'] == level }] }
open_blockers = issues.select { |issue| issue['releaseBlocking'] && issue['lifecycle'] != 'VERIFIED' }
report = {
  generatedAt: Time.now.utc.iso8601,
  source: INPUT.delete_prefix(ROOT + '/'),
  policy: {
    P0: '安全、资金、数据完整性或权威结算灾难性缺陷，立即停止发布',
    P1: '核心业务、权威状态、隔离、持久化或兼容生产路径缺陷，发布前必须清零',
    P2: '非核心功能、性能或测试证据缺口，必须有负责人和整改期限',
    P3: '文档、命名或低风险维护问题，可进入后续版本',
    releaseRule: 'OPEN P0/P1 = 0'
  },
  counts: counts,
  openReleaseBlockerCount: open_blockers.length,
  releaseAllowed: open_blockers.empty?,
  blockers: open_blockers
}
FileUtils.mkdir_p(File.dirname(OUTPUT))
File.write(OUTPUT, JSON.pretty_generate(report) + "\n")
puts JSON.generate(counts: counts, openReleaseBlockers: open_blockers.length,
                   releaseAllowed: report[:releaseAllowed])
exit(report[:releaseAllowed] ? 0 : 1)
