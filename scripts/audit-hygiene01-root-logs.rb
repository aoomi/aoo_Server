#!/usr/bin/env ruby
# frozen_string_literal: true

require "json"
require "fileutils"

root = File.expand_path("..", __dir__)
log_root = File.join(root, "logs")
files = Dir[File.join(log_root, "**", "*")].select { |path| File.file?(path) }
ignored = File.file?(File.join(root, ".gitignore")) && File.read(File.join(root, ".gitignore")).lines.map(&:strip).include?("/logs/")

max_file = 100 * 1024 * 1024
max_total = 1024 * 1024 * 1024
oversized = files.select { |path| File.size(path) > max_file }.map { |path| path.delete_prefix(root + "/") }
total_bytes = files.sum { |path| File.size(path) }

sensitive = /(?:password|passwd|access[_-]?token|refresh[_-]?token|authorization|cookie|private[_-]?key|idcard|身份证号|手机号)\s*[=:]\s*["']?[^\s,"'}]{4,}/i
sensitive_hits = []
files.each do |path|
  next if File.size(path) > 20 * 1024 * 1024
  next unless File.extname(path).match?(/\A\.(?:log|txt|json|out)\z/i)

  File.foreach(path).with_index(1) do |line, number|
    sensitive_hits << { file: path.delete_prefix(root + "/"), line: number } if line.match?(sensitive)
    break if sensitive_hits.length >= 100
  end
  break if sensitive_hits.length >= 100
end

release_files = Dir[File.join(root, "{pom.xml,server/**/pom.xml,deploy/**/*,Dockerfile,**/Dockerfile}")].select { |path| File.file?(path) }
release_refs = release_files.each_with_object([]) do |path, result|
  relative = path.delete_prefix(root + "/")
  result << relative if File.read(path).match?(%r{(?:^|[>"'])logs/})
end

checks = {
  policyPresent: File.file?(File.join(root, "docs", "运行日志治理规范.md")),
  repositoryIgnored: ignored,
  noOversizedFile: oversized.empty?,
  totalWithinLocalBudget: total_bytes <= max_total,
  noSensitiveAssignments: sensitive_hits.empty?,
  releaseUnreachable: release_refs.empty?
}

report = {
  task: "HYGIENE01",
  status: checks.values.all? ? "passed" : "failed",
  checks: checks,
  inventory: { files: files.length, bytes: total_bytes },
  oversizedFiles: oversized,
  sensitiveHits: sensitive_hits,
  releaseReferences: release_refs
}

FileUtils.mkdir_p(File.join(root, "work", "audit"))
File.write(File.join(root, "work", "audit", "hygiene01-root-logs.json"), JSON.pretty_generate(report) + "\n")
puts JSON.generate(report)
exit(checks.values.all? ? 0 : 2)
