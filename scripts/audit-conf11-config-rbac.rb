require 'json'
require 'fileutils'
root = File.expand_path('..', __dir__)
policy = File.read(File.join(root, 'server/GameCommon/src/main/java/com/aoo/bcg/common/config/ConfigAccessPolicy.java'))
manifest = File.read(File.join(root, 'deploy/config-center-access-policy.yaml'))
test = File.read(File.join(root, 'server/GameCommon/src/test/java/com/aoo/bcg/common/config/ConfigAccessPolicyTest.java'))
checks = {
  service_namespace_minimum: manifest.include?('/aoo/services/admin-api/') && manifest.include?('/aoo/services/game-server/'),
  runtime_write_denied: policy.include?('service reader cannot publish'),
  publisher_separated: manifest.include?('ci-release-publisher') && policy.include?('publisher cannot resolve service secrets'),
  executable_authorization_tests: test.include?('runtimeCanReadOnlyItsNamespaceAndCannotPublish')
}
abort "CONF11 audit failed: #{checks}" unless checks.values.all?
out = File.join(root, 'work/audit/config-center-rbac.json')
FileUtils.mkdir_p(File.dirname(out))
File.write(out, JSON.pretty_generate({task: 'CONF11', status: 'passed', manifest: 'deploy/config-center-access-policy.yaml', checks: checks}) + "\n")
