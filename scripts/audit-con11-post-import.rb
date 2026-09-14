require 'json'
require 'fileutils'
root = File.expand_path('..', __dir__)
gate = File.read(File.join(root, 'server/gameServer/src/core/server/PostImportIntegrityReadiness.java'))
entry = File.read(File.join(root, 'server/gameServer/src/core/server/ProductionSchemaReadiness.java'))
policy = File.read(File.join(root, 'docs/历史数据导入后完整性复核规范.md'))
checks = {
  startup_gate_connected: entry.include?('PostImportIntegrityReadiness.verify'),
  constraints_revalidated: gate.include?('CONSTRAINT_TYPE') && gate.include?('ENFORCED'),
  indexes_revalidated: gate.include?('information_schema.STATISTICS') && gate.include?('IS_VISIBLE'),
  primary_keys_revalidated: gate.include?('REQUIRED_PRIMARY_KEYS'),
  operational_policy: policy.include?('FOREIGN_KEY_CHECKS') && policy.include?('不得接收流量')
}
abort "CON11 audit failed: #{checks}" unless checks.values.all?
out = File.join(root, 'work/audit/post-import-integrity.json')
FileUtils.mkdir_p(File.dirname(out))
File.write(out, JSON.pretty_generate({task: 'CON11', status: 'passed', checks: checks}) + "\n")
