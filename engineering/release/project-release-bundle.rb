#!/usr/bin/env ruby
# frozen_string_literal: true

# Release-only projection consistency gate. Every input is explicit on the
# command line. This script never reads a checkout default, ENV, database,
# snapshot, or generated bundle and never derives an identity field from an id.

require 'digest'
require 'fileutils'
require 'json'
require 'open3'
require 'optparse'
require 'rbconfig'

PROJECT_ROOT = File.expand_path('../..', __dir__).freeze
PROJECTION_ROLES = %w[client server provider lobby gateway db].freeze
REQUIRED_FIELDS = %w[
  gameId gameCode family playVersion regionCode providerKey clientRuntimeEntry
  contractVersion snapshotSchemaVersion cardCodecVersion eventInterpreterVersion
  protocolVersion sourceHash releaseId indexGeneration
].freeze
TEXT_FIELDS = %w[
  gameCode family playVersion regionCode providerKey clientRuntimeEntry
  contractVersion snapshotSchemaVersion cardCodecVersion eventInterpreterVersion
  protocolVersion
].freeze
HASH_FIELDS = %w[sourceHash].freeze
VERSION_FIELDS = %w[
  snapshotSchemaVersion cardCodecVersion eventInterpreterVersion protocolVersion
].freeze
SHA256 = /\A[0-9a-f]{64}\z/.freeze
VERSION = /\A[A-Za-z0-9][A-Za-z0-9._-]{0,127}\z/.freeze
IDENTITY_CATALOG_HEADER = "gameId\tgameCode\tplayVersion\tfamily".freeze
PAYLOAD_VALIDATOR = File.join(PROJECT_ROOT, 'engineering/local-dev/task-only-runtime/validate-bundle-payload.rb').freeze

def add_error(errors, code, details = {})
  entry = { 'code' => code }.merge(details.transform_keys(&:to_s))
  errors << entry unless errors.include?(entry)
end

def read_json(path, errors, role)
  unless path && File.file?(path)
    add_error(errors, 'INPUT_MISSING', 'role' => role, 'path' => path.to_s)
    return nil
  end
  JSON.parse(File.binread(path))
rescue JSON::ParserError => error
  add_error(errors, 'INPUT_JSON_INVALID', 'role' => role, 'path' => path, 'message' => error.message)
  nil
rescue SystemCallError => error
  add_error(errors, 'INPUT_READ_FAILED', 'role' => role, 'path' => path, 'message' => error.message)
  nil
end

def sha256_file(path)
  Digest::SHA256.file(path).hexdigest
end

def nonblank_string?(value)
  value.is_a?(String) && !value.strip.empty?
end

def validate_registry(registry, path, errors)
  unless registry.is_a?(Hash)
    add_error(errors, 'IDENTITY_REGISTRY_OBJECT_REQUIRED', 'path' => path)
    return {}
  end
  add_error(errors, 'IDENTITY_REGISTRY_KIND_INVALID', 'path' => path) unless registry['manifestKind'] == 'canonical-identity-runtime-registry'
  add_error(errors, 'IDENTITY_REGISTRY_SCHEMA_INVALID', 'path' => path) unless registry['schemaVersion'] == 1
  add_error(errors, 'IDENTITY_REGISTRY_STATUS_INVALID', 'path' => path) unless registry['registryStatus'] == 'SIGNED_CANONICAL_INPUT'
  add_error(errors, 'IDENTITY_REGISTRY_NOT_CONSUMABLE', 'path' => path) unless registry['consumable'] == true
  add_error(errors, 'IDENTITY_REGISTRY_NO_FALLBACKS_REQUIRED', 'path' => path) unless registry.dig('sourceContract', 'noFallbacks') == true
  add_error(errors, 'IDENTITY_REGISTRY_NO_GAME_ID_DERIVATION_REQUIRED', 'path' => path) unless registry.dig('sourceContract', 'noGameIdDerivation') == true
  add_error(errors, 'IDENTITY_REGISTRY_REQUIRED_FIELDS_INVALID', 'path' => path) unless registry['requiredFields'] == REQUIRED_FIELDS

  rows = registry['rows']
  unless rows.is_a?(Array) && rows.length == 32
    add_error(errors, 'IDENTITY_REGISTRY_ROW_COUNT_INVALID', 'path' => path, 'actual' => rows.is_a?(Array) ? rows.length : nil, 'expected' => 32)
    return {}
  end

  by_id = {}
  rows.each_with_index do |row, index|
    unless row.is_a?(Hash)
      add_error(errors, 'IDENTITY_ROW_OBJECT_REQUIRED', 'index' => index)
      next
    end
    game_id = row['gameId']
    if !game_id.is_a?(Integer) || game_id <= 0
      add_error(errors, 'IDENTITY_GAME_ID_INVALID', 'index' => index)
      next
    end
    add_error(errors, 'IDENTITY_GAME_ID_DUPLICATE', 'gameId' => game_id) if by_id.key?(game_id)
    by_id[game_id] ||= row
    REQUIRED_FIELDS.each do |field|
      add_error(errors, 'IDENTITY_FIELD_MISSING', 'gameId' => game_id, 'field' => field) unless row.key?(field)
    end
    TEXT_FIELDS.each do |field|
      value = row[field]
      add_error(errors, 'IDENTITY_FIELD_MISSING', 'gameId' => game_id, 'field' => field) unless nonblank_string?(value)
    end
    if row['contractVersion'] == 'compatibility-v1'
      add_error(errors, 'IDENTITY_CONTRACT_COMPATIBILITY_VALUE', 'gameId' => game_id)
    end
    VERSION_FIELDS.each do |field|
      value = row[field]
      add_error(errors, 'IDENTITY_VERSION_INVALID', 'gameId' => game_id, 'field' => field) unless nonblank_string?(value) && VERSION.match?(value)
    end
    HASH_FIELDS.each do |field|
      value = row[field]
      add_error(errors, 'IDENTITY_HASH_INVALID', 'gameId' => game_id, 'field' => field) unless value.is_a?(String) && SHA256.match?(value)
    end
    %w[releaseId indexGeneration].each do |field|
      value = row[field]
      add_error(errors, 'IDENTITY_NUMBER_INVALID', 'gameId' => game_id, 'field' => field) unless value.is_a?(Integer) && value.positive?
    end
    add_error(errors, 'IDENTITY_ROW_NOT_CONSUMABLE', 'gameId' => game_id) unless row['status'] == 'CONSUMABLE'
  end
  by_id
end

def validate_current_catalog(path, identity_rows, errors)
  unless path && File.file?(path) && !File.symlink?(path)
    add_error(errors, 'CURRENT_CATALOG_INPUT_REQUIRED', 'path' => path.to_s)
    return {}
  end
  lines = File.readlines(path, chomp: true)
  unless lines.shift == IDENTITY_CATALOG_HEADER
    add_error(errors, 'CURRENT_CATALOG_HEADER_INVALID', 'path' => path)
    return {}
  end
  rows = lines.reject { |line| line.empty? || line.start_with?('#') }.map.with_index do |line, index|
    fields = line.split("\t", -1)
    unless fields.length == 4
      add_error(errors, 'CURRENT_CATALOG_ROW_INVALID', 'path' => path, 'index' => index)
      next
    end
    id = Integer(fields[0], 10) rescue nil
    unless id&.positive?
      add_error(errors, 'CURRENT_CATALOG_ID_INVALID', 'path' => path, 'index' => index)
      next
    end
    row = { 'gameId' => id, 'gameCode' => fields[1], 'playVersion' => fields[2], 'family' => fields[3] }
    add_error(errors, 'CURRENT_CATALOG_COMPATIBILITY_IDENTITY', 'gameId' => id) if row['family'].downcase.start_with?('word-card:')
    row
  end.compact
  if rows.length != 32
    add_error(errors, 'CURRENT_CATALOG_ROW_COUNT_INVALID', 'path' => path, 'actual' => rows.length, 'expected' => 32)
    return {}
  end
  by_id = rows.to_h { |row| [row['gameId'], row] }
  add_error(errors, 'CURRENT_CATALOG_DUPLICATE_ID', 'path' => path) unless by_id.length == 32
  unless by_id.keys.sort == identity_rows.keys.sort
    add_error(errors, 'CURRENT_CATALOG_ID_SET_DRIFT', 'missing' => identity_rows.keys.sort - by_id.keys.sort,
              'extra' => by_id.keys.sort - identity_rows.keys.sort)
    return by_id
  end
  by_id.each do |game_id, row|
    %w[gameCode playVersion family].each do |field|
      next if row[field] == identity_rows.fetch(game_id)[field]
      add_error(errors, 'CURRENT_CATALOG_IDENTITY_DRIFT', 'gameId' => game_id, 'field' => field,
                'expected' => identity_rows.fetch(game_id)[field], 'actual' => row[field])
    end
  end
  by_id
rescue SystemCallError => error
  add_error(errors, 'CURRENT_CATALOG_READ_FAILED', 'path' => path, 'message' => error.message)
  {}
end

def validate_payload_manifest(path, role, catalog_path, registry_path, errors)
  unless path && File.file?(path) && !File.symlink?(path)
    add_error(errors, 'PAYLOAD_MANIFEST_INPUT_REQUIRED', 'role' => role, 'path' => path.to_s)
    return
  end
  command = [RbConfig.ruby, PAYLOAD_VALIDATOR, '--manifest', path, '--bundle-root', File.dirname(path),
             '--catalog', catalog_path]
  command.concat(['--identity-registry', registry_path]) if role == 'current-production'
  _stdout, stderr, status = Open3.capture3(*command)
  return if status.success?
  details = { 'role' => role, 'path' => path }
  marker = 'BUNDLE_PAYLOAD_REJECTED|'
  if (index = stderr.index(marker))
    begin
      details['validator'] = JSON.parse(stderr[(index + marker.length)..])
    rescue JSON::ParserError
      details['message'] = stderr.strip
    end
  else
    details['message'] = stderr.strip
  end
  add_error(errors, 'PAYLOAD_MANIFEST_REJECTED', details)
end

def validate_projection(doc, path, role, identity_rows, errors)
  unless doc.is_a?(Hash)
    add_error(errors, 'PROJECTION_OBJECT_REQUIRED', 'role' => role, 'path' => path)
    return {}
  end
  add_error(errors, 'PROJECTION_KIND_INVALID', 'role' => role, 'path' => path) unless doc['projectionKind'] == role
  rows = doc['rows']
  unless rows.is_a?(Array)
    add_error(errors, 'PROJECTION_ROWS_REQUIRED', 'role' => role, 'path' => path)
    return {}
  end

  by_id = {}
  rows.each_with_index do |row, index|
    unless row.is_a?(Hash)
      add_error(errors, 'PROJECTION_ROW_OBJECT_REQUIRED', 'role' => role, 'index' => index)
      next
    end
    game_id = row['gameId']
    unless game_id.is_a?(Integer) && game_id.positive?
      add_error(errors, 'PROJECTION_GAME_ID_INVALID', 'role' => role, 'index' => index)
      next
    end
    add_error(errors, 'PROJECTION_GAME_ID_DUPLICATE', 'role' => role, 'gameId' => game_id) if by_id.key?(game_id)
    by_id[game_id] ||= row
    expected = identity_rows[game_id]
    unless expected
      add_error(errors, 'PROJECTION_GAME_ID_UNKNOWN', 'role' => role, 'gameId' => game_id)
      next
    end
    REQUIRED_FIELDS.each do |field|
      unless row.key?(field) && !row[field].nil?
        add_error(errors, 'PROJECTION_FIELD_MISSING', 'role' => role, 'gameId' => game_id, 'field' => field)
        next
      end
      next if row[field] == expected[field]
      add_error(errors, 'PROJECTION_FIELD_DRIFT', 'role' => role, 'gameId' => game_id,
                'field' => field, 'expected' => expected[field], 'actual' => row[field])
    end
  end

  expected_ids = identity_rows.keys.sort
  actual_ids = by_id.keys.sort
  unless actual_ids == expected_ids
    add_error(errors, 'PROJECTION_ID_SET_DRIFT', 'role' => role,
              'missing' => expected_ids - actual_ids, 'extra' => actual_ids - expected_ids)
  end
  by_id
end

def validate_cross_role(projections, errors)
  first_role = PROJECTION_ROLES.first
  first_rows = projections.fetch(first_role, {})
  PROJECTION_ROLES.drop(1).each do |role|
    first_rows.each do |game_id, expected|
      actual = projections.fetch(role, {})[game_id]
      next unless actual.is_a?(Hash)
      REQUIRED_FIELDS.each do |field|
        next if actual[field] == expected[field]
        add_error(errors, 'PROJECTION_CROSS_ROLE_DRIFT', 'roles' => [first_role, role],
                  'gameId' => game_id, 'field' => field,
                  'expected' => expected[field], 'actual' => actual[field])
      end
    end
  end
end

def forbidden_output_path?(path)
  absolute = File.expand_path(path)
  absolute == PROJECT_ROOT || absolute.start_with?(PROJECT_ROOT + File::SEPARATOR)
end

options = { identity_registry: nil, current_catalog: nil, projections: [], production_payload_manifest: nil,
            compatibility_payload_manifest: nil, output: nil }
begin
  OptionParser.new do |parser|
    parser.banner = 'Usage: project-release-bundle.rb --identity-registry PATH --current-catalog PATH --production-payload-manifest PATH --compatibility-payload-manifest PATH --projection ROLE=PATH [--projection ROLE=PATH ...] [--output PATH]'
    parser.on('--identity-registry PATH', 'explicit canonical identity registry') { |value| options[:identity_registry] = value }
    parser.on('--current-catalog PATH', 'explicit current 32-identity TSV') { |value| options[:current_catalog] = value }
    parser.on('--production-payload-manifest PATH', 'explicit current-production payload manifest') { |value| options[:production_payload_manifest] = value }
    parser.on('--compatibility-payload-manifest PATH', 'explicit historical-compatibility payload manifest') { |value| options[:compatibility_payload_manifest] = value }
    parser.on('--projection ROLE=PATH', 'explicit projection JSON; repeat once for each required role') { |value| options[:projections] << value }
    parser.on('--output PATH', 'optional staging output; must be outside the repository') { |value| options[:output] = value }
  end.parse!
rescue OptionParser::ParseError => error
  warn "RELEASE_BUNDLE_ARGUMENTS_INVALID|#{error.message}"
  exit 2
end

errors = []
add_error(errors, 'IDENTITY_REGISTRY_ARGUMENT_REQUIRED') if options[:identity_registry].to_s.empty?
add_error(errors, 'CURRENT_CATALOG_ARGUMENT_REQUIRED') if options[:current_catalog].to_s.empty?
add_error(errors, 'PRODUCTION_PAYLOAD_MANIFEST_ARGUMENT_REQUIRED') if options[:production_payload_manifest].to_s.empty?
add_error(errors, 'COMPATIBILITY_PAYLOAD_MANIFEST_ARGUMENT_REQUIRED') if options[:compatibility_payload_manifest].to_s.empty?
projection_specs = {}
options[:projections].each do |spec|
  role, separator, path = spec.to_s.partition('=')
  if separator.empty? || !PROJECTION_ROLES.include?(role) || path.empty?
    add_error(errors, 'PROJECTION_ARGUMENT_INVALID', 'value' => spec)
    next
  end
  add_error(errors, 'PROJECTION_ROLE_DUPLICATE', 'role' => role) if projection_specs.key?(role)
  projection_specs[role] = path
end
unless projection_specs.keys.sort == PROJECTION_ROLES.sort
  add_error(errors, 'PROJECTION_ROLE_SET_INVALID', 'expected' => PROJECTION_ROLES, 'actual' => projection_specs.keys.sort)
end

registry = read_json(options[:identity_registry], errors, 'identity-registry') unless options[:identity_registry].to_s.empty?
identity_rows = registry ? validate_registry(registry, options[:identity_registry], errors) : {}
catalog_rows = validate_current_catalog(options[:current_catalog], identity_rows, errors)
projections = {}
projection_specs.each do |role, path|
  doc = read_json(path, errors, role)
  projections[role] = validate_projection(doc, path, role, identity_rows, errors) if doc
end
validate_cross_role(projections, errors) if projections.length == PROJECTION_ROLES.length
if !options[:identity_registry].to_s.empty? && !options[:current_catalog].to_s.empty?
  validate_payload_manifest(options[:production_payload_manifest], 'current-production', options[:current_catalog], options[:identity_registry], errors)
  validate_payload_manifest(options[:compatibility_payload_manifest], 'historical-compatibility', options[:current_catalog], nil, errors)
end

if options[:output] && forbidden_output_path?(options[:output])
  add_error(errors, 'OUTPUT_REPOSITORY_PATH_FORBIDDEN', 'path' => File.expand_path(options[:output]))
end

unless errors.empty?
  payload = { 'ok' => false, 'status' => 'FAIL_CLOSED', 'errors' => errors }
  warn "RELEASE_BUNDLE_REJECTED|#{JSON.generate(payload)}"
  exit 1
end

candidate = {
  'manifestKind' => 'release-bundle-candidate',
  'schemaVersion' => 1,
  'status' => 'CANDIDATE_VALIDATED',
  'projectionRoles' => PROJECTION_ROLES,
  'identityRegistry' => {
    'path' => options[:identity_registry],
    'sha256' => sha256_file(options[:identity_registry])
  },
  'currentCatalog' => {
    'path' => options[:current_catalog],
    'sha256' => sha256_file(options[:current_catalog]),
    'rowCount' => catalog_rows.length
  },
  'payloadManifests' => {
    'current-production' => {
      'path' => options[:production_payload_manifest],
      'sha256' => sha256_file(options[:production_payload_manifest])
    },
    'historical-compatibility' => {
      'path' => options[:compatibility_payload_manifest],
      'sha256' => sha256_file(options[:compatibility_payload_manifest])
    }
  },
  'projections' => PROJECTION_ROLES.to_h do |role|
    path = projection_specs.fetch(role)
    [role, { 'path' => path, 'sha256' => sha256_file(path), 'rowCount' => projections.fetch(role).length }]
  end,
  'rows' => identity_rows.keys.sort.map { |game_id| identity_rows.fetch(game_id) }
}

if options[:output]
  output = File.expand_path(options[:output])
  if File.symlink?(output)
    warn "RELEASE_BUNDLE_REJECTED|#{JSON.generate('ok' => false, 'status' => 'FAIL_CLOSED', 'errors' => [{ 'code' => 'OUTPUT_SYMLINK_FORBIDDEN', 'path' => output }])}"
    exit 1
  end
  FileUtils.mkdir_p(File.dirname(output))
  temporary = "#{output}.tmp.#{$$}"
  File.binwrite(temporary, JSON.pretty_generate(candidate) + "\n")
  File.rename(temporary, output)
  candidate['output'] = output
end
puts JSON.generate(candidate.merge('ok' => true))
