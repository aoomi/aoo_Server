#!/usr/bin/env ruby
require 'json'
require 'fileutils'

root = File.expand_path('..', __dir__)
client = File.expand_path('../Client', root)
assets = File.join(client, 'assets')
scene_files = Dir.glob(File.join(assets, '**/*.{scene,prefab}')).sort
ui = JSON.parse(File.read(File.join(root, 'work/audit/ui-backend-closure-audit.json')))
uuid = JSON.parse(File.read(File.join(root, 'work/audit/cocos-uuid-reference-audit.json')))
store = JSON.parse(File.read(File.join(root, 'work/audit/store-ui-subscription-graph.json')))
report = {
  schemaVersion: 1,
  clientRoot: client,
  serializedAssets: {
    total: scene_files.length,
    scenes: scene_files.count { |path| path.end_with?('.scene') },
    prefabs: scene_files.count { |path| path.end_with?('.prefab') }
  },
  uuidDependencyGraph: uuid.fetch('summary'),
  uiBackendClosure: ui.fetch('summary'),
  storeSubscriptionSummary: store.fetch('summary', {}),
  limitations: uuid.fetch('limitations', []) + ['runtime-computed node lookup and handlers require instrumented browser/editor execution'],
  checks: {
    serializedAssetsPresent: !scene_files.empty?,
    noMissingUuidReferences: uuid.dig('summary', 'missingDirectReferences').to_i.zero?,
    noDuplicateUuidReferences: uuid.dig('summary', 'duplicateMetaUuids').to_i.zero?,
    allInteractiveBindingsClosed: ui.dig('summary', 'candidateFilesWithoutBackendClosure').to_i.zero? && ui.dig('summary', 'fakeSites').to_i.zero?
  }
}
report[:verdict] = report[:checks].values.all? ? 'passed' : 'blocked-dynamic-ui-closure'
out = File.join(root, 'docs/generated/drift06-client-prefab-reconciliation.json')
FileUtils.mkdir_p(File.dirname(out))
File.write(out, JSON.pretty_generate(report) + "\n")
puts "DRIFT06 AUDITED: #{report[:verdict]}"
