#!/usr/bin/env ruby
require 'fileutils'
require 'json'

root = File.expand_path('..', __dir__)
java_files = Dir.glob(File.join(root, 'server/**/*.java')).reject { |path| path.include?('/target/') || path.include?('/build/') }
usage = lambda do |pattern|
  java_files.each_with_object([]) do |path, matches|
    content = File.read(path)
    matches << path.delete_prefix(root + '/') if content.match?(pattern)
  end
end

kernel_usage = usage.call(/(?:import\s+(?:BaseCommon|BaseServer|BaseTask|BaseThread|ConsoleTask)\.|\b(?:BaseServerInit|AsyncTaskManager|SyncTaskManager|BaseMutexManager|ConsoleTaskManager|ThreadManager|CommLog)\b)/)
nettosphere_usage = usage.call(/org\.atmosphere\.nettosphere|\bNettosphere\b/)
entries = [
  {
    coordinate: 'aoo.legacy:kernel:1.0.0',
    disposition: 'REPLACED_BY_SOURCE',
    retainedPurpose: '2.22 bytecode provenance and behavioral comparison only',
    activeReplacement: 'server/AooKernel',
    businessCapabilities: %w[legacy_logging player_locking ordered_task_dispatch server_lifecycle console_compatibility],
    activeUsageFiles: kernel_usage.sort,
    runtimeRequired: false
  },
  {
    coordinate: 'aoo.legacy:nettosphere:3.2.2-qh',
    disposition: 'APPROVED_ARCHIVE_ONLY',
    retainedPurpose: '2.22 customized WebSocket/HTTP implementation provenance only',
    activeReplacement: 'server/Gateway and server/LegacyCommon websocket adapters',
    businessCapabilities: %w[historical_http_websocket_transport],
    activeUsageFiles: nettosphere_usage.sort,
    runtimeRequired: false
  }
]
ledger = { schemaVersion: 1, task: 'THIRD03', entries: entries }
ledger_path = File.join(root, 'docs/generated/third-party-necessity-map.json')
FileUtils.mkdir_p(File.dirname(ledger_path))
File.write(ledger_path, JSON.pretty_generate(ledger) + "\n")

checks = {
  every_artifact_classified: entries.length == 2 && entries.all? { |entry| !entry[:disposition].empty? },
  every_artifact_has_business_mapping: entries.all? { |entry| !entry[:businessCapabilities].empty? },
  kernel_replacement_is_used: !kernel_usage.empty? && Dir.exist?(File.join(root, 'server/AooKernel')),
  nettosphere_absent_from_active_source: nettosphere_usage.empty?,
  no_archive_runtime_requirement: entries.none? { |entry| entry[:runtimeRequired] }
}
result = { task: 'THIRD03', passed: checks.values.all?, checks: checks, ledger: 'docs/generated/third-party-necessity-map.json', kernelUsageCount: kernel_usage.length, nettosphereUsageCount: nettosphere_usage.length }
out = File.join(root, 'work/audit/third03-necessity.json')
FileUtils.mkdir_p(File.dirname(out))
File.write(out, JSON.pretty_generate(result) + "\n")
puts "THIRD03 #{result[:passed] ? 'passed' : 'failed'}: #{entries.length} artifacts mapped to business purpose and disposition"
exit(result[:passed] ? 0 : 1)
