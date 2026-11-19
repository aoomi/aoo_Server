#!/usr/bin/env ruby
# frozen_string_literal: true

require 'digest'
require 'json'
require 'open3'
require 'rbconfig'
require 'tmpdir'

ROOT = File.expand_path('../../../..', __dir__)
CHECKER = File.join(ROOT, 'server/engineering/release/validate-canonical-registry-runtime-binding.rb')
MANIFEST = File.join(ROOT, 'server/release-input/identity/runtime-metadata-canonical-manifest-r50-current-20261011.json')
REGISTRY = File.join(ROOT, 'server/release-input/identity/canonical-identity-registry.json')
CONTRACT = File.join(ROOT, 'server/tools/canonical-identity-contract-source.json')

def sha(path)
  Digest::SHA256.file(path).hexdigest
end

def run_checker(manifest, registry, contract, expected_sha, output)
  args = [RbConfig.ruby, CHECKER, '--manifest', manifest, '--manifest-sha256', expected_sha,
          '--registry', registry, '--contract-source', contract, '--output', output]
  stdout, stderr, status = Open3.capture3(*args)
  [stdout, stderr, status.exitstatus]
end

def assert(condition, message)
  raise message unless condition
end

Dir.mktmpdir('aoo-registry-binding-test-') do |dir|
  manifest_doc = JSON.parse(File.read(MANIFEST))
  registry_doc = JSON.parse(File.read(REGISTRY))
  manifest_by_id = manifest_doc.fetch('rows').to_h { |row| [row.fetch('gameId'), row] }
  fixture_registry = Marshal.load(Marshal.dump(registry_doc))
  fixture_registry['releaseEdition'] = manifest_doc.fetch('releaseEdition')
  fixture_registry['rows'].each do |row|
    source = manifest_by_id.fetch(row.fetch('gameId'))
    %w[gameCode family playVersion regionCode contractVersion sourceHash releaseId indexGeneration].each { |field| row[field] = source.fetch(field) }
  end
  fixture_manifest = File.join(dir, 'manifest.json')
  fixture_registry_path = File.join(dir, 'registry.json')
  fixture_contract_path = File.join(dir, 'contract.json')
  output = File.join(dir, 'projection.json')
  File.write(fixture_manifest, JSON.pretty_generate(manifest_doc) + "\n")
  fixture_registry['sourceDigests']['releaseManifest'] = { 'path' => fixture_manifest, 'sha256' => sha(fixture_manifest) }
  File.write(fixture_registry_path, JSON.pretty_generate(fixture_registry) + "\n")
  contract_rows = fixture_registry['rows'].map do |row|
    row.slice('gameId', 'gameCode', 'providerKey', 'clientRuntimeEntry', 'contractVersion', 'sourceHash', 'releaseId', 'indexGeneration')
  end
  contract_payload = {
    'contractVersion' => manifest_doc['sourceContract']['contractVersion'],
    'manifestSha256' => sha(fixture_manifest),
    'registrySha256' => sha(fixture_registry_path),
    'releaseEdition' => manifest_doc['releaseEdition'],
    'rows' => contract_rows
  }
  fixture_contract = contract_payload.merge(
    'manifestKind' => 'canonical-identity-current-contract',
    'status' => 'SIGNED_CANONICAL_INPUT',
    'sourceHash' => Digest::SHA256.hexdigest(JSON.generate(contract_payload))
  )
  File.write(fixture_contract_path, JSON.pretty_generate(fixture_contract) + "\n")
  fixture_manifest_sha = sha(fixture_manifest)

  stdout, stderr, exit_code = run_checker(fixture_manifest, fixture_registry_path, fixture_contract_path, fixture_manifest_sha, output)
  assert(exit_code == 0, "positive failed: #{stderr} #{stdout}")
  positive = JSON.parse(File.read(output))
  assert(positive['consumable'] == true && positive['rowCount'] == 32, 'positive row count/consumable mismatch')

  _, mismatch_stderr, mismatch_exit = run_checker(MANIFEST, REGISTRY, CONTRACT, sha(MANIFEST), File.join(dir, 'mismatch.json'))
  assert(mismatch_exit == 1 && mismatch_stderr.include?('REGISTRY_MISMATCH|field=sourceHash|gameId=5'), 'stale registry must fail on first source-hash row drift')

  _, missing_stderr, missing_exit = run_checker(fixture_manifest, fixture_registry_path, CONTRACT, fixture_manifest_sha, File.join(dir, 'missing.json'))
  assert(missing_exit == 1 && missing_stderr.include?('CONTRACT_SOURCE_MANIFEST_BINDING_MISSING'), 'legacy contract must fail closed for missing explicit binding')

  _, sha_stderr, sha_exit = run_checker(fixture_manifest, fixture_registry_path, fixture_contract_path, ('0' * 64), File.join(dir, 'sha.json'))
  assert(sha_exit == 1 && sha_stderr.include?('MANIFEST_SHA_MISMATCH'), 'manifest SHA drift must fail')

  drift_contract = Marshal.load(Marshal.dump(fixture_contract))
  drift_contract['rows'][0]['sourceHash'] = 'f' * 64
  drift_payload = drift_contract.slice('contractVersion', 'manifestSha256', 'registrySha256', 'releaseEdition', 'rows')
  drift_contract['sourceHash'] = Digest::SHA256.hexdigest(JSON.generate(drift_payload))
  File.write(File.join(dir, 'drift-contract.json'), JSON.pretty_generate(drift_contract) + "\n")
  _, drift_stderr, drift_exit = run_checker(fixture_manifest, fixture_registry_path, File.join(dir, 'drift-contract.json'), fixture_manifest_sha, File.join(dir, 'drift.json'))
  assert(drift_exit == 1 && drift_stderr.include?('CONTRACT_MISMATCH|field=sourceHash|gameId=5'), "contract row drift must fail: #{drift_stderr}")

  duplicate_registry = Marshal.load(Marshal.dump(fixture_registry))
  duplicate_registry['rows'][1]['gameId'] = duplicate_registry['rows'][0]['gameId']
  duplicate_path = File.join(dir, 'duplicate-registry.json')
  File.write(duplicate_path, JSON.pretty_generate(duplicate_registry) + "\n")
  _, duplicate_stderr, duplicate_exit = run_checker(fixture_manifest, duplicate_path, fixture_contract_path, fixture_manifest_sha, File.join(dir, 'duplicate.json'))
  assert(duplicate_exit == 1 && duplicate_stderr.include?('REGISTRY_DUPLICATE_GAME_ID'), 'duplicate registry id must fail')

  puts JSON.generate({ 'ok' => true, 'positiveRows' => positive['rowCount'], 'negativeCases' => %w[registry-drift legacy-contract-missing-binding manifest-sha-drift contract-row-drift duplicate-registry-id] })
end
