#!/usr/bin/env ruby
require 'json'
require 'rexml/document'
require 'open3'
require 'fileutils'

root = File.expand_path('..', __dir__)
pom = REXML::Document.new(File.read(File.join(root, 'pom.xml')))
properties = REXML::XPath.match(pom, '/project/properties/*').to_h { |node| [node.name, node.text.to_s.strip] }
java_home = File.expand_path('../.toolchains/jdk-26.0.2.1.jdk/Contents/Home', root)
env = { 'JAVA_HOME' => java_home, 'PATH' => "#{java_home}/bin:#{ENV['PATH']}" }
maven_out, maven_err, status = Open3.capture3(env, './mvnw', '-version', chdir: root)
abort "wrapper version probe failed: #{maven_err}" unless status.success?
java_line = maven_out.lines.find { |line| line.start_with?('Java version:') }.to_s.strip
wrapper = File.read(File.join(root, '.mvn/wrapper/maven-wrapper.properties'))
report = {
  schemaVersion: 1,
  compileTarget: { javaRelease: properties.fetch('maven.compiler.release').to_i, compatibilityPolicy: 'Java 25 LTS bytecode/API' },
  validationRuntime: { java: java_line, canonicalExternalToolchain: '../.toolchains/jdk-26.0.2.1.jdk/Contents/Home' },
  buildTool: { maven: maven_out.lines.first.to_s.strip, wrapperProperties: '.mvn/wrapper/maven-wrapper.properties' },
  clientGeneratorTool: { node: File.read(File.join(root, '.node-version')).strip, scope: 'protocol/client generation only' },
  managedVersions: properties.select { |key, _| key.end_with?('.version') }.sort.to_h,
  checks: {
    compileRuntimeCompatible: java_line.match?(/Java version: 26\./) && properties.fetch('maven.compiler.release') == '25',
    canonicalRuntimeScript: File.read(File.join(root, 'tools/runtime-java26.sh')).include?('../.toolchains/jdk-26.0.2.1.jdk/Contents/Home'),
    canonicalWrapper: wrapper.include?('apache-maven/3.9.16/') && wrapper.include?('wrapperVersion=3.3.4'),
    noMvnwFork: !File.exist?(File.join(root, 'mvnw26'))
  }
}
out = File.join(root, 'docs/generated/drift02-version-build-reconciliation.json')
FileUtils.mkdir_p(File.dirname(out))
File.write(out, JSON.pretty_generate(report) + "\n")
abort "DRIFT02 failed: #{report[:checks]}" unless report[:checks].values.all?
puts 'DRIFT02 PASS: compile, runtime, wrapper and managed versions reconciled'
