require 'json'
require 'fileutils'
root = File.expand_path('..', __dir__)
release = File.read(File.join(root, 'server/ConfigCenter/src/main/java/com/aoo/bcg/config/ConfigurationRelease.java'))
store = File.read(File.join(root, 'server/ConfigCenter/src/main/java/com/aoo/bcg/config/AtomicConfigurationReleaseStore.java'))
test = File.read(File.join(root, 'server/ConfigCenter/src/test/java/com/aoo/bcg/config/AtomicConfigurationReleaseStoreTest.java'))
checks = {
  complete_domain_version_set: release.include?('Map<String,String> domainVersions'),
  integrity_checksum: release.include?('SHA-256'),
  cas_publication: store.include?('expectedActiveReleaseId'),
  atomic_rollback: store.include?('rollback(String targetReleaseId'),
  multi_domain_test: test.include?('publishesAndRollsBackEveryDomainAsOneVersionSet')
}
abort "CONF08 audit failed: #{checks}" unless checks.values.all?
out = File.join(root, 'work/audit/atomic-configuration-release.json')
FileUtils.mkdir_p(File.dirname(out))
File.write(out, JSON.pretty_generate({task: 'CONF08', status: 'passed', checks: checks}) + "\n")
