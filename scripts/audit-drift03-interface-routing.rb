#!/usr/bin/env ruby
require 'json'
require 'fileutils'

root = File.expand_path('..', __dir__)
protocol = JSON.parse(File.read(File.join(root, 'protocol/aoo-protocol-v2.json')))
generated = File.read(File.join(root, 'server/GameSPI/src/main/java/com/aoo/bcg/gamespi/protocol/GeneratedProtocolIds.java'))
ownership = File.read(File.join(root, 'server/GameSPI/src/main/java/com/aoo/bcg/gamespi/api/ApiOwnershipCatalog.java'))
reference = File.read(File.join(root, 'docs/generated/protocol-reference.md'))
rows = protocol.fetch('messages').map do |message|
  id = message.fetch('msgId')
  namespace = id.split('.').first
  transport = message.fetch('transport')
  endpoint = transport == 'HTTPS' ? "/api/v2/#{id.tr('.', '/')}" : id
  owner_pattern = transport == 'HTTPS' ? "/api/v2/#{namespace}/*" : "#{namespace}.*"
  {
    msgId: id, endpoint: endpoint, transport: transport, direction: message.fetch('direction'),
    fields: message.fetch('request').fetch('properties').keys.sort,
    errors: message.fetch('errors'), auth: message.fetch('auth'), ownerPattern: owner_pattern,
    generatedRegistry: generated.include?(%Q{"#{id}"}),
    documented: reference.include?("## `#{id}`"),
    owned: ownership.include?(%Q{"#{owner_pattern}"})
  }
end
checks = {
  generatedRegistryMatches: rows.all? { |row| row[:generatedRegistry] },
  documentationMatches: rows.all? { |row| row[:documented] },
  ownershipMatches: rows.all? { |row| row[:owned] },
  completeContractMetadata: rows.all? { |row| row[:endpoint] && row[:fields] && row[:errors] && row[:auth] }
}
report = { schemaVersion: 1, protocolVersion: protocol.fetch('protocolVersion'), count: rows.length, rows: rows, checks: checks }
out = File.join(root, 'docs/generated/drift03-interface-routing-reconciliation.json')
FileUtils.mkdir_p(File.dirname(out))
File.write(out, JSON.pretty_generate(report) + "\n")
abort "DRIFT03 failed: #{checks}" unless checks.values.all?
puts "DRIFT03 PASS: #{rows.length} interfaces reconciled"
