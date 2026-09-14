require 'json'
require 'fileutils'
root = File.expand_path('..', __dir__)
source = File.read(File.join(root, 'server/GameCommon/src/main/java/com/aoo/bcg/common/config/StrictRuntimeConfig.java'))
test = File.read(File.join(root, 'server/GameCommon/src/test/java/com/aoo/bcg/common/config/StrictRuntimeConfigTest.java'))
checks = {
  unknown_canonical_rejected: source.include?('unknown " + source + " configuration'),
  owned_environment_typo_rejected: source.include?('unknown owned environment configuration'),
  value_types_validated: %w[BOOLEAN DURATION INTEGER ADDRESS].all? { |kind| source.include?("case #{kind}") },
  blank_rejected: source.include?('blank configuration'),
  executable_tests: test.include?('rejectsUnknownKeysAndOwnedEnvironmentTypos')
}
abort "CONF03 audit failed: #{checks}" unless checks.values.all?
out = File.join(root, 'work/audit/strict-runtime-config-schema.json')
FileUtils.mkdir_p(File.dirname(out))
File.write(out, JSON.pretty_generate({task: 'CONF03', status: 'passed', checks: checks}) + "\n")
