require 'json'
require 'fileutils'
root = File.expand_path('..', __dir__)
catalog = File.read(File.join(root, 'server/GameCommon/src/main/java/com/aoo/bcg/common/config/RuntimeConfigKey.java'))
test = File.read(File.join(root, 'server/GameCommon/src/test/java/com/aoo/bcg/common/config/RuntimeConfigKeyTest.java'))
checks = {
  canonical_catalog: catalog.include?('enum RuntimeConfigKey'),
  typed_categories: %w[ADDRESS BOOLEAN DURATION INTEGER SECRET TEXT].all? { |kind| catalog.include?(kind) },
  duplicate_guard: catalog.include?('duplicate runtime configuration key'),
  environment_alias_is_derived: catalog.include?('environmentName()'),
  executable_test: test.include?('catalogHasUniqueCanonicalAndEnvironmentNames')
}
abort "CONF02 audit failed: #{checks}" unless checks.values.all?
out = File.join(root, 'work/audit/runtime-config-key-catalog.json')
FileUtils.mkdir_p(File.dirname(out))
File.write(out, JSON.pretty_generate({task: 'CONF02', status: 'passed', checks: checks}) + "\n")
