#!/usr/bin/env ruby
# frozen_string_literal: true

require "json"
require "fileutils"
require "time"

ROOT = File.expand_path("..", __dir__)
CATALOG = File.join(ROOT, "work/generated/classification/game-classification.json")
OUTPUT = File.join(ROOT, "work/audit/historical-replay-reference-audit.json")
REQUIRED = %w[gameId playVersion ruleVersion cardCodecVersion eventInterpreterVersion protocolVersion].freeze

replay_files = Dir.glob(File.join(ROOT, "{database,server}/**/*.{sql,java}"), File::FNM_EXTGLOB).select do |path|
  path.match?(/replay|playback|gameRoom|gameSet/i)
end
evidence = replay_files.map do |path|
  text = File.read(path, encoding: "UTF-8", invalid: :replace, undef: :replace)
  fields = REQUIRED.each_with_object({}) do |field, memo|
    snake = field.gsub(/([A-Z])/, '_\\1').downcase
    memo[field] = text.match?(/\b#{Regexp.escape(field)}\b|\b#{Regexp.escape(snake)}\b/i)
  end
  {
    "path" => path.delete_prefix(ROOT + "/"),
    "fields" => fields,
    "isSchema" => path.end_with?(".sql") || text.match?(/CREATE TABLE/i),
    "hasHistoricalLookup" => text.match?(/find\s*\([^)]*(?:version|codec|interpreter)|WHERE[^\n]*(?:version|codec|interpreter)/i)
  }
end

global_fields = REQUIRED.to_h { |field| [field, evidence.any? { |row| row["fields"][field] }] }
schema_fields = REQUIRED.to_h do |field|
  [field, evidence.any? { |row| row["isSchema"] && row["fields"][field] }]
end

catalog = JSON.parse(File.read(CATALOG)).fetch("rows")
java_files = Dir.glob(File.join(ROOT, "server/**/*.java"))
games = catalog.map do |game|
  code = game["code"].to_s
  candidates = java_files.select { |path| path.match?(%r{/#{Regexp.escape(code)}/}i) }
  text = candidates.map { |path| File.read(path, encoding: "UTF-8", invalid: :replace, undef: :replace) }.join("\n")
  codec = text.match?(/class\s+\w*(?:CardCodec|CardEncoder|CardDecoder)|implements\s+\w*(?:CardCodec|CardEncoder|CardDecoder)/i)
  interpreter = text.match?(/class\s+\w*(?:EventInterpreter|ReplayInterpreter)|implements\s+\w*(?:EventInterpreter|ReplayInterpreter)/i)
  historical_test = text.match?(/historical|oldVersion|legacyReplay|versionedReplay/i) && text.match?(/@Test/)
  {
    "gameId" => game["gameId"], "gameCode" => code,
    "cardCodec" => codec, "eventInterpreter" => interpreter,
    "historicalReplayTest" => historical_test,
    "passed" => codec && interpreter && historical_test
  }
end

violations = []
schema_fields.each { |field, present| violations << "REPLAY_SCHEMA_MISSING_#{field.upcase}" unless present }
violations << "NO_VERSIONED_HISTORICAL_LOOKUP" unless evidence.any? { |row| row["hasHistoricalLookup"] }
violations << "GAMES_MISSING_VERSIONED_CODEC_OR_INTERPRETER" unless games.all? { |game| game["passed"] }
result = {
  "schemaVersion" => 1,
  "generatedAt" => Time.now.utc.iso8601,
  "gate" => { "passed" => violations.empty?, "runtimeVerified" => false },
  "summary" => {
    "replayFiles" => evidence.length,
    "games" => games.length,
    "gamesWithCardCodec" => games.count { |game| game["cardCodec"] },
    "gamesWithEventInterpreter" => games.count { |game| game["eventInterpreter"] },
    "historicalReplayTestedGames" => games.count { |game| game["historicalReplayTest"] },
    "completeGames" => games.count { |game| game["passed"] },
    "violations" => violations.length
  },
  "requiredFields" => REQUIRED,
  "globalFieldEvidence" => global_fields,
  "schemaFieldEvidence" => schema_fields,
  "violations" => violations,
  "files" => evidence,
  "games" => games,
  "retentionRule" => "Referenced rule, codec, interpreter and protocol artifacts must outlive every retained record/replay and be deleted only after reference count reaches zero.",
  "limitations" => [
    "Name-based static inventory does not prove semantic decode equivalence.",
    "Historical fixtures must be immutable production-shaped payloads, not freshly encoded data decoded by the same implementation."
  ]
}

FileUtils.mkdir_p(File.dirname(OUTPUT))
File.write(OUTPUT, JSON.pretty_generate(result) + "\n")
puts JSON.generate(result["summary"].merge("passed" => result["gate"]["passed"]))
