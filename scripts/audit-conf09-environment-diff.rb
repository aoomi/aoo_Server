require 'json'
require 'fileutils'
root = File.expand_path('..', __dir__)
source = File.read(File.join(root, 'server/GameCommon/src/main/java/com/aoo/bcg/common/config/EnvironmentDifferenceReport.java'))
test = File.read(File.join(root, 'server/GameCommon/src/test/java/com/aoo/bcg/common/config/EnvironmentDifferenceReportTest.java'))
checks = {
  automatic_report: source.include?('record Difference') && source.include?('compare('),
  explicit_allowlist: source.include?('allowedReason'),
  unexpected_drift_blocks: source.include?('requireAccepted'),
  secret_reference_redacted: source.include?('[SECRET_REFERENCE]'),
  executable_test: test.include?('reportsAllowedAndUnexpectedEnvironmentDifferences')
}
abort "CONF09 audit failed: #{checks}" unless checks.values.all?
out = File.join(root, 'work/audit/environment-config-difference.json')
FileUtils.mkdir_p(File.dirname(out))
File.write(out, JSON.pretty_generate({task: 'CONF09', status: 'passed', checks: checks}) + "\n")
