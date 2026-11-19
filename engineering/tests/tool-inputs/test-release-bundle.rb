#!/usr/bin/env ruby
# frozen_string_literal: true

# Contract tests for the release-only generator. They deliberately use a
# temporary copy of the current registry and never write a release bundle.

require 'json'
require 'digest'
require 'fileutils'
require 'open3'
require 'tmpdir'

ROOT = File.expand_path('../../..', __dir__)
GENERATOR = File.join(ROOT, 'engineering/release/project-release-bundle.rb')
REGISTRY = File.join(ROOT, 'release-input/identity/canonical-identity-registry.json')
ROLES = %w[client server provider lobby gateway db].freeze

def assert(condition, message)
  raise message unless condition
end

def deep_copy(value)
  Marshal.load(Marshal.dump(value))
end

def command_for(registry, projections, catalog:, production_manifest:, compatibility_manifest:, omit: [], output: nil)
  command = ['ruby', GENERATOR, '--identity-registry', registry, '--current-catalog', catalog,
             '--production-payload-manifest', production_manifest,
             '--compatibility-payload-manifest', compatibility_manifest]
  projections.each do |role, path|
    next if omit.include?(role)
    command.concat(['--projection', "#{role}=#{path}"])
  end
  command.concat(['--output', output]) if output
  command
end

def run_generator(registry, projections, catalog:, production_manifest:, compatibility_manifest:, omit: [], output: nil)
  Open3.capture3(*command_for(registry, projections, catalog: catalog,
                              production_manifest: production_manifest,
                              compatibility_manifest: compatibility_manifest,
                              omit: omit, output: output))
end

def rejection(stderr)
  marker = 'RELEASE_BUNDLE_REJECTED|'
  index = stderr.index(marker)
  raise "missing machine-readable rejection: #{stderr}" unless index
  JSON.parse(stderr[(index + marker.length)..])
end

Dir.mktmpdir('aoo-release-bundle-contract') do |directory|
  source = JSON.parse(File.binread(REGISTRY))
  catalog = File.join(directory, 'current-game-identities.tsv')
  FileUtils.cp(File.join(ROOT, 'server/GameSPI/src/main/resources/current-game-identities.tsv'), catalog)
  payload_root = File.join(directory, 'payload')
  FileUtils.mkdir_p(File.join(payload_root, 'WordCard'))
  production_payload = File.join(payload_root, 'production-payload.json')
  compatibility_payload = File.join(payload_root, 'compatibility-payload.json')
  bootstrap_payload = File.join(payload_root, 'bootstrap.jar')
  wordcard_payload = File.join(payload_root, 'WordCard/wordcard-registry.jar')
  File.binwrite(bootstrap_payload, 'current-production-payload')
  File.binwrite(wordcard_payload, 'historical-wordcard-payload')
  write_payload_manifest = lambda do |path, role, rows, payload_path, payload_kind, extra|
    relative = payload_path.delete_prefix("#{payload_root}/")
    manifest = {
      'manifestKind' => 'release-bundle-payload', 'schemaVersion' => 1,
      'status' => 'CANDIDATE_VALIDATED', 'bundleRole' => role,
      'catalog' => { 'cardinality' => role == 'current-production' ? 32 : rows.length },
      'identityRows' => rows,
      'payloadCount' => 1,
      'classpath' => [relative],
      'payload' => [{ 'path' => relative, 'sha256' => Digest::SHA256.file(payload_path).hexdigest, 'kind' => payload_kind }]
    }.merge(extra)
    File.write(path, JSON.pretty_generate(manifest) + "\n")
  end
  write_payload_manifest.call(
    production_payload, 'current-production', source.fetch('rows'), bootstrap_payload, 'runtime-jar',
    'admission' => { 'newRoom' => 'current-production', 'lobby' => 'current-production',
                     'reconnect' => 'current-production', 'handoff' => 'current-production',
                     'historicalSnapshot' => 'excluded' }
  )
  write_payload_manifest.call(
    compatibility_payload, 'historical-compatibility',
    [{ 'gameId' => 136, 'gameCode' => 'byzp', 'playVersion' => '1.0.0', 'family' => 'word-card:pao-hu-zi' }],
    wordcard_payload, 'compatibility-registry',
    'compatibility' => {
      'oldSnapshotEntry' => 'historical-compatibility/old-snapshot',
      'admissionMode' => 'explicit-historical-snapshot-only', 'wordCardAllowed' => true,
      'forbiddenCurrentRoutes' => { 'newRoom' => true, 'lobby' => true, 'reconnect' => true, 'handoff' => true }
    }
  )

  valid_projections = {}
  ROLES.each do |role|
    valid_projections[role] = File.join(directory, "valid-#{role}.json")
    File.write(valid_projections[role], JSON.pretty_generate({ 'projectionKind' => role, 'rows' => deep_copy(source['rows']) }) + "\n")
  end
  valid_output = File.join(directory, 'valid-candidate.json')
  _valid_stdout, valid_stderr, valid_status = run_generator(
    REGISTRY, valid_projections, catalog: catalog, production_manifest: production_payload,
    compatibility_manifest: compatibility_payload, output: valid_output
  )
  assert(valid_status.success?, "valid bundle admission unexpectedly rejected: #{valid_stderr}")
  valid_candidate = JSON.parse(File.binread(valid_output))
  assert(valid_candidate.dig('currentCatalog', 'rowCount') == 32, 'production catalog count was not recorded')
  assert(valid_candidate.dig('payloadManifests', 'current-production'), 'production payload manifest was not recorded')
  assert(valid_candidate.dig('payloadManifests', 'historical-compatibility'), 'compatibility payload manifest was not recorded')

  altered_catalog = File.join(directory, 'altered-current-game-identities.tsv')
  File.write(altered_catalog,
             File.read(catalog, encoding: 'UTF-8').sub(
               "9\tquanguo2997\tcn297-v1.0.0\tpoker:compare-hand",
               "9\tquanguo2997\tcn297-v1.0.0\tword-card:unexpected"))
  _stdout, catalog_stderr, catalog_status = run_generator(
    REGISTRY, valid_projections, catalog: altered_catalog,
    production_manifest: production_payload, compatibility_manifest: compatibility_payload
  )
  assert(!catalog_status.success?, 'current catalog identity drift was accepted')
  catalog_codes = rejection(catalog_stderr).fetch('errors').map { |entry| entry.fetch('code') }
  assert(catalog_codes.include?('CURRENT_CATALOG_IDENTITY_DRIFT'), 'current catalog identity drift was not rejected')

  # Preserve the real current rows but force the known C-identity dependency
  # into its fail-closed state. No identity value is invented by this fixture.
  blocked = deep_copy(source)
  blocked['consumable'] = false
  blocked['rows'].each { |row| row['contractVersion'] = nil }
  blocked_path = File.join(directory, 'identity-registry-blocked.json')
  File.write(blocked_path, JSON.pretty_generate(blocked) + "\n")

  projections = {}
  ROLES.each do |role|
    projections[role] = File.join(directory, "#{role}.json")
    File.write(projections[role], JSON.pretty_generate({ 'projectionKind' => role, 'rows' => deep_copy(blocked['rows']) }) + "\n")
  end

  _stdout, stderr, status = run_generator(blocked_path, projections, catalog: catalog,
                                           production_manifest: production_payload,
                                           compatibility_manifest: compatibility_payload,
                                           output: File.join(directory, 'candidate.json'))
  assert(!status.success?, 'incomplete identity contract unexpectedly accepted')
  payload = rejection(stderr)
  codes = payload.fetch('errors').map { |entry| entry.fetch('code') }
  assert(codes.include?('IDENTITY_REGISTRY_NOT_CONSUMABLE'), 'missing registry dependency rejection')
  assert(codes.include?('IDENTITY_FIELD_MISSING'), 'missing contractVersion rejection')
  assert(codes.include?('PROJECTION_FIELD_MISSING'), 'projection contract field rejection')
  assert(!File.exist?(File.join(directory, 'candidate.json')), 'failed input wrote a candidate bundle')

  # Even an explicitly supplied repository path cannot become a generated
  # bundle output; staging must remain outside the checkout.
  forbidden_output = File.join(ROOT, 'work/release/should-not-write.json')
  _stdout, forbidden_stderr, forbidden_status = run_generator(blocked_path, projections, catalog: catalog,
                                                               production_manifest: production_payload,
                                                               compatibility_manifest: compatibility_payload,
                                                               output: forbidden_output)
  assert(!forbidden_status.success?, 'repository output path unexpectedly accepted')
  assert(rejection(forbidden_stderr).fetch('errors').any? { |entry| entry['code'] == 'OUTPUT_REPOSITORY_PATH_FORBIDDEN' },
         'repository output path was not rejected')
  assert(!File.exist?(forbidden_output), 'repository output path was written')

  # A role omission cannot be silently treated as an empty projection.
  _stdout, missing_role_stderr, missing_role_status = run_generator(blocked_path, projections, catalog: catalog,
                                                                      production_manifest: production_payload,
                                                                      compatibility_manifest: compatibility_payload,
                                                                      omit: ['db'])
  assert(!missing_role_status.success?, 'missing DB projection unexpectedly accepted')
  assert(rejection(missing_role_stderr).fetch('errors').any? { |entry| entry['code'] == 'PROJECTION_ROLE_SET_INVALID' },
         'missing projection role was not rejected')

  # Mutating only a release-bound field in one projection must be observable as
  # drift, even while the upstream identity dependency remains blocked.
  provider = JSON.parse(File.binread(projections.fetch('provider')))
  provider['rows'][0]['sourceHash'] = 'f' * 64
  provider['rows'][1]['releaseId'] = provider['rows'][0]['releaseId'] + 1
  File.write(projections.fetch('provider'), JSON.pretty_generate(provider) + "\n")
  _stdout, drift_stderr, drift_status = run_generator(blocked_path, projections, catalog: catalog,
                                                       production_manifest: production_payload,
                                                       compatibility_manifest: compatibility_payload)
  assert(!drift_status.success?, 'projection drift unexpectedly accepted')
  drift_codes = rejection(drift_stderr).fetch('errors').map { |entry| entry.fetch('code') }
  assert(drift_codes.include?('PROJECTION_FIELD_DRIFT'), 'sourceHash/releaseId drift was not rejected')
  assert(drift_codes.include?('PROJECTION_CROSS_ROLE_DRIFT'), 'cross-role projection drift was not rejected')

  # Duplicate rows and absent explicit input arguments remain fail-closed.
  client = JSON.parse(File.binread(projections.fetch('client')))
  client['rows'] << client['rows'].first
  File.write(projections.fetch('client'), JSON.pretty_generate(client) + "\n")
  _stdout, duplicate_stderr, duplicate_status = run_generator(blocked_path, projections, catalog: catalog,
                                                               production_manifest: production_payload,
                                                               compatibility_manifest: compatibility_payload)
  assert(!duplicate_status.success?, 'duplicate projection row unexpectedly accepted')
  assert(rejection(duplicate_stderr).fetch('errors').any? { |entry| entry['code'] == 'PROJECTION_GAME_ID_DUPLICATE' },
         'duplicate projection row was not rejected')

  _stdout, argument_stderr, argument_status = Open3.capture3('ruby', GENERATOR)
  assert(argument_status.exitstatus == 1, 'missing explicit input did not fail closed')
  assert(rejection(argument_stderr).fetch('errors').any? { |entry| entry['code'] == 'IDENTITY_REGISTRY_ARGUMENT_REQUIRED' },
         'missing identity registry argument was not recorded')

  puts JSON.generate(
    ok: true,
    status: 'READY_FOR_INDEPENDENT_REVIEW',
    negativeCases: %w[
      missing-contractVersion
      missing-projection-role
      sourceHash-drift
      releaseId-drift
      cross-role-drift
      duplicate-projection-row
      current-catalog-drift
      missing-explicit-input
      repository-output-path
    ]
  )
end
