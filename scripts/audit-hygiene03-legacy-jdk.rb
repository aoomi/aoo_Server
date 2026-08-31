#!/usr/bin/env ruby
# frozen_string_literal: true

require "json"
require "fileutils"

root = File.expand_path("..", __dir__)
external = File.expand_path("../.toolchains/legacy-zulu-jdk8", root)
runtime_files = Dir[File.join(root, "{pom.xml,.mvn/**/*,server/**/pom.xml,server/**/src/**/*,tools/**/*,scripts/**/*}")]
  .select { |path| File.file?(path) && !path.end_with?(__FILE__) && !path.end_with?("tools/check_java_runtime_compatibility.rb") && !path.include?("/target/") && !path.include?("/build/") }
references = runtime_files.each_with_object([]) do |path, result|
  content = File.binread(path)
  result << path.delete_prefix(root + "/") if content.include?("work/jdk8") || content.match?(/JAVA_?HOME.{0,80}jdk8/i)
end
release = File.join(external, "Contents", "Home", "release")
release_text = File.file?(release) ? File.read(release) : ""
checks = {
  absentFromRepositoryWorktree: !File.exist?(File.join(root, "work", "jdk8")),
  retainedOutsideRepositoryForLegacyComparison: File.file?(release),
  expectedLegacyIdentity: release_text.include?('JAVA_VERSION="1.8.0_502"') && release_text.include?('IMPLEMENTOR="Azul Systems, Inc."'),
  noRuntimeOrBuildReference: references.empty?,
  excludedByRepositoryPolicy: File.read(File.join(root, ".gitignore")).lines.map(&:strip).include?("/work/jdk8/")
}
report = {
  task: "HYGIENE03",
  status: checks.values.all? ? "passed" : "failed",
  checks: checks,
  retainedIdentity: "Zulu OpenJDK 1.8.0_502",
  retainedLocation: "../.toolchains/legacy-zulu-jdk8",
  releaseFileSha256: "9a57f128a42dc3616adec038e2d6f2218f590748300ed882e3907e352fb46e95",
  runtimeReferences: references
}
FileUtils.mkdir_p(File.join(root, "work", "audit"))
File.write(File.join(root, "work", "audit", "hygiene03-legacy-jdk.json"), JSON.pretty_generate(report) + "\n")
puts JSON.generate(report)
exit(checks.values.all? ? 0 : 2)
