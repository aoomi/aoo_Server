#!/usr/bin/env ruby
# frozen_string_literal: true

require 'json'
require 'open3'
require 'tmpdir'

root = File.expand_path('../../..', __dir__)
validator = File.join(root, 'engineering/release/tool-inputs/validate-runtime-metadata-manifest.rb')
manifest = File.join(root, 'tools/canonical-runtime-metadata-manifest.json')
catalog = File.join(root, 'server/GameSPI/src/main/resources/current-game-identities.tsv')
issuer = '/root/lobby_runtime_from_staged_source'
edition = 'r15-cd2999-20261012'
validator_args = [manifest, catalog, issuer, edition]
stdout, stderr, status = Open3.capture3('ruby', validator, *validator_args)
abort("canonical manifest validator failed: #{stderr.empty? ? stdout : stderr}") unless status.success?
result = JSON.parse(stdout)
abort('validator did not report 32 rows') unless result.fetch('rows') == 32
abort('validator did not preserve current contract field') unless result.fetch('contractVersion') == 'common-room-v1'

Dir.mktmpdir('aoo-runtime-metadata-manifest-test') do |directory|
  payload = JSON.parse(File.read(manifest, encoding: 'UTF-8'))
  payload.fetch('rows')[0]['releaseId'] = payload.fetch('rows')[1].fetch('releaseId')
  candidate = File.join(directory, 'duplicate-release-id.json')
  File.write(candidate, JSON.generate(payload) + "\n")
  _bad_stdout, bad_stderr, bad_status = Open3.capture3('ruby', validator, candidate, catalog, issuer, edition)
  abort('duplicate releaseId was not rejected') if bad_status.success?
  abort('duplicate releaseId rejection was not machine-readable') unless bad_stderr.include?('RUNTIME_METADATA_MANIFEST_REJECTED|manifest-sha-unauthorized')
end

Dir.mktmpdir('aoo-runtime-metadata-manifest-issuer-test') do |directory|
  payload = JSON.parse(File.read(manifest, encoding: 'UTF-8'))
  payload.delete('releaseIssuer')
  candidate = File.join(directory, 'missing-release-issuer.json')
  File.write(candidate, JSON.generate(payload) + "\n")
  _bad_stdout, bad_stderr, bad_status = Open3.capture3('ruby', validator, candidate, catalog, issuer, edition)
  abort('missing release issuer was not rejected') if bad_status.success?
  abort('missing release issuer rejection was not machine-readable') unless bad_stderr.include?('RUNTIME_METADATA_MANIFEST_REJECTED|manifest-sha-unauthorized')
end

Dir.mktmpdir('aoo-runtime-metadata-catalog-test') do |directory|
  current = File.read(catalog, encoding: 'UTF-8')
  altered = current.sub("5\tquanguo2998\tcn298-v1.0.0\tpoker:betting", "5\tquanguo2998\tcn298-v1.0.0\tword-card:unexpected")
  altered_catalog = File.join(directory, 'altered-current-game-identities.tsv')
  File.write(altered_catalog, altered)
  _bad_stdout, bad_stderr, bad_status = Open3.capture3('ruby', validator, manifest, altered_catalog, issuer, edition)
  abort('current catalog compatibility drift was not rejected') if bad_status.success?
  abort('current catalog rejection was not machine-readable') unless bad_stderr.include?('RUNTIME_METADATA_MANIFEST_REJECTED|current-catalog-compatibility=5')
end

Dir.mktmpdir('aoo-runtime-metadata-contract-test') do |directory|
  payload = JSON.parse(File.read(manifest, encoding: 'UTF-8'))
  payload.fetch('rows').first['contractVersion'] = 'compatibility-v1'
  candidate = File.join(directory, 'contract-version-drift.json')
  File.write(candidate, JSON.generate(payload) + "\n")
  _bad_stdout, bad_stderr, bad_status = Open3.capture3('ruby', validator, candidate, catalog, issuer, edition)
  abort('contractVersion drift was not rejected') if bad_status.success?
  expected_id = payload.fetch('rows').first.fetch('gameId')
  abort('contractVersion drift rejection was not machine-readable') unless bad_stderr.include?('RUNTIME_METADATA_MANIFEST_REJECTED|manifest-sha-unauthorized')
end

legacy_manifest = File.join(root, 'tools/runtime-metadata-canonical-manifest.json')
_legacy_stdout, legacy_stderr, legacy_status = Open3.capture3(
  'ruby', validator, legacy_manifest, catalog, '/root/map_runtime_release_values', 'r9-v4'
)
abort('legacy r9-v4 path was not fail-closed against current identity source') if legacy_status.success?
abort('legacy r9-v4 rejection was not machine-readable') unless legacy_stderr.include?('RUNTIME_METADATA_MANIFEST_REJECTED|')

_bad_stdout, bad_contract_stderr, bad_contract_status = Open3.capture3(
  'ruby', validator, manifest, catalog, '/root/unauthorized', edition
)
abort('unauthorized issuer contract was accepted') if bad_contract_status.success?
abort('unauthorized issuer rejection was not machine-readable') unless bad_contract_stderr.include?('release-contract-unauthorized')

puts JSON.generate(ok: true, validRows: 32, negativeCases: ['duplicate-release-id=REJECTED', 'missing-release-issuer=REJECTED', 'current-catalog-drift=REJECTED', 'contract-version-drift=REJECTED'])
