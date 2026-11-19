#!/usr/bin/env ruby
# frozen_string_literal: true

# Fail-closed validator for the forward canonical projection.  The database
# provider_key is a factory/alias contract, while the signed release manifest
# records the implementation class.  They are intentionally validated as two
# explicit fields; this validator never accepts a prefix match or silently
# rewrites one spelling to the other.

require 'digest'
require 'json'
require 'optparse'

OPTIONS = {
  manifest: File.expand_path('../../../tools/runtime-metadata-canonical-manifest.json', __dir__),
  registry: File.expand_path('../../../release-input/identity/canonical-identity-registry.json', __dir__),
  projection: nil,
  provider_contract: nil,
  manifest_sha256: '7f26b45d6a3dec5290004367f5db1f83c223b08cbf883f2e0356810531add613'
}

OptionParser.new do |parser|
  parser.on('--manifest PATH') { |v| OPTIONS[:manifest] = v }
  parser.on('--registry PATH') { |v| OPTIONS[:registry] = v }
  parser.on('--projection PATH') { |v| OPTIONS[:projection] = v }
  parser.on('--provider-contract PATH') { |v| OPTIONS[:provider_contract] = v }
  parser.on('--manifest-sha256 SHA') { |v| OPTIONS[:manifest_sha256] = v }
end.parse!

def reject!(code, details = {})
  STDERR.puts JSON.generate(ok: false, errorCode: code, details: details)
  exit 1
end

def read_json!(path, code)
  reject!(code, path: path, reason: 'missing') unless File.file?(path)
  JSON.parse(File.binread(path))
rescue JSON::ParserError => e
  reject!(code, path: path, reason: e.message)
end

manifest_path = OPTIONS[:manifest]
manifest_sha = Digest::SHA256.file(manifest_path).hexdigest
reject!('CANONICAL_MANIFEST_SHA_MISMATCH', expected: OPTIONS[:manifest_sha256], actual: manifest_sha) unless manifest_sha == OPTIONS[:manifest_sha256]
manifest = read_json!(manifest_path, 'CANONICAL_MANIFEST_MISSING')
registry = read_json!(OPTIONS[:registry], 'CANONICAL_IDENTITY_REGISTRY_MISSING')
reject!('CANONICAL_MANIFEST_STATUS_INVALID') unless manifest['manifestStatus'] == 'SIGNED_CANONICAL_INPUT' && manifest['consumable'] == true

rows = manifest['rows']
reject!('CANONICAL_MANIFEST_ROWS_INVALID') unless rows.is_a?(Array) && rows.length == 32
required_manifest = %w[gameId gameCode family playVersion regionCode snapshotSchemaVersion cardCodecVersion eventInterpreterVersion protocolVersion sourceHash releaseId indexGeneration providerBinding]
rows.each do |row|
  missing = required_manifest.reject { |field| row.key?(field) && !row[field].to_s.empty? }
  reject!('CANONICAL_MANIFEST_FIELD_MISSING', gameId: row['gameId'], fields: missing) unless missing.empty?
end
reject!('CANONICAL_MANIFEST_DUPLICATE_ID') unless rows.map { |r| r['gameId'] }.uniq.length == rows.length

registry_rows = registry['rows']
reject!('CANONICAL_REGISTRY_ROWS_INVALID') unless registry_rows.is_a?(Array) && registry_rows.length == 32
registry_by_id = registry_rows.to_h { |row| [row['gameId'], row] }
rows.each do |row|
  registry_row = registry_by_id[row['gameId']]
  reject!('CANONICAL_REGISTRY_IDENTITY_MISSING', gameId: row['gameId']) unless registry_row
  %w[gameCode playVersion sourceHash releaseId indexGeneration].each do |field|
    reject!('CANONICAL_REGISTRY_FIELD_CONFLICT', gameId: row['gameId'], field: field) unless registry_row[field].to_s == row[field].to_s
  end
  %w[providerKey clientRuntimeEntry contractVersion].each do |field|
    reject!('CANONICAL_REGISTRY_FIELD_MISSING', gameId: row['gameId'], field: field) if registry_row[field].to_s.empty?
  end
end

reject!('PROJECTION_INPUT_MISSING') if OPTIONS[:projection].to_s.empty?
projection_path = OPTIONS[:projection]
reject!('PROJECTION_INPUT_NOT_FOUND', path: projection_path) unless File.file?(projection_path)
projection_lines = File.readlines(projection_path, chomp: true).reject(&:empty?)
header = projection_lines.shift.to_s.split("\t")
expected_header = %w[gameId gameCode playVersion regionCode releaseId indexGeneration activeReleaseCount activeIndexCount]
reject!('PROJECTION_HEADER_INVALID', expected: expected_header, actual: header) unless header == expected_header
projection = projection_lines.map do |line|
  values = line.split("\t", -1)
  reject!('PROJECTION_ROW_WIDTH_INVALID', row: line) unless values.length == expected_header.length
  expected_header.zip(values).to_h
end
reject!('PROJECTION_ROW_COUNT_INVALID', expected: 32, actual: projection.length) unless projection.length == 32
projection_by_id = projection.to_h { |row| [row['gameId'].to_i, row] }
rows.each do |row|
  actual = projection_by_id[row['gameId'].to_i]
  reject!('PROJECTION_IDENTITY_MISSING', gameId: row['gameId']) unless actual
  %w[gameCode playVersion regionCode releaseId indexGeneration].each do |field|
    reject!('PROJECTION_POINTER_MISMATCH', gameId: row['gameId'], field: field, expected: row[field].to_s, actual: actual[field]) unless actual[field].to_s == row[field].to_s
  end
  %w[activeReleaseCount activeIndexCount].each do |field|
    reject!('PROJECTION_ACTIVE_NOT_UNIQUE', gameId: row['gameId'], field: field, actual: actual[field]) unless actual[field].to_i == 1
  end
end

# Four identities currently expose two different provider contract fields.
# The mapping is mandatory and must be signed by source-file hashes; omission
# is an explicit rejection instead of a permissive fallback.
reject!('PROVIDER_CONTRACT_INPUT_MISSING') if OPTIONS[:provider_contract].to_s.empty?
provider_doc = read_json!(OPTIONS[:provider_contract], 'PROVIDER_CONTRACT_INPUT_NOT_FOUND')
reject!('PROVIDER_CONTRACT_SCHEMA_INVALID') unless provider_doc['schemaVersion'] == 1 && provider_doc['status'] == 'SIGNED_FORMAL_MAPPING'
provider_rows = provider_doc['rows']
target_ids = [5, 9, 630, 1001]
reject!('PROVIDER_CONTRACT_ROW_COUNT_INVALID', actual: provider_rows.is_a?(Array) ? provider_rows.length : nil) unless provider_rows.is_a?(Array) && provider_rows.map { |r| r['gameId'] }.sort == target_ids
provider_rows.each do |binding|
  manifest_row = rows.find { |row| row['gameId'] == binding['gameId'] }
  registry_row = registry_by_id[binding['gameId']]
  reject!('PROVIDER_CONTRACT_IDENTITY_MISSING', gameId: binding['gameId']) unless manifest_row && registry_row
  %w[registryProviderKey providerFactoryKey providerImplementationClass].each do |field|
    reject!('PROVIDER_CONTRACT_FIELD_MISSING', gameId: binding['gameId'], field: field) if binding[field].to_s.empty?
  end
  reject!('PROVIDER_REGISTRY_KEY_MISMATCH', gameId: binding['gameId']) unless binding['registryProviderKey'] == registry_row['providerKey']
  reject!('PROVIDER_IMPLEMENTATION_MISMATCH', gameId: binding['gameId']) unless binding['providerImplementationClass'] == manifest_row.dig('providerBinding', 'providerClass')
  refs = binding['sourceRefs']
  reject!('PROVIDER_SOURCE_EVIDENCE_MISSING', gameId: binding['gameId']) unless refs.is_a?(Array) && !refs.empty?
  refs.each do |ref|
    path = ref['path']
    reject!('PROVIDER_SOURCE_NOT_FOUND', gameId: binding['gameId'], path: path) unless path && File.file?(path)
    actual = Digest::SHA256.file(path).hexdigest
    reject!('PROVIDER_SOURCE_HASH_MISMATCH', gameId: binding['gameId'], path: path, expected: ref['sha256'], actual: actual) unless actual == ref['sha256']
  end
end

puts JSON.generate(ok: true, manifestSha256: manifest_sha, identities: rows.length,
                   projectionRows: projection.length, providerContracts: provider_rows.length,
                   activePointerUniqueness: '32/32', providerContractScope: target_ids)
