#!/usr/bin/env ruby
# frozen_string_literal: true

# Validate an explicitly supplied payload manifest and its payload bytes. This
# is an admission contract only: it never creates, rewrites, or discovers a
# bundle. Production and historical Compatibility roles are deliberately
# disjoint so a Compatibility classpath cannot silently become a current one.

require 'digest'
require 'json'
require 'optparse'

PROJECT_ROOT = File.expand_path('../../../..', __dir__).freeze
SHA256 = /\A[0-9a-f]{64}\z/.freeze
CURRENT_FIELDS = %w[
  gameId gameCode family playVersion regionCode providerKey clientRuntimeEntry
  contractVersion snapshotSchemaVersion cardCodecVersion eventInterpreterVersion
  protocolVersion sourceHash releaseId indexGeneration
].freeze
IDENTITY_FIELDS = %w[gameId gameCode family playVersion].freeze
FORBIDDEN_COMPATIBILITY_MARKERS = /word[ _-]?card|byzp|xpphz|ahphz|glzp|yzchz|ycsdr|ychp|bzp|lczp|pxphz/i.freeze
CURRENT_ROUTE_KEYS = %w[newRoom lobby reconnect handoff].freeze

def reject!(code, details = {})
  payload = { 'ok' => false, 'status' => 'FAIL_CLOSED', 'errors' => [{ 'code' => code }.merge(details)] }
  warn "BUNDLE_PAYLOAD_REJECTED|#{JSON.generate(payload)}"
  exit 1
end

def require_hash(value, code)
  reject!(code) unless value.is_a?(Hash)
  value
end

def require_text(value, code)
  reject!(code) unless value.is_a?(String) && !value.strip.empty?
  value
end

def read_json(path, role)
  reject!('INPUT_MISSING', 'role' => role, 'path' => path.to_s) unless path && File.file?(path) && !File.symlink?(path)
  JSON.parse(File.binread(path))
rescue JSON::ParserError => error
  reject!('INPUT_JSON_INVALID', 'role' => role, 'path' => path, 'message' => error.message)
rescue SystemCallError => error
  reject!('INPUT_READ_FAILED', 'role' => role, 'path' => path, 'message' => error.message)
end

def sha256_file(path)
  Digest::SHA256.file(path).hexdigest
end

def project_relative(path)
  File.expand_path(path).sub(%r{\A#{Regexp.escape(PROJECT_ROOT)}/?}, '')
end

def read_current_catalog(path)
  reject!('CURRENT_CATALOG_MISSING', 'path' => path) unless File.file?(path) && !File.symlink?(path)
  lines = File.readlines(path, chomp: true)
  reject!('CURRENT_CATALOG_HEADER_INVALID', 'path' => path) unless lines.shift == "gameId\tgameCode\tplayVersion\tfamily"
  rows = lines.reject { |line| line.empty? || line.start_with?('#') }.map.with_index do |line, index|
    fields = line.split("\t", -1)
    reject!('CURRENT_CATALOG_ROW_INVALID', 'path' => path, 'index' => index) unless fields.length == 4
    id = Integer(fields[0], 10) rescue nil
    reject!('CURRENT_CATALOG_ID_INVALID', 'path' => path, 'index' => index) unless id&.positive?
    row = { 'gameId' => id, 'gameCode' => fields[1], 'playVersion' => fields[2], 'family' => fields[3] }
    %w[gameCode playVersion family].each { |field| reject!('CURRENT_CATALOG_FIELD_INVALID', 'field' => field, 'index' => index) unless row[field].is_a?(String) && !row[field].strip.empty? }
    reject!('CURRENT_CATALOG_COMPATIBILITY_IDENTITY', 'gameId' => id) if row['family'].downcase.start_with?('word-card:')
    row
  end
  reject!('CURRENT_CATALOG_CARDINALITY_INVALID', 'actual' => rows.length, 'expected' => 32) unless rows.length == 32
  ids = rows.map { |row| row['gameId'] }
  reject!('CURRENT_CATALOG_DUPLICATE_ID') unless ids.uniq.length == rows.length
  rows.to_h { |row| [row['gameId'], row] }
end

def read_registry(path)
  registry = read_json(path, 'identity-registry')
  reject!('IDENTITY_REGISTRY_KIND_INVALID') unless registry['manifestKind'] == 'canonical-identity-runtime-registry'
  rows = registry['rows']
  reject!('IDENTITY_REGISTRY_ROWS_INVALID') unless rows.is_a?(Array) && rows.length == 32
  by_id = rows.to_h do |row|
    reject!('IDENTITY_REGISTRY_ROW_INVALID') unless row.is_a?(Hash) && row['gameId'].is_a?(Integer)
    [row['gameId'], row]
  end
  reject!('IDENTITY_REGISTRY_DUPLICATE_ID') unless by_id.length == 32
  by_id
end

def validate_payload_entries(manifest, bundle_root)
  payload = manifest['payload']
  reject!('PAYLOAD_ENTRIES_REQUIRED') unless payload.is_a?(Array) && !payload.empty?
  reject!('PAYLOAD_COUNT_INVALID', 'actual' => payload.length, 'expected' => manifest['payloadCount']) unless manifest['payloadCount'] == payload.length
  seen = {}
  payload.each_with_index.map do |entry, index|
    reject!('PAYLOAD_ENTRY_INVALID', 'index' => index) unless entry.is_a?(Hash)
    path = entry['path']
    digest = entry['sha256']
    kind = entry['kind']
    reject!('PAYLOAD_PATH_INVALID', 'index' => index) unless path.is_a?(String) && !path.empty? && !path.start_with?('/') && !path.include?('..')
    reject!('PAYLOAD_SHA_INVALID', 'path' => path) unless digest.is_a?(String) && digest.match?(SHA256)
    reject!('PAYLOAD_KIND_INVALID', 'path' => path) unless kind.is_a?(String) && !kind.strip.empty?
    reject!('PAYLOAD_DUPLICATE_PATH', 'path' => path) if seen[path]
    seen[path] = true
    absolute = File.expand_path(path, bundle_root)
    reject!('PAYLOAD_PATH_ESCAPES_ROOT', 'path' => path) unless absolute.start_with?(File.expand_path(bundle_root) + File::SEPARATOR)
    reject!('PAYLOAD_FILE_MISSING', 'path' => path) unless File.file?(absolute) && !File.symlink?(absolute)
    actual = sha256_file(absolute)
    reject!('PAYLOAD_HASH_MISMATCH', 'path' => path, 'expected' => digest, 'actual' => actual) unless actual == digest
    { 'path' => path, 'sha256' => digest, 'kind' => kind }
  end
end

def validate_classpath(manifest, role, payload)
  classpath = manifest['classpath']
  reject!('CLASSPATH_REQUIRED', 'role' => role) unless classpath.is_a?(Array) && !classpath.empty?
  payload_paths = payload.map { |entry| entry['path'] }
  classpath.each do |path|
    reject!('CLASSPATH_ENTRY_INVALID', 'role' => role) unless path.is_a?(String) && !path.empty?
    reject!('CLASSPATH_ENTRY_NOT_PAYLOAD', 'role' => role, 'path' => path) unless payload_paths.include?(path)
    reject!('PRODUCTION_WORDCARD_CLASSPATH_FORBIDDEN', 'path' => path) if role == 'current-production' && path.match?(FORBIDDEN_COMPATIBILITY_MARKERS)
  end
  if role == 'historical-compatibility'
    registry_entry = payload.find { |entry| entry['path'].match?(FORBIDDEN_COMPATIBILITY_MARKERS) && entry['kind'].downcase.include?('registry') }
    reject!('COMPATIBILITY_REGISTRY_NOT_ON_CLASSPATH') unless registry_entry && classpath.include?(registry_entry['path'])
  end
end

def validate_rows(manifest, role, catalog, registry)
  rows = manifest['identityRows']
  reject!('IDENTITY_ROWS_REQUIRED', 'role' => role) unless rows.is_a?(Array)
  by_id = {}
  rows.each_with_index do |row, index|
    reject!('IDENTITY_ROW_INVALID', 'role' => role, 'index' => index) unless row.is_a?(Hash)
    id = row['gameId']
    reject!('IDENTITY_ROW_ID_INVALID', 'role' => role, 'index' => index) unless id.is_a?(Integer) && id.positive?
    reject!('IDENTITY_ROW_DUPLICATE', 'role' => role, 'gameId' => id) if by_id.key?(id)
    by_id[id] = row
    IDENTITY_FIELDS.each do |field|
      reject!('IDENTITY_ROW_FIELD_MISSING', 'role' => role, 'gameId' => id, 'field' => field) unless row[field].is_a?(String) && !row[field].strip.empty? || field == 'gameId'
    end
    reject!('IDENTITY_ROW_COMPATIBILITY_IN_PRODUCTION', 'gameId' => id) if role == 'current-production' && row['family'].to_s.downcase.start_with?('word-card:')
  end

  if role == 'current-production'
    reject!('PRODUCTION_CATALOG_CARDINALITY_INVALID') unless manifest.dig('catalog', 'cardinality') == 32
    reject!('PRODUCTION_IDENTITY_SET_DRIFT', 'missing' => catalog.keys - by_id.keys, 'extra' => by_id.keys - catalog.keys) unless by_id.keys.sort == catalog.keys.sort
    catalog.each do |id, expected|
      IDENTITY_FIELDS.each { |field| reject!('PRODUCTION_IDENTITY_DRIFT', 'gameId' => id, 'field' => field) unless by_id.fetch(id)[field] == expected[field] }
      next unless registry
      CURRENT_FIELDS.each { |field| reject!('PRODUCTION_REGISTRY_DRIFT', 'gameId' => id, 'field' => field) unless by_id.fetch(id)[field] == registry.fetch(id)[field] }
    end
  else
    reject!('COMPATIBILITY_CATALOG_EMPTY') if by_id.empty?
    reject!('COMPATIBILITY_WORDCARD_ENTRY_REQUIRED') unless by_id.values.any? { |row| row['family'].to_s.downcase.start_with?('word-card:') }
  end
  by_id
end

options = { manifest: nil, bundle_root: nil, catalog: nil, registry: nil }
begin
  OptionParser.new do |parser|
    parser.banner = 'Usage: validate-bundle-payload.rb --manifest PATH --bundle-root PATH --catalog PATH [--identity-registry PATH]'
    parser.on('--manifest PATH', 'explicit payload admission manifest') { |value| options[:manifest] = value }
    parser.on('--bundle-root PATH', 'explicit isolated payload root') { |value| options[:bundle_root] = value }
    parser.on('--catalog PATH', 'explicit current identity catalog TSV') { |value| options[:catalog] = value }
    parser.on('--identity-registry PATH', 'current registry required for production role') { |value| options[:registry] = value }
  end.parse!
rescue OptionParser::ParseError => error
  reject!('ARGUMENTS_INVALID', 'message' => error.message)
end

%i[manifest bundle_root catalog].each { |key| reject!('ARGUMENT_REQUIRED', 'argument' => key.to_s) if options[key].to_s.empty? }
bundle_root = File.expand_path(options[:bundle_root])
reject!('BUNDLE_ROOT_INVALID', 'path' => bundle_root) unless File.directory?(bundle_root) && !File.symlink?(bundle_root)
reject!('BUNDLE_ROOT_REPOSITORY_FORBIDDEN', 'path' => bundle_root) if bundle_root == PROJECT_ROOT || bundle_root.start_with?(PROJECT_ROOT + File::SEPARATOR)

manifest = read_json(options[:manifest], 'payload-manifest')
reject!('MANIFEST_KIND_INVALID') unless manifest['manifestKind'] == 'release-bundle-payload'
reject!('MANIFEST_SCHEMA_INVALID') unless manifest['schemaVersion'] == 1
reject!('MANIFEST_STATUS_INVALID') unless %w[CANDIDATE_VALIDATED SEALED].include?(manifest['status'])
role = manifest['bundleRole']
reject!('BUNDLE_ROLE_INVALID') unless %w[current-production historical-compatibility].include?(role)
catalog = read_current_catalog(options[:catalog])
registry = role == 'current-production' ? read_registry(options[:registry]) : nil
rows = validate_rows(manifest, role, catalog, registry)
payload = validate_payload_entries(manifest, bundle_root)
reject!('PRODUCTION_WORDCARD_PAYLOAD_FORBIDDEN') if role == 'current-production' && payload.any? { |entry| entry['path'].match?(FORBIDDEN_COMPATIBILITY_MARKERS) }
validate_classpath(manifest, role, payload)

if role == 'current-production'
  routes = require_hash(manifest['admission'], 'PRODUCTION_ADMISSION_REQUIRED')
  CURRENT_ROUTE_KEYS.each { |key| reject!('PRODUCTION_ROUTE_NOT_CURRENT', 'route' => key) unless routes[key] == 'current-production' }
  reject!('PRODUCTION_HISTORICAL_ENTRY_FORBIDDEN') unless routes['historicalSnapshot'] == 'excluded'
  reject!('PRODUCTION_COMPATIBILITY_REGISTRY_FORBIDDEN') if manifest.key?('compatibilityRegistry')
else
  compatibility = require_hash(manifest['compatibility'], 'COMPATIBILITY_CONTRACT_REQUIRED')
  require_text(compatibility['oldSnapshotEntry'], 'COMPATIBILITY_OLD_SNAPSHOT_ENTRY_REQUIRED')
  reject!('COMPATIBILITY_ADMISSION_MODE_INVALID') unless compatibility['admissionMode'] == 'explicit-historical-snapshot-only'
  reject!('COMPATIBILITY_WORDCARD_NOT_ALLOWED') unless compatibility['wordCardAllowed'] == true
  routes = require_hash(compatibility['forbiddenCurrentRoutes'], 'COMPATIBILITY_FORBIDDEN_ROUTES_REQUIRED')
  CURRENT_ROUTE_KEYS.each { |key| reject!('COMPATIBILITY_ROUTE_NOT_FORBIDDEN', 'route' => key) unless routes[key] == true }
  reject!('COMPATIBILITY_REGISTRY_ENTRY_REQUIRED') unless payload.any? { |entry| entry['path'].match?(FORBIDDEN_COMPATIBILITY_MARKERS) && entry['kind'].downcase.include?('registry') }
end

puts JSON.generate(
  'ok' => true,
  'status' => 'READY_FOR_INDEPENDENT_REVIEW',
  'bundleRole' => role,
  'identityCount' => rows.length,
  'payloadCount' => payload.length
)
