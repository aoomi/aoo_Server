#!/usr/bin/env ruby
# frozen_string_literal: true

require 'digest'
require 'fileutils'
require 'json'
require 'open3'
require 'tmpdir'

ROOT = File.expand_path('../../..', __dir__)
VALIDATOR = File.join(ROOT, 'engineering/local-dev/task-only-runtime/validate-bundle-payload.rb')
REGISTRY = File.join(ROOT, 'release-input/identity/canonical-identity-registry.json')
CATALOG = File.join(ROOT, 'modules/infrastructure/GameSPI/src/main/resources/current-game-identities.tsv')

def assert(condition, message)
  raise message unless condition
end

def rejected(stderr, code)
  marker = 'BUNDLE_PAYLOAD_REJECTED|'
  index = stderr.index(marker)
  raise "missing payload rejection: #{stderr}" unless index
  payload = JSON.parse(stderr[(index + marker.length)..])
  payload.fetch('errors').any? { |entry| entry['code'] == code }
end

def invoke(manifest, root, catalog: CATALOG, registry: nil)
  command = ['ruby', VALIDATOR, '--manifest', manifest, '--bundle-root', root, '--catalog', catalog]
  command.concat(['--identity-registry', registry]) if registry
  Open3.capture3(*command)
end

Dir.mktmpdir('aoo-bundle-payload-contract') do |directory|
  root = File.join(directory, 'bundle')
  FileUtils.mkdir_p(File.join(root, 'WordCard'))
  current_payload = File.join(root, 'bootstrap.jar')
  compatibility_payload = File.join(root, 'WordCard/wordcard-registry.jar')
  File.binwrite(current_payload, 'current payload')
  File.binwrite(compatibility_payload, 'historical payload')
  registry = JSON.parse(File.binread(REGISTRY))

  current_manifest = File.join(root, 'current.json')
  current_doc = {
    'manifestKind' => 'release-bundle-payload', 'schemaVersion' => 1,
    'status' => 'CANDIDATE_VALIDATED', 'bundleRole' => 'current-production',
    'catalog' => { 'cardinality' => 32 }, 'identityRows' => registry.fetch('rows'),
    'payloadCount' => 1,
    'classpath' => ['bootstrap.jar'],
    'payload' => [{ 'path' => 'bootstrap.jar', 'sha256' => Digest::SHA256.file(current_payload).hexdigest, 'kind' => 'runtime-jar' }],
    'admission' => { 'newRoom' => 'current-production', 'lobby' => 'current-production',
                     'reconnect' => 'current-production', 'handoff' => 'current-production',
                     'historicalSnapshot' => 'excluded' }
  }
  File.write(current_manifest, JSON.pretty_generate(current_doc) + "\n")
  _stdout, stderr, status = invoke(current_manifest, root, registry: REGISTRY)
  assert(status.success?, "current production payload unexpectedly rejected: #{stderr}")

  compatibility_manifest = File.join(root, 'compatibility.json')
  compatibility_doc = {
    'manifestKind' => 'release-bundle-payload', 'schemaVersion' => 1,
    'status' => 'CANDIDATE_VALIDATED', 'bundleRole' => 'historical-compatibility',
    'catalog' => { 'cardinality' => 1 },
    'identityRows' => [{ 'gameId' => 136, 'gameCode' => 'byzp', 'playVersion' => '1.0.0', 'family' => 'word-card:pao-hu-zi' }],
    'payloadCount' => 1,
    'classpath' => ['WordCard/wordcard-registry.jar'],
    'payload' => [{ 'path' => 'WordCard/wordcard-registry.jar', 'sha256' => Digest::SHA256.file(compatibility_payload).hexdigest, 'kind' => 'compatibility-registry' }],
    'compatibility' => {
      'oldSnapshotEntry' => 'historical-compatibility/old-snapshot',
      'admissionMode' => 'explicit-historical-snapshot-only', 'wordCardAllowed' => true,
      'forbiddenCurrentRoutes' => { 'newRoom' => true, 'lobby' => true, 'reconnect' => true, 'handoff' => true }
    }
  }
  File.write(compatibility_manifest, JSON.pretty_generate(compatibility_doc) + "\n")
  _stdout, stderr, status = invoke(compatibility_manifest, root)
  assert(status.success?, "historical compatibility payload unexpectedly rejected: #{stderr}")

  # Production payloads cannot carry a WordCard jar or registry.
  contaminated = Marshal.load(Marshal.dump(current_doc))
  contaminated['payload'][0]['path'] = 'WordCard/wordcard-registry.jar'
  contaminated['payload'][0]['sha256'] = Digest::SHA256.file(compatibility_payload).hexdigest
  contaminated_path = File.join(root, 'contaminated.json')
  File.write(contaminated_path, JSON.pretty_generate(contaminated) + "\n")
  _stdout, stderr, status = invoke(contaminated_path, root, registry: REGISTRY)
  assert(!status.success? && rejected(stderr, 'PRODUCTION_WORDCARD_PAYLOAD_FORBIDDEN'), 'WordCard production contamination was accepted')

  # A classpath declaration cannot smuggle a compatibility registry into current production.
  classpath_leak = Marshal.load(Marshal.dump(current_doc))
  classpath_leak['classpath'] = ['WordCard/wordcard-registry.jar']
  classpath_path = File.join(root, 'classpath-leak.json')
  File.write(classpath_path, JSON.pretty_generate(classpath_leak) + "\n")
  _stdout, stderr, status = invoke(classpath_path, root, registry: REGISTRY)
  assert(!status.success? && rejected(stderr, 'CLASSPATH_ENTRY_NOT_PAYLOAD'), 'unknown compatibility classpath was accepted')

  # A 31-row production catalog is never a releasable current bundle.
  short = Marshal.load(Marshal.dump(current_doc))
  short['catalog']['cardinality'] = 31
  short['identityRows'] = short['identityRows'][0, 31]
  short_path = File.join(root, 'short.json')
  File.write(short_path, JSON.pretty_generate(short) + "\n")
  _stdout, stderr, status = invoke(short_path, root, registry: REGISTRY)
  assert(!status.success? && rejected(stderr, 'PRODUCTION_CATALOG_CARDINALITY_INVALID'), '31-row production catalog was accepted')

  # Compatibility must provide a concrete old-snapshot entry.
  no_snapshot = Marshal.load(Marshal.dump(compatibility_doc))
  no_snapshot['compatibility'].delete('oldSnapshotEntry')
  no_snapshot_path = File.join(root, 'compatibility-no-snapshot.json')
  File.write(no_snapshot_path, JSON.pretty_generate(no_snapshot) + "\n")
  _stdout, stderr, status = invoke(no_snapshot_path, root)
  assert(!status.success? && rejected(stderr, 'COMPATIBILITY_OLD_SNAPSHOT_ENTRY_REQUIRED'), 'missing old snapshot entry was accepted')

  # Every current route is forbidden for historical Compatibility admission.
  route_leak = Marshal.load(Marshal.dump(compatibility_doc))
  route_leak['compatibility']['forbiddenCurrentRoutes']['reconnect'] = false
  route_leak_path = File.join(root, 'compatibility-route-leak.json')
  File.write(route_leak_path, JSON.pretty_generate(route_leak) + "\n")
  _stdout, stderr, status = invoke(route_leak_path, root)
  assert(!status.success? && rejected(stderr, 'COMPATIBILITY_ROUTE_NOT_FORBIDDEN'), 'Compatibility reconnect route leak was accepted')

  # Payload bytes are part of the admission, not a declaration-only field.
  File.binwrite(current_payload, 'tampered payload')
  _stdout, stderr, status = invoke(current_manifest, root, registry: REGISTRY)
  assert(!status.success? && rejected(stderr, 'PAYLOAD_HASH_MISMATCH'), 'payload byte drift was accepted')

  # A production manifest without the canonical registry cannot be admitted.
  _stdout, stderr, status = invoke(current_manifest, root)
  assert(!status.success? && rejected(stderr, 'INPUT_MISSING'), 'missing production identity registry was accepted')

  # Repository paths are not task-owned payload roots.
  _stdout, stderr, status = invoke(current_manifest, ROOT, registry: REGISTRY)
  assert(!status.success? && rejected(stderr, 'BUNDLE_ROOT_REPOSITORY_FORBIDDEN'), 'repository payload root was accepted')

  puts JSON.generate(
    ok: true,
    status: 'READY_FOR_INDEPENDENT_REVIEW',
    negativeCases: %w[
      production-wordcard-payload
      production-wordcard-classpath
      production-catalog-cardinality
      compatibility-old-snapshot-entry
      compatibility-route-leak
      payload-byte-drift
      missing-production-registry
      repository-payload-root
    ]
  )
end
