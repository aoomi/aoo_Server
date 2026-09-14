#!/usr/bin/env ruby
# frozen_string_literal: true

require "json"
require "fileutils"

root = File.expand_path("..", __dir__)
cache_dirs = Dir[File.join(root, "**", "__pycache__")].reject { |path| path.include?("/target/") || path.include?("/build/") }
bytecode = Dir[File.join(root, "**", "*.{pyc,pyo}")].reject { |path| path.include?("/target/") || path.include?("/build/") }
ignore = File.read(File.join(root, ".gitignore"))
checks = {
  noPythonCacheDirectories: cache_dirs.empty?,
  noPythonBytecode: bytecode.empty?,
  cacheDirectoryIgnored: ignore.lines.map(&:strip).include?("**/__pycache__/"),
  bytecodeIgnored: ignore.lines.map(&:strip).include?("*.py[cod]")
}
report = {
  task: "HYGIENE07",
  status: checks.values.all? ? "passed" : "failed",
  checks: checks,
  cacheDirectories: cache_dirs.map { |path| path.delete_prefix(root + "/") },
  bytecodeFiles: bytecode.map { |path| path.delete_prefix(root + "/") }
}
FileUtils.mkdir_p(File.join(root, "work", "audit"))
File.write(File.join(root, "work", "audit", "hygiene07-python-cache.json"), JSON.pretty_generate(report) + "\n")
puts JSON.generate(report)
exit(checks.values.all? ? 0 : 2)
