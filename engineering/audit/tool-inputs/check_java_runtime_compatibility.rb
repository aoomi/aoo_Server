#!/usr/bin/env ruby
# frozen_string_literal: true

require 'json'
require 'open3'
require 'time'

root = File.expand_path('../../..', __dir__)
baseline = JSON.parse(File.read(File.join(root, 'config/java-runtime-compatibility.json')))
architecture = baseline.fetch('architectureBaseline')
pom = File.read(File.join(root, 'pom.xml'))
errors = []
compiler_release = pom[/<maven\.compiler\.release>\s*([^<]+)\s*<\/maven\.compiler\.release>/, 1]&.strip
java_range = pom[/<requireJavaVersion>\s*<version>\s*([^<]+)\s*<\/version>/, 1]&.strip
maven_range = pom[/<requireMavenVersion>\s*<version>\s*([^<]+)\s*<\/version>/, 1]&.strip
errors << 'POM compiler release is missing' if compiler_release.nil? || compiler_release.empty?
errors << 'POM Java enforcer range is missing' if java_range.nil? || java_range.empty?
errors << 'POM Maven enforcer range is missing' if maven_range.nil? || maven_range.empty?

expected_feature = compiler_release.to_i
java_env = {}
configured_java_home = ENV['AOO_JAVA_HOME'] || ENV['JAVA_HOME']
java_env['JAVA_HOME'] = configured_java_home if configured_java_home && !configured_java_home.empty?
java_command = configured_java_home && !configured_java_home.empty? ? File.join(configured_java_home, 'bin', 'java') : 'java'
maven_command = File.join(root, 'mvnw')

def capture(command, env, *arguments, chdir: nil)
  Open3.capture3(env, command, *arguments, chdir: chdir)
rescue SystemCallError => failure
  ['', failure.message, nil]
end

java_stdout, java_stderr, java_status = capture(java_command, java_env, '-XshowSettings:properties', '-version', chdir: root)
maven_stdout, maven_stderr, maven_status = capture(maven_command, java_env, '-version', chdir: root)
combined = java_stdout + java_stderr + maven_stdout + maven_stderr
errors << 'Java runtime command failed' unless java_status&.success?
errors << 'Maven wrapper command failed' unless maven_status&.success?

java_feature = combined[/java\.version\s*=\s*(\d+)(?:\.|$)/, 1]&.to_i
maven_java_feature = combined[/Java version:\s*(\d+)(?:\.|,|\s|$)/, 1]&.to_i
maven_version = combined[/Apache Maven\s+([0-9]+(?:\.[0-9]+)+)/, 1]
errors << "runtime Java feature #{java_feature.inspect} does not match POM release #{expected_feature}" unless java_feature == expected_feature
errors << "Maven Java feature #{maven_java_feature.inspect} does not match POM release #{expected_feature}" unless maven_java_feature == expected_feature

parse_version = ->(value) { value.to_s.split('.').map(&:to_i) }
compare_versions = lambda do |left, right|
  left = parse_version.call(left)
  right = parse_version.call(right)
  length = [left.length, right.length].max
  left = left + [0] * (length - left.length)
  right = right + [0] * (length - right.length)
  left <=> right
end

if java_range && (range = java_range.match(/\A\[([0-9]+),([0-9]+)\)\z/))
  lower, upper = range.captures.map(&:to_i)
  errors << "POM Java range #{java_range} excludes compiler release #{expected_feature}" unless expected_feature >= lower && expected_feature < upper
else
  errors << "unsupported POM Java range: #{java_range.inspect}"
end

if maven_range && (range = maven_range.match(/\A\[([0-9.]+),([0-9.]+)\)\z/)) && maven_version
  lower, upper = range.captures
  in_range = compare_versions.call(maven_version, lower) >= 0 && compare_versions.call(maven_version, upper) < 0
  errors << "Maven #{maven_version} is outside POM range #{maven_range}" unless in_range
else
  errors << "cannot validate Maven range #{maven_range.inspect} against #{maven_version.inspect}"
end

errors << "compiler release mismatch: #{compiler_release.inspect}" unless compiler_release == expected_feature.to_s
baseline.fetch('managedRuntimeLibraries').each do |library|
  errors << "managed dependency absent: #{library}" unless pom.include?(library)
end
warnings = []
if architecture['runtimeFeature'].to_i != expected_feature
  warnings << "compatibility config runtimeFeature=#{architecture['runtimeFeature']} differs from effective POM release=#{expected_feature}; POM is authoritative"
end
report = {
  schemaVersion: 1,
  generatedAt: Time.now.utc.iso8601,
  task: 'TOOL10',
  passed: errors.empty?,
  baseline: architecture.merge('effectivePomPolicy' => {
    'compilerRelease' => compiler_release,
    'javaRange' => java_range,
    'mavenRange' => maven_range
  }),
  observed: combined.lines.grep(/java\.version =|Java version:|Apache Maven/).map(&:strip),
  warnings: warnings,
  errors: errors
}
File.write(File.join(root, 'docs/generated/tool10-java-runtime-compatibility.json'), JSON.pretty_generate(report) + "\n")
puts "java-runtime-compatibility: #{errors.empty? ? 'passed' : 'failed'}"
exit(errors.empty? ? 0 : 1)
