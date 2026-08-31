#!/usr/bin/env ruby
require 'json'; require 'fileutils'
root = File.expand_path('..', __dir__)
runbook = File.read(File.join(root, 'docs/运维恢复演练手册.md'))
checks = {
  permissions: runbook.include?('权限与账号'),
  deployment: runbook.include?('发布前') && runbook.include?('启动与健康'),
  recovery: runbook.include?('回滚与恢复'),
  evidenceRequirements: runbook.include?('预发演练证据'),
  preproductionDrillEvidence: false
}
report = { schemaVersion: 1, runbook: 'docs/运维恢复演练手册.md', checks: checks, verdict: 'awaiting-preproduction-drill' }
out = File.join(root, 'docs/generated/drift08-operations-runbook.json'); FileUtils.mkdir_p(File.dirname(out)); File.write(out, JSON.pretty_generate(report) + "\n")
puts 'DRIFT08 AUDITED: awaiting preproduction drill'
