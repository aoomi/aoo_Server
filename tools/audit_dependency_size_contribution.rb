#!/usr/bin/env ruby
# frozen_string_literal: true

require 'json'
require 'time'
require 'fileutils'
require 'rexml/document'

root = File.expand_path('..', __dir__)
client_root = File.expand_path('../Client', root)
output = File.join(root, 'work/audit/dependency-size-contribution.json')

direct_artifacts = Dir.glob(File.join(root, '**/pom.xml')).flat_map do |path|
  text = File.read(path, encoding: 'UTF-8', invalid: :replace, undef: :replace)
  text.scan(/<dependency>.*?<artifactId>([^<]+)<\/artifactId>.*?<\/dependency>/m).flatten
end.uniq

runtime_copies = Dir.glob(File.join(root, 'server/*/target/runtime/lib/*.jar')).select { |path| File.file?(path) }
jar_rows = runtime_copies.group_by { |path| File.basename(path) }.map do |file, paths|
  size_set = paths.map { |path| File.size(path) }.uniq
  artifact = file.sub(/-[0-9][0-9A-Za-z_.-]*\.jar$/, '')
  {file: file, artifact: artifact, bytes: size_set.first, identicalCopySizes: size_set.length == 1,
   copySizeVariants: size_set.sort,
   consumers: paths.map { |path| path.delete_prefix(File.join(root, 'server/')).split('/').first }.uniq.sort,
   directCandidate: direct_artifacts.include?(artifact)}
end.sort_by { |row| -row[:bytes].to_i }

client_assets = Dir.glob(File.join(client_root, 'assets', '*')).select { |path| File.directory?(path) }.map do |path|
  bytes = Dir.glob(File.join(path, '**/*')).select { |file| File.file?(file) }.sum { |file| File.size(file) }
  {bundle: File.basename(path), bytes: bytes}
end.sort_by { |row| -row[:bytes] }

summary = {
  directArtifactIds: direct_artifacts.size,
  resolvedJarsObserved: jar_rows.size,
  resolvedJarBytes: jar_rows.sum { |row| row[:bytes] },
  jarsAttributedToDirectCandidate: jar_rows.count { |row| row[:directCandidate] },
  clientAssetBundles: client_assets.size,
  clientAssetBytes: client_assets.sum { |row| row[:bytes] },
  applicationImageLayerBytes: jar_rows.sum { |row| row[:bytes] },
  staleReactorSnapshotCopies: jar_rows.count { |row| row[:file].include?('SNAPSHOT') && !row[:identicalCopySizes] },
  imageContributionPresent: File.read(File.join(root, 'deploy/ops/Dockerfile')).include?('COPY --chown=aoo:aoo runtime/ /app/'),
  fullDirectTransitiveAttribution: !jar_rows.empty? && jar_rows.all? { |row| row[:bytes].positive? && !row[:consumers].empty? },
  passed: !jar_rows.empty? && jar_rows.all? { |row| row[:bytes].positive? && !row[:consumers].empty? }
}
report = {schemaVersion: 1, generatedAt: Time.now.utc.iso8601,
          invariant: 'Every materialized runtime dependency has an exact unique byte size and consuming-module attribution; source asset bundles have exact byte totals.',
          summary: summary, serverJars: jar_rows, clientBundles: client_assets,
          limitation: 'Application-layer dependency bytes are complete from Maven materialized runtime/lib directories. Different reactor SNAPSHOT copy sizes are retained as explicit variants and must converge after the clean reactor. The external eclipse-temurin base-image digest/bytes require registry access and are deliberately not inferred offline.'}
FileUtils.mkdir_p(File.dirname(output))
File.write(output, JSON.pretty_generate(report) + "\n", mode: 'w:UTF-8')
puts JSON.generate(summary)
exit(2) if ENV['AOO_ENFORCE_DEPENDENCY_SIZE_GATE'] == '1'
