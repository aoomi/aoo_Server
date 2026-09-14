#!/usr/bin/env ruby
# frozen_string_literal: true

require 'csv'
require 'digest'
require 'json'
require 'time'
require 'set'

ROOT = File.expand_path('..', __dir__)
MAP_PATH = File.join(ROOT, 'work/audit/r04-authoritative-four-layer-map.json')
REGISTRY_PATH = File.join(ROOT, 'work/audit/r04-family-runtime-registry.json')
V07_PATH = File.join(ROOT, 'work/audit/v07-all-games.json')
NATIVE_PATH = File.join(ROOT, 'work/audit/r04-native-family-archetype-map.tsv')
OUT_PATH = File.join(ROOT, 'work/audit/r04-family-runtime-verification.json')
REGION_RESOURCES = {
  'MAHJONG' => 'server/Mahjong/src/main/resources/mahjong-family-region-config.tsv',
  'POKER' => 'server/Poker/src/main/resources/poker-family-region-config.tsv',
  'WORD_CARD' => 'server/WordCard/src/main/resources/word-card-family-region-config.tsv',
  'LONG_CARD' => 'server/LongCard/src/main/resources/long-card-family-region-config.tsv'
}.freeze
REGION_HEADERS = %w[gameId code family archetype region ruleComponents lifecycleComponents sourceStatus sourceEvidence allowedCreateFields configSchemaHash].freeze

STAGES = %w[create join ready deal-or-start turn legal-operation settlement replay reconnect].freeze

map = JSON.parse(File.read(MAP_PATH))
registry = JSON.parse(File.read(REGISTRY_PATH))
v07 = JSON.parse(File.read(V07_PATH))
native = CSV.read(NATIVE_PATH, headers: true, col_sep: "\t")
items = map.fetch('items')
failures = []

binding_errors = items.flat_map do |item|
  errors = []
  %w[baseCardClass gameplayFamily runtimeArchetype regionRuleConfig].each do |field|
    errors << "#{item['gameId']}:missing-#{field}" if item[field].to_s.empty?
  end
  errors << "#{item['gameId']}:catalog-id-mismatch" unless item['gameId'] == item['catalogId']
  errors
end
failures << "catalog binding errors=#{binding_errors.length}" unless binding_errors.empty?
failures << "catalog binding count=#{items.length}, expected=528" unless items.length == 528

missing_source = items.select { |i| i['sourceStatus'] != 'SOURCE_SIGNATURE_AVAILABLE' }.map { |i| "#{i['gameId']}:#{i['code']}" }.sort
expected_missing = %w[238:caooszmj 250:dlaoomj]
failures << "missing-source set=#{missing_source.inspect}, expected=#{expected_missing.inspect}" unless missing_source == expected_missing

families = items.map { |i| i['gameplayFamily'] }.reject { |f| f == 'UNCLASSIFIED_MISSING_SOURCE' }.uniq.sort
archetypes = items.map { |i| i['runtimeArchetype'] }.reject { |a| a == 'RUNTIME_ARCHETYPE:UNKNOWN' }.uniq.sort
failures << "family count=#{families.length}, expected=15" unless families.length == 15
failures << "archetype count=#{archetypes.length}, expected=32" unless archetypes.length == 32

registry_rows = registry.fetch('families')
registry_ids = registry_rows.map { |r| r['id'] }.sort
failures << 'family registry ids do not match classified families' unless registry_ids == families
family_results = registry_rows.map do |entry|
  missing = []
  missing << 'serverRegistration' if entry['serverRegistration'].to_s.empty?
  missing << 'clientRegistration' if entry['clientRegistration'].to_s.empty?
  evidence = entry.fetch('nineStageEvidence', [])
  missing_stages = STAGES - evidence
  missing << "nineStageEvidence:#{missing_stages.join(',')}" unless missing_stages.empty?
  failures << "family #{entry['id']} incomplete: #{missing.join(';')}" unless missing.empty?
  {'family' => entry['id'], 'passed' => missing.empty?, 'missing' => missing}
end
registered_family_ids = family_results.select { |r| r['passed'] }.map { |r| r['family'] }.to_set

config_schema_errors = items.flat_map do |item|
  errors = []
  errors << "#{item['gameId']}:invalid-config-key" unless item['regionRuleConfig'].match?(/\ACONFIG:[a-z0-9_-]+@(?:national|[a-z0-9_-]+(?:\/[a-z0-9_-]+)?)\z/i)
  errors << "#{item['gameId']}:missing-rule-components" if item['sourceStatus'] == 'SOURCE_SIGNATURE_AVAILABLE' && item['ruleComponents'].to_s.empty?
  errors << "#{item['gameId']}:missing-lifecycle-components" if item['sourceStatus'] == 'SOURCE_SIGNATURE_AVAILABLE' && item['lifecycleComponents'].to_s.empty?
  errors
end
failures << "region config schema errors=#{config_schema_errors.length}" unless config_schema_errors.empty?

item_by_id = items.each_with_object({}) { |item, h| h[item['gameId'].to_s] = item }
resource_rows = []
region_resource_errors = []
REGION_RESOURCES.each do |category, relative|
  path = File.join(ROOT, relative)
  unless File.file?(path)
    region_resource_errors << "missing-resource:#{relative}"
    next
  end
  table = CSV.read(path, headers: true, col_sep: "\t")
  region_resource_errors << "#{relative}:headers" unless table.headers == REGION_HEADERS
  expected_count = items.count { |item| item['category'] == category }
  region_resource_errors << "#{relative}:rows=#{table.size},expected=#{expected_count}" unless table.size == expected_count
  table.each do |row|
    resource_rows << row
    item = item_by_id[row['gameId']]
    if item.nil?
      region_resource_errors << "#{relative}:unknown-gameId=#{row['gameId']}"
      next
    end
    %w[code].each { |field| region_resource_errors << "#{row['gameId']}:#{field}-mismatch" unless row[field] == item[field] }
    region_resource_errors << "#{row['gameId']}:family-mismatch" unless row['family'] == item['gameplayFamily']
    region_resource_errors << "#{row['gameId']}:archetype-mismatch" unless row['archetype'] == item['runtimeArchetype']
    region_resource_errors << "#{row['gameId']}:source-status-mismatch" unless row['sourceStatus'] == item['sourceStatus']
    fields = row['allowedCreateFields'].to_s.split(',').reject(&:empty?)
    region_resource_errors << "#{row['gameId']}:allowed-fields-not-sorted-unique" unless fields == fields.uniq.sort
    if row['sourceStatus'] == 'SOURCE_SIGNATURE_AVAILABLE'
      region_resource_errors << "#{row['gameId']}:missing-source-evidence" if row['sourceEvidence'].to_s.empty?
    elsif !%w[238 250].include?(row['gameId'])
      region_resource_errors << "#{row['gameId']}:unexpected-blocked-source"
    end
    canonical = REGION_HEADERS[0, 8].map { |h| row[h] } + [row['allowedCreateFields'].to_s]
    expected_hash = Digest::SHA256.hexdigest(canonical.join("\t"))
    region_resource_errors << "#{row['gameId']}:schema-hash" unless row['configSchemaHash'] == expected_hash
  end
end
region_resource_errors << "resource-union=#{resource_rows.size},expected=528" unless resource_rows.size == 528
resource_ids = resource_rows.map { |row| row['gameId'] }
region_resource_errors << 'resource-gameIds-not-unique' unless resource_ids.uniq.size == 528
failures << "family region resource errors=#{region_resource_errors.length}" unless region_resource_errors.empty?

duplicate_server_groups = native.group_by { |r| [r['gameplayFamily'], r['runtimeArchetype']] }.map do |key, rows|
  next if registered_family_ids.include?(key[0])
  next if rows.length < 2
  {'family' => key[0], 'archetype' => key[1], 'codes' => rows.map { |r| r['code'] }.sort,
   'providerClasses' => rows.map { |r| r['providerClass'] }.sort}
end.compact
failures << "code-specific duplicate provider groups=#{duplicate_server_groups.length}" unless duplicate_server_groups.empty?

catalog_by_code = items.each_with_object({}) { |i, h| h[i['code']] = i }
client_rows = v07.fetch('clientAdapters', []).map do |path|
  code = path[%r{/([a-z0-9]+)/network/}i, 1]&.downcase || File.basename(path)[/\A([A-Za-z0-9]+)NetworkAdapter/, 1]&.downcase
  item = catalog_by_code[code]
  {'code' => code, 'path' => path, 'family' => item&.[]('gameplayFamily'), 'archetype' => item&.[]('runtimeArchetype')}
end
duplicate_client_groups = client_rows.select { |r| r['family'] }.group_by { |r| [r['family'], r['archetype']] }.map do |key, rows|
  next if registry_rows.any? { |entry| entry['id'] == key[0] && !entry['clientRegistration'].to_s.empty? }
  next if rows.length < 2
  {'family' => key[0], 'archetype' => key[1], 'codes' => rows.map { |r| r['code'] }.sort, 'paths' => rows.map { |r| r['path'] }.sort}
end.compact
failures << "code-specific duplicate client-main groups=#{duplicate_client_groups.length}" unless duplicate_client_groups.empty?

result = {
  'schemaVersion' => 1,
  'generatedAt' => Time.now.utc.iso8601,
  'passed' => failures.empty?,
  'summary' => {
    'catalogBindings' => items.length,
    'bindingErrors' => binding_errors.length,
    'missingSourceItems' => missing_source.length,
    'families' => families.length,
    'registeredCompleteFamilies' => family_results.count { |r| r['passed'] },
    'runtimeArchetypes' => archetypes.length,
    'regionConfigSchemaErrors' => config_schema_errors.length,
    'familyRegionResourceRows' => resource_rows.length,
    'familyRegionResourceErrors' => region_resource_errors.length,
    'codeSpecificDuplicateProviderGroups' => duplicate_server_groups.length,
    'codeSpecificDuplicateClientMainGroups' => duplicate_client_groups.length
  },
  'missingSource' => missing_source,
  'familyResults' => family_results,
  'bindingErrors' => binding_errors,
  'regionConfigSchemaErrors' => config_schema_errors,
  'familyRegionResourceErrors' => region_resource_errors,
  'duplicateServerGroups' => duplicate_server_groups,
  'duplicateClientGroups' => duplicate_client_groups,
  'failures' => failures
}
File.write(OUT_PATH, JSON.pretty_generate(result) + "\n")
puts JSON.generate(result['summary'].merge('passed' => result['passed'], 'failures' => failures))
exit(result['passed'] ? 0 : 1)
