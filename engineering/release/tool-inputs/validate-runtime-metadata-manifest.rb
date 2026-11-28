#!/usr/bin/env ruby
# frozen_string_literal: true

require 'digest'
require 'json'
require 'open3'

ROOT = File.expand_path('../../../..', __dir__)
MANIFEST = File.join(__dir__, '../../../tools/runtime-metadata-canonical-manifest.json')
IDENTITIES = File.join(__dir__, '../../../release-input/identity/room-rule-play-identities.json')
CURRENT_CATALOG = File.join(ROOT, 'Server/server/GameSPI/src/main/resources/current-game-identities.tsv')
HASH = /\A[0-9a-f]{64}\z/
VERSION = /\A[A-Za-z0-9][A-Za-z0-9._-]{0,127}\z/
RUNTIME_VERSION = /\A[A-Za-z0-9][A-Za-z0-9._-]{0,63}\z/
LEGACY_REQUIRED_FIELDS = %w[
  snapshotSchemaVersion cardCodecVersion eventInterpreterVersion protocolVersion
  sourceHash releaseId indexGeneration
].freeze
CURRENT_REQUIRED_FIELDS = %w[
  snapshotSchemaVersion cardCodecVersion eventInterpreterVersion protocolVersion
  contractVersion sourceHash releaseId indexGeneration
].freeze
AUTHORIZED_MANIFEST_CONTRACTS = {
  'r9-v4' => {
    'manifestVersion' => 'r9-v4',
    'releaseIssuer' => '/root/map_runtime_release_values',
    'releaseEdition' => 'r9-v4',
    'manifestSha256' => '7f26b45d6a3dec5290004367f5db1f83c223b08cbf883f2e0356810531add613'
  },
  'r15-cd2999-20261012' => {
    'manifestVersion' => 'r50-current-20261011',
    'releaseIssuer' => '/root/lobby_runtime_from_staged_source',
    'releaseEdition' => 'r15-cd2999-20261012',
    'manifestSha256' => 'a453cb986a674f0f6044a581f8e69927a4cfbf94f531567ef021dfaee07a8282'
  },
  'r32-test-20261127' => {
    'manifestVersion' => 'r32-test-20261127-v4',
    'releaseIssuer' => 'aoo-test-release-admin',
    'releaseEdition' => 'r32-test-20261127',
    'manifestSha256' => '40bc9104d609daaa9bc5d1fbd6a276f11143a459358d7d108efac3791090299b'
  }
}.freeze

def fail!(message)
  abort("RUNTIME_METADATA_MANIFEST_REJECTED|#{message}")
end

def canonical_json(value)
  case value
  when Hash
    '{' + value.keys.map(&:to_s).sort.map { |key| JSON.generate(key) + ':' + canonical_json(value[key]) }.join(',') + '}'
  when Array
    '[' + value.map { |item| canonical_json(item) }.join(',') + ']'
  else
    JSON.generate(value)
  end
end

def sha256_bytes(value)
  Digest::SHA256.hexdigest(value)
end

def sha256_file(path)
  fail!("missing-source|#{path}") unless File.file?(path)
  Digest::SHA256.file(path).hexdigest
end

def normalized_workbook_hash(path)
  fail!("missing-workbook|#{path}") unless File.file?(path)
  entries, status = Open3.capture2('unzip', '-Z1', path)
  fail!("workbook-list-failed|#{path}") unless status.success?
  records = entries.lines.map(&:chomp).sort.reject { |entry| entry.empty? || entry.end_with?('/') || entry == 'docProps/core.xml' }.map do |entry|
    escaped_entry = entry.gsub('[', '\\[').gsub(']', '\\]')
    content, content_status = Open3.capture2('unzip', '-p', path, escaped_entry)
    fail!("workbook-member-read-failed|#{path}|#{entry}") unless content_status.success?
    { 'member' => entry, 'sha256' => sha256_bytes(content) }
  end
  sha256_bytes(JSON.generate(records))
end

def source_path_and_hash(value)
  path, marker, expected = value.to_s.partition('#sha256=')
  fail!("implementation-evidence-format|#{value}") unless marker && path != '' && expected.match?(HASH)
  [path, expected]
end

def current_catalog(path)
  fail!("current-catalog-missing|#{path}") unless File.file?(path)
  lines = File.readlines(path, chomp: true)
  fail!('current-catalog-header') unless lines.shift == "gameId\tgameCode\tplayVersion\tfamily"
  rows = lines.reject { |line| line.empty? || line.start_with?('#') }.map.with_index do |line, index|
    fields = line.split("\t", -1)
    fail!("current-catalog-row=#{index}") unless fields.length == 4
    game_id = Integer(fields[0], 10) rescue nil
    fail!("current-catalog-id=#{index}") unless game_id&.positive?
    row = { 'gameId' => game_id, 'gameCode' => fields[1], 'playVersion' => fields[2], 'family' => fields[3] }
    %w[gameCode playVersion family].each { |field| fail!("current-catalog-field=#{field}|#{game_id}") unless row[field].is_a?(String) && !row[field].strip.empty? }
    fail!("current-catalog-compatibility=#{game_id}") if row['family'].downcase.start_with?('word-card:')
    row
  end
  fail!("current-catalog-count=#{rows.length}") unless rows.length == 32
  fail!('current-catalog-duplicate') unless rows.map { |row| row['gameId'] }.uniq.length == 32
  rows.to_h { |row| [row['gameId'], row] }
end

manifest_path = ARGV[0] || MANIFEST
catalog_path = ARGV[1] || CURRENT_CATALOG
expected_release_issuer = ARGV[2] || '/root/lobby_runtime_from_staged_source'
expected_release_edition = ARGV[3] || 'r15-cd2999-20261012'
authorized = AUTHORIZED_MANIFEST_CONTRACTS[expected_release_edition]
fail!('release-contract-unauthorized') unless authorized && authorized['releaseIssuer'] == expected_release_issuer
begin
  manifest = JSON.parse(File.read(manifest_path, encoding: 'UTF-8'))
  identities = JSON.parse(File.read(IDENTITIES, encoding: 'UTF-8')).fetch('plays')
rescue Errno::ENOENT, JSON::ParserError, KeyError => error
  fail!("input-invalid|#{error.message}")
end

actual_manifest_sha = sha256_file(manifest_path)
fail!('manifest-sha-unauthorized') unless actual_manifest_sha == authorized['manifestSha256']

fail!('manifest-kind') unless manifest['manifestKind'] == 'runtime-metadata-canonical'
fail!('manifest-status') unless manifest['manifestStatus'] == 'SIGNED_CANONICAL_INPUT'
fail!('manifest-not-consumable') unless manifest['consumable'] == true
fail!('manifest-version') unless manifest['manifestVersion'] == authorized['manifestVersion']
fail!('release-issuer') unless manifest['releaseIssuer'] == authorized['releaseIssuer']
fail!('release-edition') unless manifest['releaseEdition'] == authorized['releaseEdition']
contract_version_required = manifest.fetch('requiredFields', []).include?('contractVersion')
required_fields = contract_version_required ? CURRENT_REQUIRED_FIELDS : LEGACY_REQUIRED_FIELDS
fail!('required-fields') unless manifest['requiredFields'] == required_fields
contract = manifest.fetch('sourceContract')
fail!('fallbacks-enabled') unless contract['noFallbacks'] == true
fail!('contract-version') if contract_version_required && !contract['contractVersion'].is_a?(String)
fail!('source-contract') unless %w[
  normalized-production-rule-and-component-artifact
  normalized-current-production-rule-and-component-artifact
].include?(contract['sourceKind'])
authority_by_id = nil
if contract['identityAuthority']
  authority_ref = contract.fetch('identityAuthority')
  authority_path = File.join(ROOT, authority_ref.fetch('path'))
  fail!('identity-authority-sha') unless sha256_file(authority_path) == authority_ref.fetch('sha256')
  authority_doc = JSON.parse(File.read(authority_path, encoding: 'UTF-8'))
  fail!('identity-authority-status') unless authority_doc['status'] == 'USER_SPECIFIED_LOCAL_ENGINEERING_DUMP'
  artifact = authority_doc.fetch('sourceArtifact')
  fail!('identity-authority-artifact-sha') unless sha256_file(File.join(ROOT, artifact.fetch('path'))) == artifact.fetch('sha256')
  authority_rows = authority_doc.fetch('rows')
  fail!('identity-authority-count') unless authority_rows.length == 32
  authority_by_id = authority_rows.to_h { |row| [row.fetch('gameId'), row] }
  fail!('identity-authority-duplicate') unless authority_by_id.length == 32
  fail!('identity-authority-contract') unless contract['identityPreserving'] == true && contract['databaseWrite'] == false
end
rows = manifest.fetch('rows')
fail!("row-count=#{rows.length}") unless rows.is_a?(Array) && rows.length == 32
by_id = identities.to_h { |identity| [identity.fetch('gameId'), identity] }
fail!('identity-source-count') unless by_id.length == 32
catalog = current_catalog(catalog_path)
release_ids = []
index_generations = []
seen_ids = {}

rows.each do |row|
  id = row.fetch('gameId')
  fail!("duplicate-game-id=#{id}") if seen_ids[id]
  seen_ids[id] = true
  identity = by_id[id]
  fail!("unknown-game-id=#{id}") unless identity && identity['publishingEnabled'] == true
  current = catalog[id]
  fail!("current-catalog-unknown-game-id=#{id}") unless current
  authority = authority_by_id && authority_by_id[id]
  if authority_by_id
    fail!("identity-authority-missing=#{id}") unless authority
    %w[gameCode family playVersion sourceHash releaseId indexGeneration].each do |field|
      fail!("identity-authority-mismatch=#{id}|#{field}") unless row[field] == authority[field]
    end
  end
  %w[gameCode playVersion family regionCode].each do |field|
    fail!("identity-mismatch=#{id}|#{field}") unless row[field] == identity[field]
  end
  %w[gameCode playVersion family].each do |field|
    fail!("current-catalog-mismatch=#{id}|#{field}") unless row[field] == current[field]
  end
  if contract_version_required
    fail!("missing-contract-version=#{id}") unless row['contractVersion'].is_a?(String) && row['contractVersion'].match?(RUNTIME_VERSION)
    fail!("contract-version-mismatch=#{id}") unless row['contractVersion'] == contract['contractVersion']
  end
  fail!("word-card-production-row=#{id}") if row['family'].to_s.downcase.start_with?('word-card:')
  %w[snapshotSchemaVersion cardCodecVersion eventInterpreterVersion protocolVersion].each do |field|
    fail!("missing-runtime-field=#{id}|#{field}") unless row[field].is_a?(String) && row[field].match?(RUNTIME_VERSION)
    evidence = row.fetch('runtimeMetadataEvidence').fetch(field)
    fail!("missing-runtime-provenance=#{id}|#{field}") unless evidence.is_a?(Array) && !evidence.empty? && evidence.any? { |item| item.include?('provider-route=') }
  end
  %w[sourceHash ruleContentHash componentContentHash sourceRevision].each do |field|
    fail!("invalid-hash=#{id}|#{field}") unless row[field].is_a?(String) && row[field].match?(HASH)
  end
  fail!("invalid-artifact-version=#{id}") unless row.fetch('artifactVersion').match?(VERSION)
  %w[releaseId indexGeneration].each do |field|
    value = row.fetch(field)
    fail!("invalid-literal=#{id}|#{field}") unless value.is_a?(Integer) && value.positive?
  end
  release_ids << row['releaseId']; index_generations << row['indexGeneration']
  reason = row.fetch('issuanceReason')
  fail!("issuance-reason=#{id}") unless reason.include?('explicit literal') && reason.include?('no fallback')

  implementation = row.fetch('providerBinding').fetch('implementationEvidence')
  records = implementation.map do |item|
    path, expected = source_path_and_hash(item)
    actual = sha256_file(File.join(ROOT, path))
    fail!("source-drift=#{id}|#{path}") unless actual == expected
    { 'path' => path, 'sha256' => expected }
  end
  component_hash = sha256_bytes(canonical_json(records))
  fail!("component-hash=#{id}") unless component_hash == row['componentContentHash'] && row.fetch('sourceArtifact').fetch('normalizedContentHash') == component_hash

  workbook = identity.fetch('workbookPath')
  workbook_hash = normalized_workbook_hash(File.join(ROOT, workbook))
  artifact = row.fetch('sourceArtifact')
  fail!("workbook-provenance=#{id}") unless artifact.fetch('normalizedWorkbookSource').start_with?("#{workbook}#normalized-") && artifact.fetch('normalizedWorkbookHash') == workbook_hash
  rule_hash = sha256_bytes(canonical_json(identity))
  fail!("rule-hash=#{id}") unless rule_hash == row['ruleContentHash']
  revision_input = { 'components' => records, 'workbook' => { 'path' => workbook, 'normalizedHash' => workbook_hash } }
  revision = sha256_bytes(canonical_json(revision_input))
  fail!("source-revision=#{id}") unless revision == row['sourceRevision']
  binding = { 'gameId' => id, 'gameCode' => row['gameCode'], 'providerRoute' => row.fetch('providerBinding').fetch('route'), 'providerClass' => row.fetch('providerBinding').fetch('providerClass'), 'artifactVersion' => row['artifactVersion'], 'ruleContentHash' => rule_hash, 'normalizedWorkbookHash' => workbook_hash, 'componentContentHash' => component_hash, 'sourceRevision' => revision }
  binding['contractVersion'] = row['contractVersion'] if contract_version_required
  unless authority_by_id
    fail!("source-hash=#{id}") unless sha256_bytes(canonical_json(binding)) == row['sourceHash']
  end
end

fail!('missing-identity-row') unless seen_ids.keys.sort == by_id.keys.sort
fail!('missing-current-catalog-row') unless seen_ids.keys.sort == catalog.keys.sort
fail!('duplicate-release-id') unless release_ids.uniq.length == 32
fail!('duplicate-index-generation') unless index_generations.uniq.length == 32
result = {
  ok: true,
  manifest: manifest_path,
  rows: rows.length,
  releaseIds: release_ids.length,
  indexGenerations: index_generations.length
}
result[:contractVersion] = contract['contractVersion'] if contract_version_required
puts JSON.generate(result)
