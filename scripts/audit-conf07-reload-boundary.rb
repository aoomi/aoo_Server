require 'json'
require 'fileutils'
root = File.expand_path('..', __dir__)
catalog = File.read(File.join(root, 'server/GameCommon/src/main/java/com/aoo/bcg/common/config/RuntimeConfigKey.java'))
policy = File.read(File.join(root, 'server/GameCommon/src/main/java/com/aoo/bcg/common/config/ConfigReloadPolicy.java'))
test = File.read(File.join(root, 'server/GameCommon/src/test/java/com/aoo/bcg/common/config/ConfigReloadPolicyTest.java'))
checks = {
  explicit_mutability: catalog.include?('enum Mutability'),
  safe_default_restart: catalog.include?('return Mutability.RESTART_REQUIRED'),
  changed_set_atomic_validation: policy.include?('requireAtomicHotReload'),
  infrastructure_tested: test.include?('infrastructureAndProtocolSettingsCannotBePartiallyHotChanged')
}
abort "CONF07 audit failed: #{checks}" unless checks.values.all?
out = File.join(root, 'work/audit/config-reload-boundary.json')
FileUtils.mkdir_p(File.dirname(out))
File.write(out, JSON.pretty_generate({task: 'CONF07', status: 'passed', checks: checks}) + "\n")
