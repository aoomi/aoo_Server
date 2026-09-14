#!/usr/bin/env ruby
# frozen_string_literal: true

require "json"
require "fileutils"

root = File.expand_path("..", __dir__)
roots = %w[pom.xml .mvn server tools scripts deploy config].map { |path| File.join(root, path) }
files = roots.flat_map { |path| File.directory?(path) ? Dir[File.join(path, "**", "*")] : [path] }
  .select { |path| File.file?(path) }
  .reject { |path| path == __FILE__ || path.include?("/target/") || path.include?("/build/") || path.include?("/work/") || path.include?("/logs/") || path.end_with?(".class", ".jar", ".zip", ".rb") || path.end_with?("tools/legacy-isolation/audit.json") }

violations = []
files.each do |path|
  content = File.binread(path)
  next unless content.valid_encoding?
  content.each_line.with_index(1) do |line, number|
    violations << { file: path.delete_prefix(root + "/"), line: number } if line.match?(%r{(?:/Users/[A-Za-z0-9._-]+/|[A-Za-z]:\\Users\\)})
  end
end

runtime_classpaths = Dir[File.join(root, "server", "**", "runtime-classpath.txt")].reject { |path| path.include?("/target/") }
checks = {
  noHostAbsolutePathInRuntimeSources: violations.empty?,
  noCheckedInRuntimeClasspath: runtime_classpaths.empty?,
  generatedClasspathIgnored: File.read(File.join(root, ".gitignore")).lines.map(&:strip).include?("**/runtime-classpath.txt"),
  javaToolchainIsWorkspaceRelative: File.read(File.join(root, "tools", "runtime-java26.sh")).include?("$ROOT_DIR/../.toolchains")
}
report = {
  task: "HYGIENE09",
  status: checks.values.all? ? "passed" : "failed",
  checks: checks,
  violations: violations,
  runtimeClasspathFiles: runtime_classpaths.map { |path| path.delete_prefix(root + "/") }
}
FileUtils.mkdir_p(File.join(root, "work", "audit"))
File.write(File.join(root, "work", "audit", "hygiene09-portable-paths.json"), JSON.pretty_generate(report) + "\n")
puts JSON.generate(report)
exit(checks.values.all? ? 0 : 2)
