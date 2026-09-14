#!/usr/bin/env ruby
# frozen_string_literal: true

require 'digest'
require 'fileutils'
require 'json'
require 'net/http'
require 'rexml/document'
require 'time'

root = File.expand_path('..', __dir__)
repository = File.expand_path('~/.m2/repository')
tree_files = Dir.glob(File.join(root, '{work,server/*/work}/audit/cur04-dependency-tree.txt'))

def child_text(element, name)
  REXML::XPath.first(element, "./*[local-name()='#{name}']")&.text&.strip
end

def pom_licenses(path, repository, seen = [])
  return [] unless File.file?(path) && !seen.include?(path)
  seen << path
  document = REXML::Document.new(File.read(path))
  project = document.root
  licenses = REXML::XPath.match(project, "./*[local-name()='licenses']/*[local-name()='license']").map do |license|
    {name: child_text(license, 'name'), url: child_text(license, 'url')}
  end.reject { |license| license[:name].to_s.empty? && license[:url].to_s.empty? }
  return licenses unless licenses.empty?
  parent = REXML::XPath.first(project, "./*[local-name()='parent']")
  return [] unless parent
  group = child_text(parent, 'groupId')
  artifact = child_text(parent, 'artifactId')
  version = child_text(parent, 'version')
  return [] unless [group, artifact, version].all?
  parent_path = File.join(repository, group.tr('.', '/'), artifact, version, "#{artifact}-#{version}.pom")
  pom_licenses(parent_path, repository, seen)
rescue REXML::ParseException
  []
end

resolved = Hash.new { |hash, key| hash[key] = [] }
tree_files.each do |path|
  consumer = path.include?('/server/') ? path.split('/server/').last.split('/').first : 'parent'
  File.foreach(path) do |line|
    match = line.match(/(?:\+-|\\-)\s+([^\s]+):(compile|runtime)(?:\s|$)/)
    next unless match
    fields = match[1].split(':')
    next if fields.length < 4 || fields[0] == 'com.aoo.bcg'
    group, artifact = fields[0], fields[1]
    version = fields[-1]
    resolved[[group, artifact, version]] << consumer
  end
end
components = resolved.map do |(group, artifact, version), consumers|
  directory = File.join(repository, group.tr('.', '/'), artifact, version)
  candidates = Dir.glob(File.join(directory, "#{artifact}-#{version}*.jar")).reject { |path| path.end_with?('-sources.jar', '-javadoc.jar') }
  artifact_path = candidates.find { |path| File.basename(path) == "#{artifact}-#{version}.jar" } || candidates.first
  abort "No local Maven artifact for #{group}:#{artifact}:#{version}" unless artifact_path
  pom = File.join(directory, "#{artifact}-#{version}.pom")
  {coordinate: "#{group}:#{artifact}:#{version}", package: {ecosystem: 'Maven', name: "#{group}:#{artifact}"},
   version: version, sha256: Digest::SHA256.file(artifact_path).hexdigest, bytes: File.size(artifact_path),
   licenses: pom_licenses(pom, repository), consumers: consumers.uniq.sort}
end.sort_by { |component| component[:coordinate] }

snapshot = File.join(root, 'docs/generated/active-dependency-osv.json')
network = {attempted: false, succeeded: false, error: nil}
vulnerability_results = nil
begin
  network[:attempted] = true
  uri = URI('https://api.osv.dev/v1/querybatch')
  request = Net::HTTP::Post.new(uri, 'Content-Type' => 'application/json')
  request.body = JSON.generate(queries: components.map { |component| {package: component[:package], version: component[:version]} })
  response = Net::HTTP.start(uri.host, uri.port, use_ssl: true, open_timeout: 15, read_timeout: 60) { |http| http.request(request) }
  raise "HTTP #{response.code}" unless response.is_a?(Net::HTTPSuccess)
  vulnerability_results = JSON.parse(response.body).fetch('results')
  network[:succeeded] = true
  FileUtils.mkdir_p(File.dirname(snapshot))
  File.write(snapshot, JSON.pretty_generate({schemaVersion: 1, fetchedAt: Time.now.utc.iso8601,
    source: 'https://api.osv.dev/v1/querybatch', components: components.map { |component| component[:coordinate] }, results: vulnerability_results}) + "\n")
rescue StandardError => error
  network[:error] = "#{error.class}: #{error.message}"
  if File.file?(snapshot)
    cached = JSON.parse(File.read(snapshot))
    expected = components.map { |component| component[:coordinate] }
    vulnerability_results = cached['results'] if cached['components'] == expected
  end
end

abort 'No current-coordinate OSV evidence available online or from snapshot' unless vulnerability_results
findings = components.zip(vulnerability_results).map do |component, result|
  vulns = result.fetch('vulns', [])
  {coordinate: component[:coordinate], vulnerabilities: vulns.map { |vulnerability| vulnerability['id'] }} unless vulns.empty?
end.compact
missing_licenses = components.select { |component| component[:licenses].empty? }.map { |component| component[:coordinate] }
report = {schemaVersion: 1, generatedAt: Time.now.utc.iso8601, componentCount: components.length,
  network: network, osvSnapshot: 'docs/generated/active-dependency-osv.json', vulnerabilityFindings: findings,
  missingLicenseDeclarations: missing_licenses, components: components,
  passed: findings.empty? && missing_licenses.empty?}
output = File.join(root, 'work/audit/active-dependency-evidence.json')
FileUtils.mkdir_p(File.dirname(output))
File.write(output, JSON.pretty_generate(report) + "\n")
puts JSON.generate({components: components.length, vulnerabilities: findings.length,
  missingLicenses: missing_licenses.length, network: network, passed: report[:passed]})
exit(report[:passed] ? 0 : 1)
