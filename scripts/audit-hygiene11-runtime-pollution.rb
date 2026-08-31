#!/usr/bin/env ruby
# frozen_string_literal: true

require "json"
require "fileutils"

root = File.expand_path("..", __dir__)
generated_names = %w[target build library temp node_modules __pycache__]
generated_dirs = Dir[File.join(root, "**", "*")].select do |path|
  File.directory?(path) && generated_names.include?(File.basename(path))
end.reject { |path| path.include?("/reference/") || path.include?("/work/") }

allowed_segments = %w[/target/ /build/ /library/ /temp/ /node_modules/ /work/ /reference/ /third-party/]
artifacts = Dir[File.join(root, "**", "*.{class,jar,war,pyc,pyo}")].select { |path| File.file?(path) }
pollution = artifacts.reject { |path| allowed_segments.any? { |segment| path.include?(segment) } }
release_ignore = File.read(File.join(root, ".releaseignore"))
clean_evidence_path = File.join(root, "work", "audit", "unused38-clean-recovery.json")
clean_evidence = File.file?(clean_evidence_path) ? JSON.parse(File.read(clean_evidence_path)) : {}
checks = {
  everyGeneratedDirectoryReleaseExcluded: generated_dirs.all? { |path| release_ignore.include?("**/#{File.basename(path)}/") },
  noCompiledArtifactInSourceOrLogs: pollution.empty?,
  cleanRecoveryEvidencePassed: clean_evidence.dig("checks", "freshReactorJars") && clean_evidence.dig("checks", "freshCompiledClasses"),
  smokeHelperUsesRuntimeWorkdir: File.read(File.join(root, "tools", "runtime_smoke_backends_equivalent.py")).include?('ROOT / "work" / "runtime"')
}
report = {
  task: "HYGIENE11",
  status: checks.values.all? ? "passed" : "failed",
  checks: checks,
  generatedDirectories: generated_dirs.map { |path| path.delete_prefix(root + "/") },
  generatedDirectoryCount: generated_dirs.length,
  sourcePollution: pollution.map { |path| path.delete_prefix(root + "/") },
  cleanEvidence: "work/audit/unused38-clean-recovery.json"
}
FileUtils.mkdir_p(File.join(root, "work", "audit"))
File.write(File.join(root, "work", "audit", "hygiene11-runtime-pollution.json"), JSON.pretty_generate(report) + "\n")
puts JSON.generate(report.merge(generatedDirectories: generated_dirs.length))
exit(checks.values.all? ? 0 : 2)
