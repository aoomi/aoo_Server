#!/usr/bin/env ruby
require 'digest'
require 'fileutils'
require 'json'
require 'open3'
require 'set'

root = File.expand_path('..', __dir__)
custom = File.join(root, 'reference/legacy-2.22/third-party-maven-repository/com/aoo/legacy/nettosphere/3.2.2-qh/nettosphere-3.2.2-qh.jar')
upstream = File.join(root, 'reference/upstream/org.atmosphere/nettosphere/3.2.2/nettosphere-3.2.2.jar')
upstream_sources = File.join(root, 'reference/upstream/org.atmosphere/nettosphere/3.2.2/nettosphere-3.2.2-sources.jar')
kernel = File.join(root, 'reference/legacy-2.22/third-party-maven-repository/com/aoo/legacy/kernel/1.0.0/kernel-1.0.0.jar')

zip_entries = lambda do |jar|
  list, _error, status = Open3.capture3('unzip', '-Z1', jar)
  raise "cannot list #{jar}" unless status.success?
  list.b.lines.map { |line| line.strip.force_encoding(Encoding::BINARY) }.select { |name| name.end_with?('.class'.b) }.to_h do |name|
    bytes, _stderr, entry_status = Open3.capture3('unzip', '-p', jar, name)
    raise "cannot read #{name}" unless entry_status.success?
    [name.dup.force_encoding(Encoding::UTF_8), Digest::SHA256.hexdigest(bytes)]
  end
end
custom_classes = zip_entries.call(custom)
upstream_classes = zip_entries.call(upstream)
custom_names = custom_classes.keys.to_set
upstream_names = upstream_classes.keys.to_set
added = (custom_names - upstream_names).to_a.sort
removed = (upstream_names - custom_names).to_a.sort
changed = (custom_names & upstream_names).select { |name| custom_classes[name] != upstream_classes[name] }.sort

kernel_sources = Dir.glob(File.join(root, 'server/AooKernel/src/main/java/**/*.java')).map { |path| path.delete_prefix(root + '/') }.sort
dossier = {
  schemaVersion: 1,
  task: 'THIRD05',
  artifacts: [
    {
      coordinate: 'aoo.legacy:kernel:1.0.0',
      original: 'private 2.22 kernel binary; no public upstream coordinate',
      originalSha256: Digest::SHA256.file(kernel).hexdigest,
      modificationRecord: 'binary behavior recovered into owned source module; archive retained as immutable baseline',
      reason: 'preserve BaseServer/BaseTask/BaseThread contracts while removing private binary runtime dependency',
      replacement: 'server/AooKernel',
      replacementSources: kernel_sources,
      regressions: ['server/AooKernel/src/test/java/BaseThread/CosMutexTimeoutTest.java']
    },
    {
      coordinate: 'aoo.legacy:nettosphere:3.2.2-qh',
      upstream: 'org.atmosphere:nettosphere:3.2.2',
      upstreamRepository: 'https://repo.maven.apache.org/maven2/org/atmosphere/nettosphere/3.2.2/',
      upstreamSha256: Digest::SHA256.file(upstream).hexdigest,
      upstreamSourcesSha256: Digest::SHA256.file(upstream_sources).hexdigest,
      customizedSha256: Digest::SHA256.file(custom).hexdigest,
      binaryPatch: { addedClasses: added, removedClasses: removed, changedClasses: changed },
      reason: 'historical private transport customization; no longer runtime-reachable',
      replacement: 'server/Gateway',
      regressions: Dir.glob(File.join(root, 'server/Gateway/src/test/java/**/*.java')).map { |path| path.delete_prefix(root + '/') }.sort
    }
  ]
}
dossier_path = File.join(root, 'docs/generated/third-party-modification-dossier.json')
FileUtils.mkdir_p(File.dirname(dossier_path))
File.write(dossier_path, JSON.pretty_generate(dossier) + "\n")
checks = {
  immutable_originals_present: [custom, kernel].all? { |path| File.file?(path) },
  nettosphere_upstream_binary_present: File.file?(upstream),
  nettosphere_upstream_sources_present: File.file?(upstream_sources),
  nettosphere_difference_recorded: !(added.empty? && removed.empty? && changed.empty?),
  kernel_owned_source_present: !kernel_sources.empty?,
  regression_ownership_present: dossier[:artifacts].all? { |artifact| !artifact[:regressions].empty? },
  archives_release_excluded: File.read(File.join(root, '.releaseignore')).lines.map(&:strip).include?('reference/')
}
result = { task: 'THIRD05', passed: checks.values.all?, checks: checks, dossier: 'docs/generated/third-party-modification-dossier.json', nettosphereDiff: { added: added.length, removed: removed.length, changed: changed.length }, kernelSourceCount: kernel_sources.length }
out = File.join(root, 'work/audit/third05-modification-dossier.json')
FileUtils.mkdir_p(File.dirname(out))
File.write(out, JSON.pretty_generate(result) + "\n")
puts "THIRD05 #{result[:passed] ? 'passed' : 'failed'}: Nettosphere diff #{added.length}/#{removed.length}/#{changed.length}, kernel #{kernel_sources.length} owned sources"
exit(result[:passed] ? 0 : 1)
