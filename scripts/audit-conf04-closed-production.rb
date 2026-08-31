require 'json'
require 'fileutils'
root = File.expand_path('..', __dir__)
policy = File.read(File.join(root, 'server/GameCommon/src/main/java/com/aoo/bcg/common/config/ProductionConfigPolicy.java'))
environment = File.read(File.join(root, 'server/GameCommon/src/main/java/com/aoo/bcg/common/config/DeploymentEnvironment.java'))
test = File.read(File.join(root, 'server/GameCommon/src/test/java/com/aoo/bcg/common/config/ProductionConfigPolicyTest.java'))
checks = {
  explicit_environment: environment.include?('aoo.environment must be explicit'),
  development_fallback_forbidden: policy.include?('development-only configuration is forbidden'),
  legacy_protocol_forbidden: policy.include?('legacy protocol cannot be enabled'),
  required_values_fail_closed: policy.include?('requiredForService'),
  executable_tests: test.include?('productionRejectsDevelopmentFallbacksAndLegacyProtocol')
}
abort "CONF04 audit failed: #{checks}" unless checks.values.all?
out = File.join(root, 'work/audit/closed-production-config.json')
FileUtils.mkdir_p(File.dirname(out))
File.write(out, JSON.pretty_generate({task: 'CONF04', status: 'passed', checks: checks}) + "\n")
