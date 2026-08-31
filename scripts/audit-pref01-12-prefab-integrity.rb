#!/usr/bin/env ruby
require 'json'
require 'find'
require 'digest'
require 'fileutils'

root = File.expand_path('..', __dir__)
client = File.expand_path('../Client', root)
assets = File.join(client, 'assets')
out = File.join(root, 'docs/generated')
FileUtils.mkdir_p(out)
prefabs = Dir.glob(File.join(assets, '**/*.{prefab,scene}')).sort
meta = JSON.parse(File.read(File.join(out, 'creal08-meta-integrity.json')))
serialized = JSON.parse(File.read(File.join(out, 'creal09-prefab-scene.json')))

def emit(out, id, data)
  File.write(File.join(out, "#{id}.json"), JSON.pretty_generate({schemaVersion: 1}.merge(data)) + "\n")
end

parse_errors = []
component_types = Hash.new { |h, k| h[k] = [] }
override_records = []
active_records = []
name_lookup_risks = []
prefabs.each do |path|
  data = JSON.parse(File.read(path))
  rel = path.delete_prefix(client + '/')
  data.each_with_index do |entry, index|
    next unless entry.is_a?(Hash)
    type = entry['__type__']
    component_types[type] << {path: rel, index: index} if type
    override_records << {path: rel, index: index, type: type} if type.to_s.match?(/Override|TargetInfo/)
    active_records << {path: rel, index: index, active: entry['_active']} if entry.key?('_active')
  end
rescue JSON::ParserError => e
  parse_errors << {path: path.delete_prefix(client + '/'), error: e.message}
end

script_uuid_types = component_types.keys.compact.select { |type| type.match?(/\A[0-9a-f]{8}/i) }
known_uuids = Dir.glob(File.join(assets, '**/*.meta')).map { |path| JSON.parse(File.read(path))['uuid'] rescue nil }.compact.to_h { |uuid| [uuid, true] }
unresolved_script_types = script_uuid_types.reject { |type| known_uuids[type] }

Dir.glob(File.join(assets, '**/*.ts')).each do |path|
  File.foreach(path).with_index(1) do |line, no|
    if line.match?(/(?:find|getChildByName)\s*\(/)
      name_lookup_risks << {path: path.delete_prefix(client + '/'), line: no}
    end
  end
end

emit(out, 'pref01-json-version', {files: prefabs.map { |p| p.delete_prefix(client + '/') }, count: prefabs.length, parseErrors: parse_errors, creatorVersion: '3.8.8', creatorLoadEvidence: false, passed: false})
emit(out, 'pref02-missing-script', {componentTypeCount: component_types.length, scriptUuidTypeCount: script_uuid_types.length, unresolvedScriptTypes: unresolved_script_types, staticPassed: unresolved_script_types.empty?})
emit(out, 'pref03-missing-asset', {referenceAudit: meta['referenceAudit'], missingDirectReferences: meta.dig('referenceAudit', 'missingDirectReferences'), passed: meta.dig('referenceAudit', 'missingDirectReferences') == 0})
emit(out, 'pref04-event-targets', {interactiveComponents: serialized.dig('ui', 'interactiveComponents'), serializedBindings: serialized.dig('ui', 'serializedBindings'), dynamicClickSites: serialized.dig('ui', 'clickSites'), completeTargetMethodProof: false, passed: false})
emit(out, 'pref05-duplicate-uuid', {metaFiles: meta['metaFiles'], uuidCount: meta['uuidCount'], duplicateMetaUuids: meta.dig('referenceAudit', 'duplicateMetaUuids'), passed: meta.dig('referenceAudit', 'duplicateMetaUuids') == 0})
emit(out, 'pref06-orphan-meta', {orphanMeta: meta['orphanMeta'], missingMeta: meta['missingMeta'], passed: meta['orphanMeta'].empty? && meta['missingMeta'].empty?})
emit(out, 'pref07-nested-overrides', {overrideRecordCount: override_records.length, overrideRecords: override_records, creatorPropertyPathProof: false, passed: false})
emit(out, 'pref08-default-state', {serializedActiveRecords: active_records.length, requiredDomains: %w[chat voice errorLayer operationButtons masks], fullAssertions: false, passed: false})
emit(out, 'pref09-node-lookup', {riskCount: name_lookup_risks.length, risks: name_lookup_risks, stableReferenceGate: false, passed: false})
emit(out, 'pref10-batch-edit-conservation', {currentHashesRecorded: prefabs.length, beforeHashesRecorded: 0, uuidReferenceTypePrecisionConserved: false, passed: false})
emit(out, 'pref11-editor-open-save', {prefabSceneCount: prefabs.length, creatorVersion: '3.8.8', opened: 0, saved: 0, zeroConsoleErrors: false, manual: true, passed: false})
emit(out, 'pref12-test-mapping', {interactiveComponents: serialized.dig('ui', 'interactiveComponents'), mappedControls: 0, complete: false, passed: false})
puts "PREF01-PREF12 audited: #{prefabs.length} Prefab/Scene files, #{component_types.length} component types"
