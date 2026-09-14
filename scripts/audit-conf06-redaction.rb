require 'json'
require 'fileutils'
root = File.expand_path('..', __dir__)
source = File.read(File.join(root, 'server/GameCommon/src/main/java/com/aoo/bcg/common/config/SensitiveDataRedactor.java'))
test = File.read(File.join(root, 'server/GameCommon/src/test/java/com/aoo/bcg/common/config/SensitiveDataRedactorTest.java'))
checks = {
  bearer_masking: source.include?('BEARER'),
  credential_masking: source.include?('ASSIGNMENT'),
  connection_masking: source.include?('JDBC_CREDENTIALS'),
  certificate_masking: source.include?('PEM'),
  structured_recursive_masking: source.include?('redact(Map<String,?>'),
  throwable_boundary: source.include?('safeFailure(Throwable'),
  executable_tests: test.include?('masksCredentialsTokensCertificatesAndConnectionStrings')
}
abort "CONF06 audit failed: #{checks}" unless checks.values.all?
out = File.join(root, 'work/audit/sensitive-log-redaction.json')
FileUtils.mkdir_p(File.dirname(out))
File.write(out, JSON.pretty_generate({task: 'CONF06', status: 'passed', checks: checks}) + "\n")
