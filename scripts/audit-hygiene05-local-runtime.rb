#!/usr/bin/env ruby
# frozen_string_literal: true

require "json"
require "fileutils"

root = File.expand_path("..", __dir__)
paths = %w[work/runtime work/local-runtime]
files = paths.flat_map { |relative| Dir[File.join(root, relative, "**", "*")] }.select { |path| File.file?(path) }
ignore = File.read(File.join(root, ".gitignore")).lines.map(&:strip)
release_files = Dir[File.join(root, "{pom.xml,server/**/pom.xml,deploy/**/*,Dockerfile,**/Dockerfile}")].select { |path| File.file?(path) }
release_refs = release_files.each_with_object([]) do |path, result|
  result << path.delete_prefix(root + "/") if File.binread(path).match?(%r{work/(?:runtime|local-runtime)})
end
sensitive_names = /(?:\.properties|\.json|\.sql|\.sh)\z/i
unsafe_modes = files.select { |path| path.match?(sensitive_names) && (File.stat(path).mode & 0o077).positive? }
absolute_path_files = files.select do |path|
  next false if File.size(path) > 20 * 1024 * 1024
  File.binread(path).include?("/Users/")
end
module_runtime_files = Dir[File.join(root, "server", "*", "work", "runtime", "**", "*")].select { |path| File.file?(path) }

checks = {
  policyPresent: File.file?(File.join(root, "docs", "本地运行产物边界规范.md")),
  runtimeIgnored: ignore.include?("/work/runtime/"),
  localRuntimeIgnored: ignore.include?("/work/local-runtime/"),
  releaseUnreachable: release_refs.empty?,
  sensitiveArtifactsOwnerOnly: unsafe_modes.empty?,
  moduleRuntimeEmpty: module_runtime_files.empty?
}
report = {
  task: "HYGIENE05",
  status: checks.values.all? ? "passed" : "failed",
  checks: checks,
  inventory: { files: files.length, bytes: files.sum { |path| File.size(path) } },
  isolatedAbsolutePathFiles: absolute_path_files.map { |path| path.delete_prefix(root + "/") },
  releaseReferences: release_refs,
  unsafePermissionFiles: unsafe_modes.map { |path| path.delete_prefix(root + "/") },
  moduleRuntimeFiles: module_runtime_files.map { |path| path.delete_prefix(root + "/") }
}
FileUtils.mkdir_p(File.join(root, "work", "audit"))
File.write(File.join(root, "work", "audit", "hygiene05-local-runtime.json"), JSON.pretty_generate(report) + "\n")
puts JSON.generate(report.merge(isolatedAbsolutePathFiles: absolute_path_files.length))
exit(checks.values.all? ? 0 : 2)
