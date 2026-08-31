require 'json'
require 'fileutils'
root = File.expand_path('..', __dir__)
source = File.read(File.join(root, 'server/ConfigCenter/src/main/java/com/aoo/bcg/config/SignedLastKnownGoodConfiguration.java'))
test = File.read(File.join(root, 'server/ConfigCenter/src/test/java/com/aoo/bcg/config/SignedLastKnownGoodConfigurationTest.java'))
checks = {
  remote_failure_required: source.include?('remote failure is required before fallback'),
  signed_hmac: source.include?('HmacSHA256') && source.include?('MessageDigest.isEqual'),
  bounded_age: source.include?('last-known-good configuration expired'),
  atomic_persistence: source.include?('StandardCopyOption.ATOMIC_MOVE'),
  tamper_drill: test.include?('tampered[tampered.length / 2] ^= 1')
}
abort "CONF12 audit failed: #{checks}" unless checks.values.all?
out = File.join(root, 'work/audit/signed-last-known-good-config.json')
FileUtils.mkdir_p(File.dirname(out))
File.write(out, JSON.pretty_generate({task: 'CONF12', status: 'passed', checks: checks}) + "\n")
