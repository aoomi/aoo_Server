#!/usr/bin/env ruby
require 'digest'
require 'fileutils'
require 'json'
require 'open3'
require 'rexml/document'

root = File.expand_path('..', __dir__)
repository = File.join(root, 'reference/legacy-2.22/third-party-maven-repository/com')
artifacts = Dir.glob(File.join(repository, '**/*.jar')).sort.map do |jar|
  relative_jar = jar.delete_prefix(root + '/')
  base = jar.delete_suffix('.jar')
  pom = base + '.pom'
  xml = REXML::Document.new(File.read(pom))
  text = ->(name) { REXML::XPath.first(xml, "/project/#{name}")&.text.to_s.strip }
  entries, status = Open3.capture2('unzip', '-Z1', jar)
  files = status.success? ? entries.lines.map(&:strip).reject(&:empty?) : []
  manifest, = Open3.capture2('unzip', '-p', jar, 'META-INF/MANIFEST.MF')
  {
    coordinate: [text.call('groupId'), text.call('artifactId'), text.call('version')].join(':'),
    jar: relative_jar,
    pom: pom.delete_prefix(root + '/'),
    sha256: { jar: Digest::SHA256.file(jar).hexdigest, pom: Digest::SHA256.file(pom).hexdigest },
    sha1Sidecars: { jar: File.exist?(base + '.jar.sha1'), pom: File.exist?(base + '.pom.sha1') },
    content: {
      classCount: files.count { |file| file.end_with?('.class') },
      resourceCount: files.count { |file| !file.end_with?('/') && !file.end_with?('.class') },
      packages: files.grep(/\.class$/).map { |file| File.dirname(file) }.uniq.sort
    },
    provenance: {
      implementationVersion: manifest[/^Bundle-Version:\s*(.+)$/i, 1]&.strip || text.call('version'),
      buildJdk: manifest[/^Build-Jdk:\s*(.+)$/i, 1]&.strip || 'unknown',
      licenseDeclaration: manifest[/^Bundle-License:\s*(.+)$/i, 1]&.strip || 'not-declared',
      embeddedLicenseFiles: files.grep(/(?:^|\/)(?:LICENSE|NOTICE|COPYING)(?:[._-]|$)/i).sort,
      sourceArchivePresent: File.exist?(base + '-sources.jar')
    }
  }
end

inventory = { schemaVersion: 1, task: 'THIRD01', repository: 'reference/legacy-2.22/third-party-maven-repository/com', runtimeReachable: false, generatedBy: 'scripts/audit-third01-inventory.rb', artifacts: artifacts }
inventory_path = File.join(root, 'docs/generated/third-party-content-inventory.json')
FileUtils.mkdir_p(File.dirname(inventory_path))
File.write(inventory_path, JSON.pretty_generate(inventory) + "\n")

checks = {
  repository_exists: Dir.exist?(repository),
  every_jar_has_pom: Dir.glob(File.join(repository, '**/*.jar')).all? { |jar| File.file?(jar.delete_suffix('.jar') + '.pom') },
  every_binary_hashed: artifacts.all? { |artifact| artifact.dig(:sha256, :jar)&.length == 64 },
  every_pom_hashed: artifacts.all? { |artifact| artifact.dig(:sha256, :pom)&.length == 64 },
  content_catalogued: artifacts.all? { |artifact| artifact.dig(:content, :classCount).positive? },
  provenance_catalogued: artifacts.all? { |artifact| !artifact.dig(:provenance, :implementationVersion).to_s.empty? }
}
result = {
  task: 'THIRD01', passed: checks.values.all?, checks: checks, artifactCount: artifacts.length,
  inventory: 'docs/generated/third-party-content-inventory.json',
  licenseGaps: artifacts.select { |artifact| artifact.dig(:provenance, :licenseDeclaration) == 'not-declared' }.map { |artifact| artifact[:coordinate] },
  sourceArchiveGaps: artifacts.reject { |artifact| artifact.dig(:provenance, :sourceArchivePresent) }.map { |artifact| artifact[:coordinate] }
}
out = File.join(root, 'work/audit/third01-content-inventory.json')
FileUtils.mkdir_p(File.dirname(out))
File.write(out, JSON.pretty_generate(result) + "\n")
puts "THIRD01 #{result[:passed] ? 'passed' : 'failed'}: #{artifacts.length} embedded artifacts catalogued"
exit(result[:passed] ? 0 : 1)
