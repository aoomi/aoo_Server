#!/usr/bin/env ruby
# frozen_string_literal: true

require 'fileutils'
require 'json'
require 'time'

ROOT = File.expand_path('..', __dir__)
REGISTRY = File.join(ROOT, 'work', 'audit', 'issue-registry.json')
SEVERITY = File.join(ROOT, 'work', 'audit', 'issue-severity-gate.json')
OUTPUT = File.join(ROOT, 'work', 'audit', 'issue-root-cause-registry.json')

abort 'run FIX01 and FIX02 gates first' unless File.file?(REGISTRY) && File.file?(SEVERITY)

RULES = [
  ['SECURITY_BOUNDARY', /鉴权|越权|暗牌|密钥|重放|签名|Origin|防作弊/, 'security-platform'],
  ['LEGACY_REACHABILITY', /legacy|旧接口|旧版本|兼容|2\.22|隔离/, 'migration-platform'],
  ['DATA_CONSISTENCY', /数据库|持久化|事务|Outbox|幂等|快照|恢复|账务|事件/, 'data-platform'],
  ['PROTOCOL_WIRING', /协议|消息|通信|HTTP|WebSocket|WSS|接口|按钮|广播/, 'gateway-and-client'],
  ['ARCHITECTURE_BOUNDARY', /架构|公共层|SPI|组件|依赖图|目录|边界/, 'architecture'],
  ['CONFIGURATION', /配置|环境|端口|参数|开关|索引/, 'configuration'],
  ['DEPENDENCY_VERSION', /依赖|插件|版本|JDK|Maven|Jar|SBOM/, 'build-platform'],
  ['PERFORMANCE_CAPACITY', /性能|压测|延迟|吞吐|内存|包体|容量|背压/, 'performance'],
  ['TEST_EVIDENCE', /测试|证据|核验|真机|浏览器|采集|基准/, 'quality-engineering'],
  ['MIGRATION_OMISSION', /迁移|遗漏|未映射|缺失|尚未|未覆盖/, 'migration-platform']
].freeze

def classify(text)
  rule = RULES.find { |_category, pattern, _owner| text.match?(pattern) }
  rule || ['UNCLASSIFIED', /.*/, 'task-domain-owner']
end

issues = JSON.parse(File.read(REGISTRY, encoding: 'UTF-8')).fetch('issues').map do |issue|
  text = [issue['summary'], issue['acceptance']].join(' ')
  category, _pattern, owner = classify(text)
  issue.merge(
    'rootCauseCategory' => category,
    'accountableOwner' => owner,
    'remediationStrategy' => category == 'UNCLASSIFIED' ? 'perform causal analysis before code change' :
      "remove #{category.downcase} cause at shared boundary; verify all affected consumers",
    'rootCauseState' => category == 'UNCLASSIFIED' ? 'NEEDS_ANALYSIS' : 'CLASSIFIED'
  )
end

counts = issues.group_by { |issue| issue['rootCauseCategory'] }.transform_values(&:length)
unclassified = issues.count { |issue| issue['rootCauseCategory'] == 'UNCLASSIFIED' }
report = {
  generatedAt: Time.now.utc.iso8601,
  sources: [REGISTRY, SEVERITY].map { |path| path.delete_prefix(ROOT + '/') },
  taxonomy: RULES.map { |category, _pattern, owner| { category: category, defaultOwner: owner } },
  categoryCounts: counts,
  unclassifiedCount: unclassified,
  passed: unclassified.zero?,
  issues: issues
}
FileUtils.mkdir_p(File.dirname(OUTPUT))
File.write(OUTPUT, JSON.pretty_generate(report) + "\n")
puts JSON.generate(issueCount: issues.length, categories: counts.length,
                   unclassified: unclassified, passed: report[:passed])
exit(report[:passed] ? 0 : 1)
