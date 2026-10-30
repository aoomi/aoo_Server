#!/usr/bin/env ruby
# frozen_string_literal: true

require "json"
require "fileutils"

root = File.expand_path("../../..", __dir__)
external = File.expand_path("../.toolchains/legacy-zulu-jdk8", root)
scan_inputs=JSON.parse(File.read(File.join(root, 'engineering/scan-input-roots.json'))); tool_input_roots=scan_inputs.fetch('toolInputs').map { |relative| File.join(root, relative) }; script_input_roots=scan_inputs.fetch('scriptInputs').map { |relative| File.join(root, relative) }; runtime_files = (Dir[File.join(root, '{pom.xml,.mvn/**/*,modules/**/pom.xml,modules/**/src/**/*}')] + (tool_input_roots + script_input_roots).flat_map { |input_root| Dir[File.join(input_root, '**/*')] }).uniq.select do |path|
  input_root = (tool_input_roots + script_input_roots).find { |base| path.start_with?(base + '/') } || root
  relative = path.delete_prefix(input_root + '/')
  File.file?(path) && path != File.expand_path(__FILE__) && path != File.join(root, 'engineering/audit/tool-inputs/check_java_runtime_compatibility.rb') && !('/' + relative).include?('/target/') && !('/' + relative).include?('/build/')
end
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
FileUtils.mkdir_p(File.join(root, "build-only", "audit"))
File.write(File.join(root, "build-only", "audit", "hygiene03-legacy-jdk.json"), JSON.pretty_generate(report) + "\n")
puts JSON.generate(report)
exit(checks.values.all? ? 0 : 2)
