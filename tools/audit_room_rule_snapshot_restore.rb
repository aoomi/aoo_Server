#!/usr/bin/env ruby
# frozen_string_literal: true

require "json"
require "fileutils"
require "time"

ROOT = File.expand_path("..", __dir__)
CATALOG = File.join(ROOT, "work/generated/classification/game-classification.json")
OUTPUT = File.join(ROOT, "work/audit/room-rule-snapshot-restore-audit.json")

catalog = JSON.parse(File.read(CATALOG)).fetch("rows")
all_java = Dir.glob(File.join(ROOT, "server/**/*.java"))
test_files = all_java.select { |path| path.match?(%r{/(?:test|src/test)/}) }

rows = catalog.map do |game|
  source = game["source"].to_s
  provider_path = source.start_with?("server/") ? File.join(ROOT, source) : nil
  provider_text = provider_path && File.file?(provider_path) ? File.read(provider_path) : ""
  module_root = provider_path && provider_path.match(%r{\A#{Regexp.escape(ROOT)}/server/[^/]+})&.[](0)
  module_files = module_root ? all_java.select { |path| path.start_with?(module_root + "/") } : []
  implementation_text = module_files.map { |path| File.read(path, encoding: "UTF-8", invalid: :replace, undef: :replace) }.join("\n")
  module_tests = module_root ? test_files.select { |path| path.start_with?(module_root + "/") } : []
  test_text = module_tests.map { |path| File.read(path, encoding: "UTF-8", invalid: :replace, undef: :replace) }.join("\n")
  has_restore = provider_text.match?(/restoreAuthoritativeSession\s*\(/)
  state_has_rules = implementation_text.match?(/authoritativeState\s*\([^)]*\)[\s\S]{0,1800}(?:immutableRules|["']rules["']|ruleSnapshot|configVersion)/)
  restore_reads_rules = implementation_text.match?(/(?:restore|fromSnapshot)\s*\([^)]*\)[\s\S]{0,2200}(?:immutableRules|["']rules["']|ruleSnapshot|configVersion)/)
  round_trip_test = test_text.match?(/restore|fromSnapshot/) && test_text.match?(/authoritativeState|immutableRules|rules/)
  {
    "gameId" => game["gameId"],
    "gameCode" => game["code"],
    "sourceType" => game["sourceType"],
    "provider" => source,
    "roomRuleSnapshotContract" => true,
    "restoreSpiImplemented" => has_restore,
    "authoritativeStateContainsRules" => state_has_rules,
    "restoreReadsRules" => restore_reads_rules,
    "roundTripTest" => round_trip_test,
    "passed" => has_restore && state_has_rules && restore_reads_rules && round_trip_test
  }
end

result = {
  "schemaVersion" => 1,
  "generatedAt" => Time.now.utc.iso8601,
  "gate" => { "passed" => rows.all? { |row| row["passed"] }, "runtimeVerified" => false },
  "summary" => {
    "games" => rows.length,
    "restoreSpiImplemented" => rows.count { |row| row["restoreSpiImplemented"] },
    "stateContainsRules" => rows.count { |row| row["authoritativeStateContainsRules"] },
    "restoreReadsRules" => rows.count { |row| row["restoreReadsRules"] },
    "roundTripTested" => rows.count { |row| row["roundTripTest"] },
    "passedGames" => rows.count { |row| row["passed"] },
    "failedGames" => rows.count { |row| !row["passed"] }
  },
  "games" => rows,
  "requiredSnapshotIdentity" => %w[configId gameId playVersion componentVersion protocolVersion clientBundleVersion uiProfileVersion publishedAt immutableRules],
  "limitations" => [
    "Static source evidence cannot prove database JSON round-trip compatibility or startup restoration.",
    "A family hard-coded in provider source is not equivalent to restoring the immutable room rule snapshot.",
    "Every enabled game requires create-save-restart-restore-state/view equivalence tests."
  ]
}

FileUtils.mkdir_p(File.dirname(OUTPUT))
File.write(OUTPUT, JSON.pretty_generate(result) + "\n")
puts JSON.generate(result["summary"].merge("passed" => result["gate"]["passed"]))
