#!/usr/bin/env ruby
# frozen_string_literal: true

require 'json'
require 'digest'

path = ARGV.fetch(0, File.join(__dir__, '../../../release-input/identity/canonical-identity-registry.json'))
doc = JSON.parse(File.binread(path))
required = doc.fetch('requiredFields')
rows = doc.fetch('rows')

def fail_with(message)
  raise "IDENTITY_REGISTRY_REJECTED #{message}"
end

def validate(doc)
  fail_with('manifestKind') unless doc['manifestKind'] == 'canonical-identity-runtime-registry'
  fail_with('schemaVersion') unless doc['schemaVersion'] == 1
  fail_with('sourceContract.noFallbacks') unless doc.dig('sourceContract', 'noFallbacks') == true
  fail_with('sourceContract.noGameIdDerivation') unless doc.dig('sourceContract', 'noGameIdDerivation') == true
  fail_with('projectionBoundary') unless doc.dig('sourceContract', 'projectionBoundary') == %w[serverDescriptor provider lobby gateway client]
  contract_ref = doc.dig('sourceContract', 'currentContractSource')
  fail_with('current-contract-source-reference') unless contract_ref.is_a?(Hash)
  fail_with('current-contract-source-hash') unless contract_ref['sha256'].to_s.match?(/\A[0-9a-f]{64}\z/)
  fail_with('current-contract-source-file') unless File.file?(contract_ref['path'])
  fail_with('current-contract-source-stale') unless Digest::SHA256.file(contract_ref['path']).hexdigest == contract_ref['sha256']
  contract_source = JSON.parse(File.binread(contract_ref['path']))
  fail_with('current-contract-source-kind') unless contract_source['manifestKind'] == 'canonical-identity-current-contract'
  fail_with('current-contract-source-status') unless contract_source['status'] == 'SIGNED_CANONICAL_INPUT'
  fail_with('current-contract-source-issuer') if contract_source['issuer'].to_s.empty?
  fail_with('current-contract-source-version') unless contract_source['contractVersion'] == 'common-room-v1'
  fail_with('current-contract-source-compatibility') if contract_source['contractVersion'] == 'compatibility-v1'
  contract_rows = contract_source.fetch('rows')
  fail_with('current-contract-source-row-count') unless contract_rows.length == 32
  fail_with('current-contract-source-duplicate-gameId') unless contract_rows.map { |row| row.fetch('gameId') }.uniq.length == contract_rows.length
  fail_with('current-contract-source-duplicate-gameCode') unless contract_rows.map { |row| row.fetch('gameCode') }.uniq.length == contract_rows.length
  contract_by_code = contract_rows.to_h { |row| [row.fetch('gameCode'), row] }
  fail_with('rowCount') unless doc['rows'].length == 32
  ids = doc['rows'].map { |row| row.fetch('gameId') }
  fail_with('duplicate-gameId') unless ids.uniq.length == ids.length
  fail_with('compatibility-516') if ids.include?(516)
  codes = doc['rows'].map { |row| row.fetch('gameCode') }
  fail_with('duplicate-gameCode') unless codes.uniq.length == codes.length
  doc['rows'].each do |row|
    contract_row = contract_by_code[row.fetch('gameCode')]
    fail_with("current-contract-row-missing-gameId=#{row['gameId']}") if contract_row.nil?
    %w[gameId sourceHash releaseId indexGeneration].each do |field|
      fail_with("current-contract-row-conflict-gameId=#{row['gameId']} field=#{field}") unless row[field].to_s == contract_row[field].to_s
    end
    fail_with("current-contract-row-version-gameId=#{row['gameId']}") unless row['contractVersion'].to_s == contract_row['contractVersion'].to_s
    required = doc.fetch('requiredFields')
    required.each { |field| fail_with("missing-#{field}-gameId=#{row['gameId']}") unless row.key?(field) }
    fail_with("identity-status-gameId=#{row['gameId']}") unless %w[CONSUMABLE FAIL_CLOSED].include?(row['status'])
    if row['providerKey'].nil?
      fail_with("provider-failure-code-gameId=#{row['gameId']}") unless row.fetch('failureCodes').include?('PROVIDER_KEY_MISSING_FROM_CURRENT_PRODUCTION_CONTRACT')
    elsif row['providerKey'].to_s.empty?
      fail_with("providerKey-gameId=#{row['gameId']}")
    end
    fail_with("clientRuntimeEntry-gameId=#{row['gameId']}") if row['clientRuntimeEntry'].to_s.empty?
    if row['contractVersion'].nil?
      fail_with("contract-failure-code-gameId=#{row['gameId']}") unless row.fetch('failureCodes').include?('CONTRACT_VERSION_MISSING_FROM_FORMAL_RELEASE_SOURCE')
    elsif row['contractVersion'] == 'compatibility-v1'
      fail_with("compatibility-contract-version-gameId=#{row['gameId']}")
    elsif row['contractVersion'] != 'common-room-v1'
      fail_with("current-contract-version-gameId=#{row['gameId']}")
    end
    %w[providerKey clientRuntimeEntry contractVersion].each do |field|
      next if field == 'contractVersion' && row['contractVersion'].nil?
      fail_with("evidence-missing-#{field}-gameId=#{row['gameId']}") unless row.dig('evidence', field).is_a?(Array) && !row.dig('evidence', field).empty?
    end
    row.fetch('evidence').each_value do |items|
      items.each do |item|
        fail_with('evidence-sha') unless item['sha256'].to_s.match?(/\A[0-9a-f]{64}\z/)
        fail_with('evidence-file') unless File.file?(item['path'])
        fail_with("stale-evidence path=#{item['path']}") unless Digest::SHA256.file(item['path']).hexdigest == item['sha256']
      end
    end
  end
  fail_with('consumable-must-be-false-while-contract-missing') if doc['consumable'] && doc['rows'].any? { |row| row['contractVersion'].nil? }
  true
end

validate(doc)
provider_count = rows.count { |row| !row['providerKey'].nil? }
client_count = rows.count { |row| !row['clientRuntimeEntry'].nil? }
fail_with('provider-count') unless provider_count == 32
fail_with('client-count') unless client_count == 32
fail_with('consumable-status') unless doc['consumable'] == true && rows.all? { |row| row['status'] == 'CONSUMABLE' }

negative = {}
negative['duplicate-gameId'] = Marshal.load(Marshal.dump(doc)).tap { |copy| copy['rows'][1]['gameId'] = copy['rows'][0]['gameId'] }
negative['missing-provider'] = Marshal.load(Marshal.dump(doc)).tap { |copy| copy['rows'][0]['providerKey'] = '' }
negative['default-contract'] = Marshal.load(Marshal.dump(doc)).tap { |copy| copy['rows'][0]['contractVersion'] = 'compatibility-v1' }
negative['historical-516'] = Marshal.load(Marshal.dump(doc)).tap { |copy| copy['rows'][0]['gameId'] = 516 }
rejected = negative.each_with_object([]) do |(name, candidate), result|
  begin
    validate(candidate)
  rescue RuntimeError
    result << name
  end
end
fail_with("negative-cases=#{rejected}") unless rejected == negative.keys

puts JSON.generate(ok: true, rows: rows.length, providerKeys: provider_count,
                   clientRuntimeEntries: client_count, consumable: doc['consumable'],
                   negativeCases: rejected)
