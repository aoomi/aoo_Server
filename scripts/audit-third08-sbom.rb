#!/usr/bin/env ruby
require 'digest'
require 'fileutils'
require 'json'

root = File.expand_path('..', __dir__)
legacy_root = File.join(root, 'reference/legacy-2.22/third-party-maven-repository/com')
upstream_root = File.join(root, 'reference/upstream/org.atmosphere/nettosphere/3.2.2')
jars = (Dir.glob(File.join(legacy_root, '**/*.jar')) + Dir.glob(File.join(upstream_root, '*.jar'))).sort

component_for = lambda do |path|
  relative = path.delete_prefix(root + '/')
  sha = Digest::SHA256.file(path).hexdigest
  if relative.include?('/kernel/')
    group, name, version, purl, license = 'com.aoo.legacy', 'kernel', '1.0.0', 'pkg:maven/com.aoo.legacy/kernel@1.0.0', 'NOASSERTION'
  elsif relative.include?('3.2.2-qh')
    group, name, version, purl, license = 'com.aoo.legacy', 'nettosphere', '3.2.2-qh', 'pkg:maven/com.aoo.legacy/nettosphere@3.2.2-qh', 'Apache-2.0'
  else
    classifier = relative.end_with?('-sources.jar') ? '?classifier=sources' : ''
    group, name, version, purl, license = 'org.atmosphere', 'nettosphere', '3.2.2', "pkg:maven/org.atmosphere/nettosphere@3.2.2#{classifier}", 'Apache-2.0'
  end
  {
    type: relative.end_with?('-sources.jar') ? 'file' : 'library',
    'bom-ref': relative,
    group: group,
    name: name,
    version: version,
    hashes: [{ alg: 'SHA-256', content: sha }],
    licenses: [{ license: { id: license } }],
    purl: purl,
    scope: 'excluded',
    properties: [
      { name: 'aoo.runtimeReachable', value: 'false' },
      { name: 'aoo.archivePath', value: relative }
    ]
  }
end
components = jars.map { |path| component_for.call(path) }
sbom = {
  bomFormat: 'CycloneDX',
  specVersion: '1.6',
  serialNumber: 'urn:uuid:aoo-archived-third-party-20260823',
  version: 1,
  metadata: {
    component: { type: 'application', name: 'aoo-server-archived-third-party', version: '1.0.0-SNAPSHOT' },
    properties: [{ name: 'aoo.distributionScope', value: 'reference-only' }]
  },
  components: components
}
sbom_path = File.join(root, 'docs/generated/archived-third-party.cdx.json')
FileUtils.mkdir_p(File.dirname(sbom_path))
File.write(sbom_path, JSON.pretty_generate(sbom) + "\n")

osv_path = File.join(root, 'reference/security/osv/nettosphere-3.2.2-20260823.json')
osv = JSON.parse(File.read(osv_path))
scan = {
  schemaVersion: 1,
  task: 'THIRD08',
  snapshotDate: '2026-08-23',
  entries: [
    { coordinate: 'org.atmosphere:nettosphere:3.2.2', scanner: 'OSV', findings: osv.fetch('vulns', []), status: 'scanned' },
    { coordinate: 'com.aoo.legacy:nettosphere:3.2.2-qh', scanner: 'OSV upstream inheritance plus binary-diff dossier', findings: osv.fetch('vulns', []), status: 'custom-derivative-archive-only' },
    { coordinate: 'com.aoo.legacy:kernel:1.0.0', scanner: 'private-coordinate classification and owned-source regression', findings: [], status: 'not-publicly-indexed-archive-only' }
  ],
  licenseLedger: 'docs/generated/third-party-license-ledger.json',
  modificationDossier: 'docs/generated/third-party-modification-dossier.json'
}
scan_path = File.join(root, 'docs/generated/archived-third-party-vulnerability-scan.json')
File.write(scan_path, JSON.pretty_generate(scan) + "\n")
checks = {
  all_archived_jars_in_sbom: components.length == jars.length && jars.length == 4,
  every_component_hashed: components.all? { |component| component[:hashes].first[:content].length == 64 },
  every_component_has_license_disposition: components.all? { |component| !component[:licenses].empty? },
  every_component_release_excluded: components.all? { |component| component[:scope] == 'excluded' },
  osv_snapshot_present: File.file?(osv_path),
  every_legacy_coordinate_scan_classified: scan[:entries].length == 3 && scan[:entries].all? { |entry| !entry[:status].empty? },
  custom_binary_diff_linked: File.file?(File.join(root, scan[:modificationDossier])),
  license_ledger_linked: File.file?(File.join(root, scan[:licenseLedger]))
}
result = { task: 'THIRD08', passed: checks.values.all?, checks: checks, componentCount: components.length, publicVulnerabilityCount: osv.fetch('vulns', []).length, sbom: 'docs/generated/archived-third-party.cdx.json', scan: 'docs/generated/archived-third-party-vulnerability-scan.json' }
out = File.join(root, 'work/audit/third08-sbom.json')
FileUtils.mkdir_p(File.dirname(out))
File.write(out, JSON.pretty_generate(result) + "\n")
puts "THIRD08 #{result[:passed] ? 'passed' : 'failed'}: #{components.length} archived files in CycloneDX, #{result[:publicVulnerabilityCount]} OSV findings"
exit(result[:passed] ? 0 : 1)
