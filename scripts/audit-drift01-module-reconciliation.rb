#!/usr/bin/env ruby
require 'json'
require 'rexml/document'
require 'fileutils'
require 'set'
root = File.expand_path('..', __dir__)
pom = REXML::Document.new(File.read(File.join(root, 'pom.xml')))
active = REXML::XPath.match(pom, '/project/modules/module').map { |node| node.text.strip }
all_poms = Dir.glob(File.join(root, 'server/**/pom.xml')).sort
coordinates = lambda do |path|
  doc = REXML::Document.new(File.read(path))
  artifact = REXML::XPath.first(doc, '/project/artifactId')&.text
  dependencies = REXML::XPath.match(doc, '/project/dependencies/dependency/artifactId').map { |node| node.text.strip }
  [artifact, dependencies]
end
active_rows = active.map do |path|
  absolute = File.join(root, path, 'pom.xml')
  abort "active module POM missing: #{path}" unless File.file?(absolute)
  artifact, dependencies = coordinates.call(absolute)
  {path: path, artifactId: artifact, dependencies: dependencies.sort}
end
inactive_rows = (all_poms - active.map { |path| File.join(root, path, 'pom.xml') }).map do |path|
  artifact, dependencies = coordinates.call(path)
  {path: File.dirname(path).delete_prefix(root + '/'), artifactId: artifact,
   dependencies: dependencies.sort, classification: 'legacy-quarantine-not-reactor'}
end
inactive_artifacts = inactive_rows.map { |row| row[:artifactId] }.compact.to_set
violations = active_rows.flat_map do |row|
  row[:dependencies].select { |artifact| inactive_artifacts.include?(artifact) }
      .map { |artifact| {module: row[:path], inactiveDependency: artifact} }
end
ant = Dir.glob(File.join(root, 'server/**/build.xml')).sort.map do |path|
  {path: path.delete_prefix(root + '/'), classification: 'legacy-non-production-build-entry'}
end
report = {schemaVersion: 1, activeReactorModuleCount: active_rows.length,
  activeReactorModules: active_rows, inactiveMavenModules: inactive_rows, antBuildEntries: ant,
  dependencyDirectionViolations: violations,
  policy: 'Only modules listed by the root Maven reactor are production modules. Other Maven and Ant entries are inventory-only legacy quarantine until their dedicated removal tasks close.'}
out = File.join(root, 'docs/generated/drift01-module-reconciliation.json')
FileUtils.mkdir_p(File.dirname(out)); File.write(out, JSON.pretty_generate(report) + "\n")
abort "DRIFT01 dependency violations: #{violations}" unless violations.empty?
puts "DRIFT01 PASS: #{active_rows.length} active, #{inactive_rows.length} quarantined Maven, #{ant.length} Ant entries"
