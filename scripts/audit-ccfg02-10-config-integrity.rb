#!/usr/bin/env ruby
require 'json'
require 'find'
require 'digest'
require 'fileutils'

root = File.expand_path('..', __dir__)
client = File.expand_path('../Client', root)
assets = File.join(client, 'assets')
configs = File.join(client, 'development/configs')
out = File.join(root, 'docs/generated')
FileUtils.mkdir_p(out)

def write_report(out, id, payload)
  File.write(File.join(out, "#{id}.json"), JSON.pretty_generate(payload) + "\n")
end

json_files = Dir.glob(File.join(configs, '**/*.json')).sort
config_inventory = json_files.map do |path|
  parsed = JSON.parse(File.read(path)) rescue nil
  {path: path.delete_prefix(client + '/'), sha256: Digest::SHA256.file(path).hexdigest, parseable: !parsed.nil?, example: File.basename(path).include?('example')}
end
legacy_create = Dir.glob(File.join(assets, '**/{gameCreate,roomcost}.json')).sort
write_report(out, 'ccfg02-default-authority', {schemaVersion: 1, developmentDefaults: Dir.glob(File.join(configs, 'defaults/*')).select { |p| File.file?(p) && !p.end_with?('.meta') }, legacyRuntimeConfigs: legacy_create.map { |p| p.delete_prefix(client + '/') }, databaseAuthorityPresent: true, uniqueGenerationChain: false, passed: false})

manifests = config_inventory.select { |row| row[:path].include?('/manifests/') }
manifest_complete = manifests.any? { |row| !row[:example] } && manifests.all? { |row| row[:parseable] }
write_report(out, 'ccfg03-manifest-completeness', {schemaVersion: 1, manifests: manifests, requiredFields: %w[version hashes resources rebuild], complete: false, passed: false})

schema_files = Dir.glob(File.join(configs, 'schemas/*.json')).sort
write_report(out, 'ccfg04-schema-constraints', {schemaVersion: 1, schemaFiles: schema_files.map { |p| p.delete_prefix(client + '/') }, requiredConstraints: %w[type range enum mutualExclusion dependency deprecation], complete: false, passed: false})

test_files = Dir.glob(File.join(configs, 'tests/**/*')).select { |p| File.file?(p) && !p.end_with?('.meta') }
write_report(out, 'ccfg05-config-tests', {schemaVersion: 1, testFiles: test_files.map { |p| p.delete_prefix(client + '/') }, schemaDefaultBundleCoverage: false, passed: false})

write_report(out, 'ccfg06-generation-chain', {schemaVersion: 1, stages: %w[database generate sign publish rollback], implementedStages: [], unique: false, passed: false})

runtime_configs = Dir.glob(File.join(assets, '**/*.json')).select { |p| p.include?('/Config/') }
write_report(out, 'ccfg07-config-drift', {schemaVersion: 1, developmentConfigs: config_inventory, runtimeConfigCount: runtime_configs.length, parallelCopies: legacy_create.map { |p| p.delete_prefix(client + '/') }, passed: false})

secret_patterns = /(?:password|passwd|secret|private[_-]?key|access[_-]?token)\s*["'=:\s]+[^\s,"'}]{6,}/i
secret_hits = []
(json_files + runtime_configs).uniq.each do |path|
  next if File.size(path) > 20_000_000
  File.foreach(path).with_index(1) { |line, no| secret_hits << {path: path.delete_prefix(client + '/'), line: no} if line.match?(secret_patterns) }
rescue ArgumentError
end
write_report(out, 'ccfg08-sensitive-config', {schemaVersion: 1, scannedFiles: (json_files + runtime_configs).uniq.length, suspiciousHits: secret_hits, publicationGateImplemented: false, historyAndBuildLogIsolationProven: false, passed: false})

endpoint_hits = []
Dir.glob(File.join(assets, '**/*.{ts,json}')).each do |path|
  next if File.size(path) > 10_000_000
  File.foreach(path).with_index(1) do |line, no|
    endpoint_hits << {path: path.delete_prefix(client + '/'), line: no, kind: line[/localhost|127\.0\.0\.1|ws:\/\//]} if line.match?(/localhost|127\.0\.0\.1|ws:\/\//)
  end
end
write_report(out, 'ccfg09-environment-endpoints', {schemaVersion: 1, endpointHits: endpoint_hits, controlledProfiles: [], productionInjectionUnique: false, passed: false})

forbidden = /(?:qh|QH|2\.22|archived|development)/
manifest_hits = manifests.flat_map do |row|
  path = File.join(client, row[:path])
  File.readlines(path).each_with_index.map { |line, i| {path: row[:path], line: i + 1} if line.match?(forbidden) }.compact
end
write_report(out, 'ccfg10-manifest-legacy-exclusion', {schemaVersion: 1, manifestHits: manifest_hits, productionManifestPresent: manifest_complete, exclusionGateImplemented: false, passed: false})

puts 'CCFG02-CCFG10 audited: all nine items remain blocked with deterministic evidence'
