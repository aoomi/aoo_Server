#!/usr/bin/env ruby
require 'fileutils'
require 'json'
require 'open3'
require 'tmpdir'

root = File.expand_path('../../..', __dir__)
project_root = File.expand_path('..', root)
publisher = File.join(root, 'engineering/release/tool-inputs/room-rules-publisher.rb')
identity_path = File.join(root, 'release-input/identity/room-rule-play-identities.json')
identities = JSON.parse(File.read(identity_path, encoding: 'UTF-8')).fetch('plays').select do |entry|
  (2003..2006).cover?(entry['gameId'])
end
abort('missing four v29 PDK identity fixtures') unless identities.map { |entry| entry['gameId'] }.sort == [2003, 2004, 2005, 2006]

def run_publisher(publisher, command, workbook, keys, output, identity_path)
  Open3.capture3({
    'AOO_ROOM_RULE_WORKBOOK'=>workbook,
    'AOO_ROOM_RULE_REGISTRY'=>keys,
    'AOO_ROOM_RULE_GENERATED'=>output,
    'AOO_ROOM_RULE_IDENTITY_REGISTRY'=>identity_path
  }, 'ruby', publisher, command)
end

def write_payload(path, payload)
  File.open(path, 'wb', 0o600) { |file| file.write(JSON.pretty_generate(payload) + "\n") }
end

Dir.mktmpdir('aoo-room-rule-key-owner-migration-test') do |directory|
  identities.each do |identity|
    workbook = File.expand_path(identity.fetch('workbookPath'), project_root)
    basename = File.basename(workbook, '.xlsx')
    key_source = File.join(root, "release-input/room-rules/#{basename}.keys.json")
    canonical = identity.fetch('gameCode')
    legacy_alias = identity.fetch('legacyKeyOwnerAliases').fetch(0)
    legacy_field_aliases = identity.fetch('legacyFieldKeyAliases')
    abort("#{canonical} must define one exact field-key alias") unless legacy_field_aliases.length == 1
    keys = File.join(directory, "#{canonical}.keys.json")
    output = File.join(directory, "#{canonical}.generated.json")
    original = JSON.parse(File.read(key_source, encoding: 'UTF-8'))
    stable_data = Marshal.load(Marshal.dump(original.reject { |key, _| key == 'gameCode' }))
    stable_data.fetch('fields', []).each do |field|
      field['key'] = legacy_field_aliases.fetch(field['key'], field['key'])
    end

    # The only allowed legacy owner is transformed to the canonical identity;
    # all stable key values, sequence counters, fields and options are preserved.
    write_payload(keys, original.merge('gameCode'=>legacy_alias))
    _stdout, stderr, status = run_publisher(publisher, 'migrate-key-owner', workbook, keys, output, identity_path)
    abort("#{canonical} allowed alias migration failed: #{stderr}") unless status.success?
    migrated_bytes = File.read(keys, encoding: 'UTF-8')
    migrated = JSON.parse(migrated_bytes)
    abort("#{canonical} owner was not canonicalized") unless migrated.fetch('gameCode') == canonical
    abort("#{canonical} stable key data changed during migration") unless migrated.reject { |key, _| key == 'gameCode' } == stable_data

    # Repeating the explicit command against a canonical owner is a byte-stable no-op.
    _stdout, stderr, status = run_publisher(publisher, 'migrate-key-owner', workbook, keys, output, identity_path)
    abort("#{canonical} repeat should be a no-op: #{stderr}") unless status.success?
    abort("#{canonical} repeat rewrote the registry") unless File.read(keys, encoding: 'UTF-8') == migrated_bytes

    # A different old code is not implicitly accepted and cannot mutate the file.
    unknown = original.merge('gameCode'=>'UNREGISTERED201')
    write_payload(keys, unknown)
    unknown_bytes = File.read(keys, encoding: 'UTF-8')
    _stdout, _stderr, status = run_publisher(publisher, 'migrate-key-owner', workbook, keys, output, identity_path)
    abort("#{canonical} unknown owner unexpectedly migrated") if status.success?
    abort("#{canonical} unknown owner rejection changed the registry") unless File.read(keys, encoding: 'UTF-8') == unknown_bytes

    # Ordinary preview remains strict: aliases are not a normal-path fallback.
    write_payload(keys, original.merge('gameCode'=>legacy_alias))
    alias_bytes = File.read(keys, encoding: 'UTF-8')
    _stdout, stderr, status = run_publisher(publisher, 'preview', workbook, keys, output, identity_path)
    abort("#{canonical} normal preview silently accepted a legacy owner") if status.success?
    abort("#{canonical} normal preview failed for an unexpected reason") unless stderr.include?('稳定键注册表所属玩法不匹配')
    abort("#{canonical} normal preview changed the owner registry") unless File.read(keys, encoding: 'UTF-8') == alias_bytes
  end
end

puts 'PASS: four explicit allowlisted owner/field-key migrations, stable-data preservation, idempotency, unknown-owner rejection, and strict normal preview'
