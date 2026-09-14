#!/usr/bin/env ruby
# frozen_string_literal: true

require 'json'
require 'time'
require 'fileutils'

root = File.expand_path('..', __dir__)
output = File.join(root, 'work/audit/legacy-build-isolation.json')

generated_dirs = Dir.glob(File.join(root, 'server/**/{target,build}')).select { |path| File.directory?(path) }
bundled_jars = Dir.glob(File.join(root, 'server/**/*.jar')).reject do |path|
  path.include?('/target/') || path.include?('/build/')
end
legacy_client_sources = Dir.glob(File.join(root, 'server/**/*Legacy*.{java,kt}'))
allowed_legacy_sources = legacy_client_sources.select do |path|
  path.match?(%r{/(?:compat|replay|billing|protocol/v2)/}) || path.end_with?(
    'LegacyCompatibleRoom.java', 'LegacyConfigGuard.java', 'LegacyConfigGuardTest.java',
    'LegacyReplayGuard.java', 'LegacyGameRequestGuard.java', 'LegacyUnifiedRoomBinding.java',
    'LegacyProtocolUsage.java', 'LegacyRuntimeContractSelfTest.java')
end
legacy_core_sources = legacy_client_sources - allowed_legacy_sources

pom_findings = []
Dir.glob(File.join(root, '**/pom.xml')).each do |path|
  text = File.read(path, encoding: 'UTF-8', invalid: :replace, undef: :replace)
  pom_findings << path.delete_prefix(root + '/') if text.match?(/aoo-legacy|com\.aoo\.legacy/)
end

summary = {
  generatedDirectories: generated_dirs.size,
  bundledJarsOutsideBuildOutput: bundled_jars.size,
  legacyNamedClientSources: legacy_client_sources.size,
  pomsReachingLegacyRepositoryOrKernel: pom_findings.size,
  passed: bundled_jars.empty? && legacy_core_sources.empty? && pom_findings.empty?
}
report = {
  schemaVersion: 1,
  generatedAt: Time.now.utc.iso8601,
  invariant: 'Production build and release classpaths cannot reach preserved 2.22 libraries, generated artifacts, legacy kernels or compatibility client sources.',
  summary: summary,
  findings: {
    generatedDirectories: generated_dirs.map { |path| path.delete_prefix(root + '/') },
    bundledJars: bundled_jars.map { |path| path.delete_prefix(root + '/') },
    isolatedLegacySources: allowed_legacy_sources.map { |path| path.delete_prefix(root + '/') },
    legacyCoreSources: legacy_core_sources.map { |path| path.delete_prefix(root + '/') },
    legacyPoms: pom_findings
  },
  policy: '2.22 binary dependencies are retained only under reference/legacy-2.22; source compatibility is allowed only in named adapter/guard/replay/billing/protocol boundaries and cannot expose a legacy production entry.'
}
FileUtils.mkdir_p(File.dirname(output))
File.write(output, JSON.pretty_generate(report) + "\n", mode: 'w:UTF-8')
puts JSON.generate(summary)
exit(summary[:passed] ? 0 : 2) if ENV['AOO_ENFORCE_LEGACY_BUILD_GATE'] == '1'
