#!/usr/bin/env ruby
# frozen_string_literal: true

require "json"
require "digest"
require "fileutils"

root = File.expand_path("..", __dir__)
quarantine = File.join(root, "reference", "legacy-2.22")
binary_roots = [File.join(quarantine, "common-lib"), File.join(quarantine, "root-common-runtime")]
files = binary_roots.flat_map { |path| Dir[File.join(path, "**", "*")] }.select { |path| File.file?(path) }
entries = files.map do |path|
  name = File.basename(path)
  version = name[/-(\d+(?:\.\d+)+(?:[-.][A-Za-z0-9]+)*)\.(?:jar|zip)\z/, 1]
  role = if name.end_with?("-sources.jar")
           "legacy-source-reference"
         elsif name.end_with?("-javadoc.jar")
           "legacy-documentation-reference"
         elsif name.end_with?(".jar", ".zip")
           "legacy-binary-reference"
         else
           "legacy-runtime-evidence"
         end
  {
    path: path.delete_prefix(root + "/"),
    sha256: Digest::SHA256.file(path).hexdigest,
    bytes: File.size(path),
    inferredVersion: version,
    role: role,
    source: "2.22 common runtime import",
    productionRequired: false,
    retainedFor: "historical behavior, format and dependency comparison"
  }
end

poms = Dir[File.join(root, "{pom.xml,server/**/pom.xml}")].select { |path| File.file?(path) }
pom_refs = poms.select { |path| File.read(path).match?(%r{(?:common/(?:bin|lib)|reference/legacy-2\.22)}) }
checks = {
  activeRootCommonBinAbsent: !File.exist?(File.join(root, "common", "bin")),
  activeRootCommonLibAbsent: !File.exist?(File.join(root, "common", "lib")),
  legacyCommonBinAbsent: !File.exist?(File.join(root, "server", "LegacyCommon", "bin")),
  quarantinePresent: entries.length >= 100,
  everyFileHasHashAndDisposition: entries.all? { |entry| entry[:sha256].length == 64 && entry[:role] && entry[:productionRequired] == false },
  productionPomUnreachable: pom_refs.empty?
}

generated = File.join(root, "docs", "generated", "legacy-common-binary-inventory.json")
FileUtils.mkdir_p(File.dirname(generated))
File.write(generated, JSON.pretty_generate(entries) + "\n")
report = {
  task: "HYGIENE06",
  status: checks.values.all? ? "passed" : "failed",
  checks: checks,
  inventory: { files: entries.length, bytes: entries.sum { |entry| entry[:bytes] } },
  productionPomReferences: pom_refs.map { |path| path.delete_prefix(root + "/") },
  ledger: "docs/generated/legacy-common-binary-inventory.json"
}
FileUtils.mkdir_p(File.join(root, "work", "audit"))
File.write(File.join(root, "work", "audit", "hygiene06-common-binaries.json"), JSON.pretty_generate(report) + "\n")
puts JSON.generate(report)
exit(checks.values.all? ? 0 : 2)
