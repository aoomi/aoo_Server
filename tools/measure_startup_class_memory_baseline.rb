#!/usr/bin/env ruby
# frozen_string_literal: true

require 'json'
require 'time'
require 'fileutils'

root = File.expand_path('..', __dir__)
output = File.join(root, 'work/audit/startup-class-memory-baseline.json')
source_classes = Dir.glob(File.join(root, 'server/**/*.{java,kt}')).reject { |path| path.include?('/target/') || path.include?('/build/') }
compiled_classes = Dir.glob(File.join(root, 'server/**/target/classes/**/*.class'))
providers = source_classes.select { |path| File.basename(path).end_with?('GameProvider.java') }
modules = Dir.glob(File.join(root, 'server/*/pom.xml')).map { |path| File.basename(File.dirname(path)) }
runtime_file = ENV['AOO_RUNTIME_METRICS_JSON']
runtime = runtime_file && File.file?(runtime_file) ? JSON.parse(File.read(runtime_file, encoding: 'UTF-8')) : nil
runtime_complete = runtime.is_a?(Hash) && %w[startupMillis loadedClassCount rssBytes heapUsedBytes].all? { |key| runtime[key].is_a?(Numeric) }
summary = {
  modules: modules.size,
  providerSources: providers.size,
  sourceClassFiles: source_classes.size,
  compiledClassFiles: compiled_classes.size,
  runtimeMetricsPresent: runtime_complete,
  startupMillis: runtime && runtime['startupMillis'],
  loadedClassCount: runtime && runtime['loadedClassCount'],
  rssBytes: runtime && runtime['rssBytes'],
  heapUsedBytes: runtime && runtime['heapUsedBytes'],
  passed: runtime_complete
}
report = {schemaVersion: 1, generatedAt: Time.now.utc.iso8601,
          invariant: 'Each production framework/plugin composition has reproducible startup time, loaded-class count, RSS and heap baselines.',
          summary: summary, modules: modules.sort, providers: providers.map { |path| path.delete_prefix(root + '/') }.sort,
          runtimeMetricsSource: runtime_file}
FileUtils.mkdir_p(File.dirname(output))
File.write(output, JSON.pretty_generate(report) + "\n", mode: 'w:UTF-8')
puts JSON.generate(summary)
exit(summary[:passed] ? 0 : 2) if ENV['AOO_ENFORCE_RUNTIME_BASELINE_GATE'] == '1'
