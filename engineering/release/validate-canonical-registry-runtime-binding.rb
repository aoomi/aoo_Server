#!/usr/bin/env ruby
# frozen_string_literal: true

# Strict, forward-only binding gate. It never fills or rewrites a canonical
# registry/contract. A projection is emitted only after the supplied manifest,
# registry and explicitly signed contract all bind to one another.

require 'digest'
require 'json'
require 'optparse'

class BindingError < StandardError; end

def read_json(path)
  raise BindingError, "SOURCE_MISSING|path=#{path}" unless File.file?(path)
  JSON.parse(File.binread(path))
rescue JSON::ParserError => e
  raise BindingError, "SOURCE_JSON_INVALID|path=#{path}|message=#{e.message}"
end

def sha256(path)
  raise BindingError, "SOURCE_MISSING|path=#{path}" unless File.file?(path)
  Digest::SHA256.file(path).hexdigest
end

def require_hex(value, error)
  raise BindingError, error unless value.is_a?(String) && value.match?(/\A[0-9a-f]{64}\z/)
  value
end

def require_text(value, error)
  raise BindingError, error unless value.is_a?(String) && !value.strip.empty?
  value
end

def rows_by_id(rows, kind)
  raise BindingError, "#{kind}_ROWS_INVALID" unless rows.is_a?(Array) && rows.length == 32
  ids = rows.map { |row| row['gameId'] }
  raise BindingError, "#{kind}_DUPLICATE_GAME_ID" unless ids.all? { |id| id.is_a?(Integer) } && ids.uniq.length == 32
  rows.to_h { |row| [row.fetch('gameId'), row] }
rescue KeyError => e
  raise BindingError, "#{kind}_FIELD_MISSING|field=#{e.key}"
end

def assert_manifest(manifest, manifest_path, expected_sha)
  actual_sha = sha256(manifest_path)
  raise BindingError, 'MANIFEST_SHA_MISMATCH' unless actual_sha == expected_sha
  raise BindingError, 'MANIFEST_KIND_INVALID' unless manifest['manifestKind'] == 'runtime-metadata-canonical'
  raise BindingError, 'MANIFEST_STATUS_INVALID' unless manifest['manifestStatus'] == 'SIGNED_CANONICAL_INPUT' && manifest['consumable'] == true
  require_text(manifest['sourceContract']['contractVersion'], 'MANIFEST_CONTRACT_VERSION_INVALID')
  rows_by_id(manifest['rows'], 'MANIFEST')
  manifest['rows'].each do |row|
    %w[gameId gameCode family playVersion regionCode snapshotSchemaVersion cardCodecVersion eventInterpreterVersion protocolVersion sourceHash releaseId indexGeneration contractVersion].each do |field|
      raise BindingError, "MANIFEST_FIELD_MISSING|field=#{field}|gameId=#{row['gameId']}" if row[field].nil? || row[field].to_s.empty?
    end
    require_hex(row['sourceHash'], "MANIFEST_SOURCE_HASH_INVALID|gameId=#{row['gameId']}")
  end
  manifest['releaseEdition'].to_s.empty? && (raise BindingError, 'MANIFEST_RELEASE_EDITION_MISSING')
  actual_sha
end

def assert_registry(registry, registry_path, manifest, manifest_path, manifest_sha)
  raise BindingError, 'REGISTRY_KIND_INVALID' unless registry['manifestKind'] == 'canonical-identity-runtime-registry'
  raise BindingError, 'REGISTRY_STATUS_INVALID' unless registry['registryStatus'] == 'SIGNED_CANONICAL_INPUT' && registry['consumable'] == true
  digest = registry.dig('sourceDigests', 'releaseManifest')
  manifest_rows = rows_by_id(manifest['rows'], 'MANIFEST')
  registry_rows = rows_by_id(registry['rows'] || registry['plays'], 'REGISTRY')
  fields = %w[gameCode family playVersion regionCode contractVersion sourceHash releaseId indexGeneration]
  registry_rows.each do |game_id, row|
    manifest_row = manifest_rows[game_id]
    raise BindingError, "REGISTRY_ID_NOT_IN_MANIFEST|gameId=#{game_id}" if manifest_row.nil?
    fields.each do |field|
      next if row[field] == manifest_row[field]
      raise BindingError, "REGISTRY_MISMATCH|field=#{field}|gameId=#{game_id}"
    end
    %w[providerKey clientRuntimeEntry].each do |field|
      raise BindingError, "REGISTRY_FIELD_MISSING|field=#{field}|gameId=#{game_id}" if row[field].to_s.empty?
    end
  end
  raise BindingError, 'REGISTRY_RELEASE_EDITION_MISMATCH' unless registry['releaseEdition'] == manifest['releaseEdition']
  raise BindingError, 'REGISTRY_MANIFEST_BINDING_MISSING' unless digest.is_a?(Hash)
  raise BindingError, 'REGISTRY_MANIFEST_PATH_MISMATCH' unless digest['path'] == manifest_path
  raise BindingError, 'REGISTRY_MANIFEST_SHA_MISMATCH' unless digest['sha256'] == manifest_sha
  registry_sha = sha256(registry_path)
  [registry_rows, registry_sha]
end

def assert_contract(contract, manifest, manifest_sha, registry_sha, registry_rows)
  raise BindingError, 'CONTRACT_KIND_INVALID' unless contract['manifestKind'] == 'canonical-identity-current-contract'
  raise BindingError, 'CONTRACT_STATUS_INVALID' unless contract['status'] == 'SIGNED_CANONICAL_INPUT'
  %w[manifestSha256 registrySha256 releaseEdition contractVersion rows sourceHash].each do |field|
    raise BindingError, "CONTRACT_SOURCE_MANIFEST_BINDING_MISSING|field=#{field}" if contract[field].nil?
  end
  require_hex(contract['manifestSha256'], 'CONTRACT_MANIFEST_SHA_INVALID')
  require_hex(contract['registrySha256'], 'CONTRACT_REGISTRY_SHA_INVALID')
  raise BindingError, 'CONTRACT_MANIFEST_SHA_MISMATCH' unless contract['manifestSha256'] == manifest_sha
  raise BindingError, 'CONTRACT_REGISTRY_SHA_MISMATCH' unless contract['registrySha256'] == registry_sha
  raise BindingError, 'CONTRACT_RELEASE_EDITION_MISMATCH' unless contract['releaseEdition'] == manifest['releaseEdition']
  raise BindingError, 'CONTRACT_VERSION_MISMATCH' unless contract['contractVersion'] == manifest['sourceContract']['contractVersion']
  contract_rows = rows_by_id(contract['rows'], 'CONTRACT')
  fields = %w[gameCode providerKey clientRuntimeEntry contractVersion sourceHash releaseId indexGeneration]
  registry_rows.each do |game_id, registry_row|
    contract_row = contract_rows[game_id]
    raise BindingError, "CONTRACT_ID_NOT_IN_REGISTRY|gameId=#{game_id}" if contract_row.nil?
    fields.each do |field|
      next if contract_row[field] == registry_row[field]
      raise BindingError, "CONTRACT_MISMATCH|field=#{field}|gameId=#{game_id}"
    end
  end
  digest_payload = {
    'contractVersion' => contract['contractVersion'],
    'manifestSha256' => contract['manifestSha256'],
    'registrySha256' => contract['registrySha256'],
    'releaseEdition' => contract['releaseEdition'],
    'rows' => contract['rows']
  }
  expected = Digest::SHA256.hexdigest(JSON.generate(digest_payload))
  raise BindingError, 'CONTRACT_SOURCE_HASH_MISMATCH' unless contract['sourceHash'] == expected
end

options = {}
OptionParser.new do |parser|
  parser.on('--manifest PATH') { |v| options[:manifest] = v }
  parser.on('--manifest-sha256 SHA') { |v| options[:manifest_sha256] = v }
  parser.on('--registry PATH') { |v| options[:registry] = v }
  parser.on('--contract-source PATH') { |v| options[:contract] = v }
  parser.on('--output PATH') { |v| options[:output] = v }
end.parse!
%i[manifest manifest_sha256 registry contract output].each do |key|
  raise BindingError, "#{key}=REQUIRED" if options[key].to_s.empty?
end

begin
  manifest = read_json(options[:manifest])
  registry = read_json(options[:registry])
  contract = read_json(options[:contract])
  manifest_sha = assert_manifest(manifest, options[:manifest], options[:manifest_sha256])
  registry_rows, registry_sha = assert_registry(registry, options[:registry], manifest, options[:manifest], manifest_sha)
  assert_contract(contract, manifest, manifest_sha, registry_sha, registry_rows)
  projection = {
    'consumable' => true,
    'manifestSha256' => manifest_sha,
    'registrySha256' => registry_sha,
    'contractSourceSha256' => sha256(options[:contract]),
    'releaseEdition' => manifest['releaseEdition'],
    'contractVersion' => manifest['sourceContract']['contractVersion'],
    'rowCount' => registry_rows.length
  }
  tmp = "#{options[:output]}.tmp.#{$$}"
  File.write(tmp, JSON.pretty_generate(projection) + "\n")
  File.rename(tmp, options[:output])
  puts JSON.generate(projection)
rescue BindingError => e
  warn e.message
  exit 1
end
