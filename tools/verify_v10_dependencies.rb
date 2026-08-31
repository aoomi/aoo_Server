#!/usr/bin/env ruby
# frozen_string_literal: true

require 'json'
require 'pathname'
require 'rexml/document'

root = Pathname(__dir__).join('..').expand_path
workspace = root.parent
errors = []

server_pom = root.join('pom.xml').read
client = JSON.parse(workspace.join('Client/package.json').read)
admin = JSON.parse(workspace.join('Admin/package.json').read)
wrapper = root.join('.mvn/wrapper/maven-wrapper.properties').read

expected = {
  java: '25', maven: '3.9.16', node: '24.19.0', pnpm: '11.19.0', cocos: '3.8.8'
}

errors << 'Server compiler release must be Java 25' unless server_pom.include?('<maven.compiler.release>25</maven.compiler.release>')
errors << 'Server Enforcer must require Java [25,26)' unless server_pom.include?('<requireJavaVersion><version>[25,26)</version></requireJavaVersion>')
errors << 'Maven wrapper must pin 3.9.16' unless wrapper.include?('/apache-maven/3.9.16/')
errors << 'Maven wrapper checksum is required' unless wrapper.match?(/^distributionSha512Sum=[0-9a-f]{128}$/)

{
  'Server/.node-version' => root.join('.node-version').read.strip,
  'Client/.node-version' => workspace.join('Client/.node-version').read.strip,
  'Client engines.node' => client.dig('engines', 'node'),
  'Admin/.node-version' => workspace.join('Admin/.node-version').read.strip,
  'Admin/.nvmrc' => workspace.join('Admin/.nvmrc').read.strip,
  'Admin engines.node' => admin.dig('engines', 'node')
}.each { |label, value| errors << "#{label} must equal #{expected[:node]}" unless value == expected[:node] }

errors << 'Client must pin pnpm 11.19.0' unless client['packageManager'] == "pnpm@#{expected[:pnpm]}" && client.dig('engines', 'pnpm') == expected[:pnpm]
errors << 'Admin must pin pnpm 11.19.0' unless admin['packageManager'] == "pnpm@#{expected[:pnpm]}" && admin.dig('engines', 'pnpm') == expected[:pnpm]
errors << 'Client must pin Cocos Creator 3.8.8' unless client.dig('creator', 'version') == expected[:cocos] && client.dig('engines', 'cocos') == expected[:cocos]

%w[Client Admin].each do |project|
  directory = workspace.join(project)
  errors << "#{project} pnpm-lock.yaml is missing" unless directory.join('pnpm-lock.yaml').file?
  %w[package-lock.json yarn.lock].each do |other|
    errors << "#{project} contains competing lock file #{other}" if directory.join(other).exist?
  end
end

active_poms = Dir[root.join('{pom.xml,server/**/pom.xml}').to_s].reject do |path|
  path.include?('/LegacyAccountServer/') || path.include?('/LegacyGameHall/') || path.include?('/target/')
end
active_poms.each do |path|
  document = REXML::Document.new(File.read(path))
  coordinates = REXML::XPath.match(document, '/*[local-name()="project"]/*[local-name()="dependencies"]/*[local-name()="dependency"]').map do |dependency|
    group = REXML::XPath.first(dependency, './*[local-name()="groupId"]')&.text.to_s
    artifact = REXML::XPath.first(dependency, './*[local-name()="artifactId"]')&.text.to_s
    "#{group}:#{artifact}"
  end
  counts = Hash.new(0)
  coordinates.each { |coordinate| counts[coordinate] += 1 }
  duplicates = counts.select { |_coordinate, count| count > 1 }.keys
  errors << "#{Pathname(path).relative_path_from(root)} duplicates #{duplicates.join(', ')}" unless duplicates.empty?
  text = File.read(path)
  errors << "#{Pathname(path).relative_path_from(root)} reaches a legacy Maven coordinate" if text.match?(/com\.aoo\.legacy|<groupId>account_server<\/groupId>|<scope>system<\/scope>|<systemPath>/)
end

gradle = Dir[workspace.join('{Server,Client,Admin}/**/{build.gradle,build.gradle.kts,settings.gradle,settings.gradle.kts}').to_s].reject do |path|
  path.include?('/node_modules/') || path.include?('/reference/') || path.include?('/development/migration/')
end
errors << "unexpected active Gradle build: #{gradle.join(', ')}" unless gradle.empty?

reference = root.join('reference/legacy-2.22')
errors << '2.22 reference quarantine is missing' unless reference.directory? && reference.join('README.md').read.include?('生产运行时不加载')
production_reference_hits = active_poms.select { |path| File.read(path).include?('reference/legacy-2.22') }
errors << "production POM reaches 2.22 quarantine: #{production_reference_hits.join(', ')}" unless production_reference_hits.empty?

report = {
  task: 'V10-dependencies', passed: errors.empty?, expected: expected,
  activePomCount: active_poms.length, gradleBuilds: gradle,
  lockFiles: %w[Client/pnpm-lock.yaml Admin/pnpm-lock.yaml],
  legacyBoundary: 'Server/reference/legacy-2.22 (read-only; absent from production POMs)',
  errors: errors
}
out = root.join('work/audit/v10-dependencies.json')
out.dirname.mkpath
out.write(JSON.pretty_generate(report) + "\n")
puts JSON.generate(report)
abort("V10 dependency gate failed:\n- #{errors.join("\n- ")}") unless errors.empty?
