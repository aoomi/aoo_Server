#!/usr/bin/env ruby
# frozen_string_literal: true

require 'json'
require 'open3'
require 'time'

root = File.expand_path('..', __dir__)
baseline = JSON.parse(File.read(File.join(root, 'config/java-runtime-compatibility.json')))
architecture = baseline.fetch('architectureBaseline')
shell = <<~SH
  source tools/runtime-java26.sh
  printf 'JAVA_HOME=%s\n' "$JAVA_HOME"
  java -XshowSettings:properties -version 2>&1
  ./mvnw -version
SH
stdout, stderr, status = Open3.capture3('bash', '-lc', shell, chdir: root)
combined = stdout + stderr
pom = File.read(File.join(root, 'pom.xml'))
runtime = File.read(File.join(root, 'tools/runtime-java26.sh'))
errors = []
errors << 'runtime launcher failed' unless status.success?
errors << 'runtime is not Java 26' unless combined.match?(/java\.version = 26\./)
errors << 'Maven does not run on Java 26' unless combined.match?(/Java version: 26\./)
errors << 'Maven wrapper version mismatch' unless combined.include?("Apache Maven #{architecture['maven']}")
errors << 'compiler release mismatch' unless pom.include?("<maven.compiler.release>#{architecture['bytecodeRelease']}</maven.compiler.release>")
errors << 'Java enforcer range mismatch' unless pom.include?('<requireJavaVersion><version>[26,27)</version></requireJavaVersion>')
errors << 'Maven enforcer range mismatch' unless pom.include?('<requireMavenVersion><version>[3.9.16,4.0.0)</version></requireMavenVersion>')
errors << 'repository-local legacy toolchain is reachable' if runtime.include?('work/toolchains') || runtime.include?('work/jdk8')
baseline.fetch('managedRuntimeLibraries').each do |library|
  errors << "managed dependency absent: #{library}" unless pom.include?(library)
end
report = {
  schemaVersion: 1,
  generatedAt: Time.now.utc.iso8601,
  task: 'TOOL10',
  passed: errors.empty?,
  baseline: architecture,
  observed: combined.lines.grep(/JAVA_HOME=|java\.version =|Java version:|Apache Maven/).map(&:strip),
  errors: errors
}
File.write(File.join(root, 'docs/generated/tool10-java-runtime-compatibility.json'), JSON.pretty_generate(report) + "\n")
puts "java-runtime-compatibility: #{errors.empty? ? 'passed' : 'failed'}"
exit(errors.empty? ? 0 : 1)
