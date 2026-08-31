#!/usr/bin/env ruby
# frozen_string_literal: true

require 'json'
require 'rexml/document'

ROOT = File.expand_path('..', __dir__)
POLICY = JSON.parse(File.read(File.join(ROOT, 'config/engineering-governance.json'), encoding: 'UTF-8'))
errors = []

pom_text = File.read(File.join(ROOT, 'pom.xml'), encoding: 'UTF-8')
pom = REXML::Document.new(pom_text)
java = POLICY.dig('authoritativeVersions', 'java')
maven = POLICY.dig('authoritativeVersions', 'maven')
errors << "pom compiler release is not #{java}" unless pom_text.include?("<maven.compiler.release>#{java}</maven.compiler.release>")
errors << "enforcer Java range is not [#{java},#{java.to_i + 1})" unless pom_text.include?("<requireJavaVersion><version>[#{java},#{java.to_i + 1})</version></requireJavaVersion>")
errors << "enforcer Maven lower bound is not #{maven}" unless pom_text.include?("<requireMavenVersion><version>[#{maven},4.0.0)</version></requireMavenVersion>")

wrapper = File.read(File.join(ROOT, '.mvn/wrapper/maven-wrapper.properties'), encoding: 'UTF-8')
errors << "wrapper is not Maven #{maven}" unless wrapper.include?("apache-maven-#{maven}-bin")

Dir[File.join(ROOT, '.github/workflows/*.{yml,yaml}')].each do |path|
  text = File.read(path, encoding: 'UTF-8')
  next unless text.include?('setup-java')
  errors << "#{path.delete_prefix(ROOT + '/')} does not use Java #{java}" unless text.match?(/java-version:\s*['\"]?#{Regexp.escape(java)}['\"]?/)
end

errors << 'dynamic Maven version found' if pom_text.match?(/<version>\s*(?:LATEST|RELEASE|[^<]*-SNAPSHOT)\s*<\/version>/i)
errors << 'file:// Maven repository found' if pom_text.match?(/<url>\s*file:/i)

required = POLICY.fetch('requiredEvidence')
errors << 'requiredEvidence entries must be unique' unless required.uniq.size == required.size
errors << 'production must not be a default environment value' if POLICY.dig('environmentSchema', 'AOO_ENV', 'default') == 'production'
errors << 'high severity CVE budget must be zero' unless POLICY.dig('releasePolicy', 'highSeverityCveAllowed') == 0
errors << 'SBOM must be required' unless POLICY.dig('releasePolicy', 'sbomRequired') == true

capacity = POLICY.fetch('capacityPolicy')
java_lines = Dir[File.join(ROOT, 'server/**/*.java')].reject { |p| p.include?('/target/') || p.include?('/build/') }
                                                    .map { |p| [p, File.foreach(p).count] }
oversized = java_lines.select { |_p, lines| lines > capacity.fetch('newJavaFileMaxLines') }
errors << "oversized Java file count grew to #{oversized.size}" if oversized.size > capacity.fetch('legacyOversizedJavaFilesMax')
largest = java_lines.map(&:last).max || 0
errors << "largest Java file grew to #{largest} lines" if largest > capacity.fetch('legacyLargestJavaFileMaxLines')
binaries = Dir[File.join(ROOT, 'server/**/*.{jar,so,dll,dylib}')].reject { |p| p.include?('/target/') }.count
errors << "embedded binary count grew to #{binaries}" if binaries > capacity.fetch('embeddedBinaryFilesMax')

if errors.empty?
  puts "engineering-governance: PASS (Java #{java}, Maven #{maven}, #{required.size} evidence classes)"
  exit 0
end

warn "engineering-governance: FAIL\n- #{errors.join("\n- ")}"
exit 1
