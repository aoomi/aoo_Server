#!/usr/bin/env ruby
# frozen_string_literal: true

require "json"
require "fileutils"

root = File.expand_path("..", __dir__)
gitignore = File.read(File.join(root, ".gitignore"))
releaseignore = File.read(File.join(root, ".releaseignore"))
categories = {
  logs: ["logs/", "server/logs/"],
  work: ["work/"],
  backup: ["database/backups/", "*.backup", "*.dump"],
  secrets: ["*.pem", "*.key", "*.p12", "*.jks", ".env"],
  caches: ["__pycache__/", "*.pyc"],
  ide: ["*.iml", ".classpath", ".project", ".settings/"],
  generated: ["target/", "build/", "bin/", "library/", "temp/", "node_modules/"]
}
missing_git = categories.each_with_object({}) do |(category, patterns), result|
  missing = patterns.reject { |pattern| gitignore.include?(pattern) }
  result[category] = missing unless missing.empty?
end
missing_release = categories.each_with_object({}) do |(category, patterns), result|
  missing = patterns.reject { |pattern| releaseignore.include?(pattern) }
  result[category] = missing unless missing.empty?
end
poms = Dir[File.join(root, "{pom.xml,server/**/pom.xml}")].select { |path| File.file?(path) }
forbidden_resource_refs = poms.select do |path|
  File.read(path).match?(%r{<(?:directory|source)>[^<]*(?:/logs|/work|/target|/build|/library|/temp|database/(?:original|backups))})
end
checks = {
  gitIgnoreComplete: missing_git.empty?,
  releaseIgnoreComplete: missing_release.empty?,
  historicalSourcesReleaseExcluded: %w[reference/ database/original/ database/backups/ test-move/].all? { |pattern| releaseignore.include?(pattern) },
  noForbiddenMavenResource: forbidden_resource_refs.empty?
}
report = {
  task: "HYGIENE10",
  status: checks.values.all? ? "passed" : "failed",
  checks: checks,
  missingGitIgnore: missing_git,
  missingReleaseIgnore: missing_release,
  forbiddenMavenResources: forbidden_resource_refs.map { |path| path.delete_prefix(root + "/") }
}
FileUtils.mkdir_p(File.join(root, "work", "audit"))
File.write(File.join(root, "work", "audit", "hygiene10-ignore-policy.json"), JSON.pretty_generate(report) + "\n")
puts JSON.generate(report)
exit(checks.values.all? ? 0 : 2)
