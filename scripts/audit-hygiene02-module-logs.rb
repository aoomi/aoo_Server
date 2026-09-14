#!/usr/bin/env ruby
# frozen_string_literal: true

require "json"
require "fileutils"

root = File.expand_path("..", __dir__)
module_logs = File.join(root, "server", "logs")
files = Dir[File.join(module_logs, "**", "*")].select { |path| File.file?(path) }
ignore_lines = File.read(File.join(root, ".gitignore")).lines.map(&:strip)
max_file = 100 * 1024 * 1024
max_total = 1024 * 1024 * 1024
total = files.sum { |path| File.size(path) }
oversized = files.select { |path| File.size(path) > max_file }.map { |path| path.delete_prefix(root + "/") }

sensitive_pattern = /(?:password|passwd|access[_-]?token|refresh[_-]?token|authorization|cookie|private[_-]?key|idcard|身份证号|手机号)\s*[=:]\s*["']?[^\s,"'}]{4,}/i
sensitive = []
files.each do |path|
  File.foreach(path).with_index(1) do |line, number|
    sensitive << { file: path.delete_prefix(root + "/"), line: number } if line.match?(sensitive_pattern)
    break if sensitive.length >= 100
  end
  break if sensitive.length >= 100
end

checks = {
  sharedPolicy: File.file?(File.join(root, "docs", "运行日志治理规范.md")),
  repositoryIgnored: ignore_lines.include?("/server/logs/"),
  noOversizedFile: oversized.empty?,
  totalWithinBudget: total <= max_total,
  noSensitiveAssignments: sensitive.empty?
}
report = {
  task: "HYGIENE02",
  status: checks.values.all? ? "passed" : "failed",
  checks: checks,
  inventory: { files: files.length, bytes: total },
  limits: { fileBytes: max_file, directoryBytes: max_total, retentionDays: 7 },
  oversizedFiles: oversized,
  sensitiveHits: sensitive
}
FileUtils.mkdir_p(File.join(root, "work", "audit"))
File.write(File.join(root, "work", "audit", "hygiene02-module-logs.json"), JSON.pretty_generate(report) + "\n")
puts JSON.generate(report)
exit(checks.values.all? ? 0 : 2)
