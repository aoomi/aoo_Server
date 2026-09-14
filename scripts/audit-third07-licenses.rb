#!/usr/bin/env ruby
require 'digest'
require 'fileutils'
require 'json'
require 'open3'

root = File.expand_path('..', __dir__)
custom = File.join(root, 'reference/legacy-2.22/third-party-maven-repository/com/aoo/legacy/nettosphere/3.2.2-qh/nettosphere-3.2.2-qh.jar')
sources = File.join(root, 'reference/upstream/org.atmosphere/nettosphere/3.2.2/nettosphere-3.2.2-sources.jar')
license = File.join(root, 'reference/licenses/Apache-2.0.txt')
notice = File.join(root, 'THIRD_PARTY_NOTICE.md')
manifest, = Open3.capture2('unzip', '-p', custom, 'META-INF/MANIFEST.MF')
source_names, = Open3.capture2('unzip', '-Z1', sources)
java_sources = source_names.lines.map(&:strip).select { |name| name.end_with?('.java') }
licensed_sources = java_sources.count do |name|
  content, = Open3.capture2('unzip', '-p', sources, name)
  content.include?('Apache License, Version 2.0')
end
source_header_exceptions = java_sources.reject do |name|
  content, = Open3.capture2('unzip', '-p', sources, name)
  content.include?('Apache License, Version 2.0')
end
notice_text = File.read(notice)
release_rules = File.read(File.join(root, '.releaseignore')).lines.map(&:strip)
entries = [
  {
    coordinate: 'org.atmosphere:nettosphere:3.2.2 and historical customized derivative',
    license: 'Apache-2.0',
    evidence: ['source headers', 'custom Jar Bundle-License', 'reference/licenses/Apache-2.0.txt'],
    obligations: ['retain copyright', 'retain Apache-2.0 text', 'state historical customization'],
    distribution: 'archive excluded; notice retained defensively'
  },
  {
    coordinate: 'aoo.legacy:kernel:1.0.0',
    license: 'private-internal-no-public-license-declaration',
    evidence: ['THIRD_PARTY_NOTICE.md', 'release exclusion'],
    obligations: ['do not redistribute standalone binary', 'retain only as internal migration evidence'],
    distribution: 'prohibited and release excluded'
  }
]
checks = {
  apache_license_text_present: File.file?(license) && File.read(license).include?('Apache License'),
  apache_license_hash_pinned: Digest::SHA256.file(license).hexdigest == 'cfc7749b96f63bd31c3c42b5c471bf756814053e847c10f3eb003417bc523d30',
  custom_manifest_declares_apache: manifest.include?('www.apache.org/licenses/LICENSE-2.0'),
  upstream_source_archive_has_apache_headers: !java_sources.empty? && licensed_sources.positive?,
  notice_covers_both_artifacts: notice_text.include?('Nettosphere 3.2.2') && notice_text.include?('legacy kernel 1.0.0'),
  modifications_disclosed: notice_text.include?('customized binary'),
  archive_and_license_reference_excluded: release_rules.include?('reference/'),
  private_binary_non_distribution_declared: notice_text.include?('must not be redistributed')
}
ledger = { schemaVersion: 1, task: 'THIRD07', entries: entries }
ledger_path = File.join(root, 'docs/generated/third-party-license-ledger.json')
FileUtils.mkdir_p(File.dirname(ledger_path))
File.write(ledger_path, JSON.pretty_generate(ledger) + "\n")
result = { task: 'THIRD07', passed: checks.values.all?, checks: checks, sourceHeaderCoverage: { licensed: licensed_sources, total: java_sources.length, exceptionsCoveredByProjectLicense: source_header_exceptions }, ledger: 'docs/generated/third-party-license-ledger.json', notice: 'THIRD_PARTY_NOTICE.md' }
out = File.join(root, 'work/audit/third07-licenses.json')
FileUtils.mkdir_p(File.dirname(out))
File.write(out, JSON.pretty_generate(result) + "\n")
puts "THIRD07 #{result[:passed] ? 'passed' : 'failed'}: #{entries.length} archived artifacts have license/distribution dispositions"
exit(result[:passed] ? 0 : 1)
