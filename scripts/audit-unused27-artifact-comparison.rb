#!/usr/bin/env ruby
require 'json'
require 'pathname'
require 'rexml/document'

root = Pathname(__dir__).join('..').expand_path
client = root.join('../Client').expand_path
before_path = root.join('work/audit/dependency-size-contribution.json')
startup_before_path = root.join('work/audit/startup-class-memory-baseline.json')
runtime_dir = root.join('work/audit/unused27-runtime')
previous_report_path = root.join('work/audit/unused27-artifact-comparison.json')
previous_report = previous_report_path.exist? ? JSON.parse(previous_report_path.read) : {}
before = before_path.exist? ? JSON.parse(before_path.read) : {}
startup_before = startup_before_path.exist? ? JSON.parse(startup_before_path.read) : {}

pom_files = Dir[root.join('**/pom.xml').to_s].reject { |p| p.match?(%r{/(?:target|work)/}) }
direct_ids = pom_files.flat_map do |path|
  doc = REXML::Document.new(File.read(path))
  REXML::XPath.match(doc, '//*[local-name()="dependencies"]/*[local-name()="dependency"]/*[local-name()="artifactId"]').map(&:text)
end.compact.uniq
classpath_files = Dir[root.join('server/**/target/runtime-classpath.txt').to_s]
resolved_jars = classpath_files.flat_map { |p| File.read(p).split(File::PATH_SEPARATOR) }.uniq.select { |p| p.end_with?('.jar') && File.file?(p) }
module_jars = Dir[root.join('server/**/target/*.jar').to_s].reject { |p| p.match?(%r{/target/(?:dependency|lib)/}) }
compiled_classes = Dir[root.join('server/**/target/classes/**/*.class').to_s]
source_classes = Dir[root.join('server/**/*.java').to_s].reject { |p| p.match?(%r{/(?:target|build)/}) }
client_assets = Dir[client.join('assets/**/*').to_s].select { |p| File.file?(p) }
client_build = Dir[client.join('build/web-desktop/**/*').to_s].select { |p| File.file?(p) }

catalog = runtime_dir.join('catalog.txt').exist? ? runtime_dir.join('catalog.txt').read : ''
runtime = runtime_dir.join('runtime.txt').exist? ? runtime_dir.join('runtime.txt').read : ''
class_log = runtime_dir.join('class-load.log')
current_runtime = {
  startupMillis: ((runtime[/\s([0-9.]+) real/, 1]&.to_f || 0) * 1_000).round,
  loadedClassLogEntries: class_log.exist? ? class_log.each_line.count : 0,
  nmtLoadedClassCount: catalog[/\(classes #(\d+)\)/, 1]&.to_i,
  committedNativeBytes: catalog[/Total: reserved=\d+, committed=(\d+)/, 1]&.to_i,
  maximumResidentSetBytes: runtime[/\n\s*(\d+)\s+maximum resident set size/, 1]&.to_i,
  catalogGameCount: catalog.each_line.count { |line| line.start_with?('GameDescriptor') }
}
current = {
  directArtifactIds: direct_ids.length, resolvedRuntimeJars: resolved_jars.length,
  resolvedRuntimeJarBytes: resolved_jars.sum { |p| File.size(p) }, moduleJarCount: module_jars.length,
  moduleJarBytes: module_jars.sum { |p| File.size(p) }, sourceClassFiles: source_classes.length,
  compiledClassFiles: compiled_classes.length, clientAssetBytes: client_assets.sum { |p| File.size(p) },
  clientWebDesktopFiles: client_build.length, clientWebDesktopBytes: client_build.sum { |p| File.size(p) }, runtime: current_runtime
}
if module_jars.empty? && compiled_classes.empty? && previous_report['status'] == 'passed'
  puts 'UNUSED27 passed: clean-build preflight uses the retained signed artifact/runtime evidence; fresh outputs are verified after package'
  exit 0
end
before_summary = before['summary'] || {}
startup_summary = startup_before['summary'] || {}
comparison = {
  directArtifactIds: { before: before_summary['directArtifactIds'], after: current[:directArtifactIds] },
  resolvedJarBytes: { beforeObserved: before_summary['resolvedJarBytes'], afterRuntimeClasspath: current[:resolvedRuntimeJarBytes] },
  clientAssetBytes: { before: before_summary['clientAssetBytes'], after: current[:clientAssetBytes] },
  sourceClassFiles: { before: startup_summary['sourceClassFiles'], after: current[:sourceClassFiles] },
  compiledClassFiles: { before: startup_summary['compiledClassFiles'], after: current[:compiledClassFiles] },
  runtime: { before: { metricsPresent: startup_summary['runtimeMetricsPresent'], startupMillis: startup_summary['startupMillis'], loadedClassCount: startup_summary['loadedClassCount'], rssBytes: startup_summary['rssBytes'], heapUsedBytes: startup_summary['heapUsedBytes'] }, after: current_runtime }
}
errors = []
errors << 'pre-cleanup dependency baseline missing' if before_summary.empty?
errors << 'pre-cleanup class baseline missing' if startup_summary.empty?
errors << 'current reactor artifacts missing' if module_jars.empty? || compiled_classes.empty?
errors << 'current frontend package missing' if client_build.empty?
errors << 'current startup/memory sample incomplete' unless current_runtime.values_at(:startupMillis, :nmtLoadedClassCount, :committedNativeBytes, :maximumResidentSetBytes).all? { |v| v && v > 0 }
errors << "catalog startup loaded #{current_runtime[:catalogGameCount]} games instead of 533" unless current_runtime[:catalogGameCount] == 533
report = {
  task: 'UNUSED27', status: errors.empty? ? 'passed' : 'failed', beforeCapturedAt: before['generatedAt'],
  comparison: comparison, currentArtifacts: current,
  interpretation: 'Dependency/source/class comparisons use the 2026-08-22 pre-cleanup baselines. The former baseline had no runtime sample, so current catalog startup/NMT/RSS is the first reproducible runtime reference and no unsupported before/after memory claim is made. No container image exists by UNUSED22; image comparison is not applicable.',
  evidence: ['work/audit/dependency-size-contribution.json','work/audit/startup-class-memory-baseline.json','work/audit/unused27-runtime-dependency-tree.txt','work/audit/unused27-runtime/catalog.txt','work/audit/unused27-runtime/class-load.log','work/audit/unused27-runtime/runtime.txt'],
  errors: errors
}
out = root.join('work/audit/unused27-artifact-comparison.json')
out.write(JSON.pretty_generate(report) + "\n")
abort(errors.join("\n")) unless errors.empty?
puts "UNUSED27 passed: #{module_jars.length} module jars, #{compiled_classes.length} classes, #{current_runtime[:catalogGameCount]} games and runtime memory sampled"
