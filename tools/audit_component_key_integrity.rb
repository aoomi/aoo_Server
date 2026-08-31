#!/usr/bin/env ruby
# frozen_string_literal: true

require "json"
require "fileutils"
require "time"

ROOT = File.expand_path("..", __dir__)
CATALOG = File.join(ROOT, "work/generated/classification/game-classification.json")
COMPLETENESS = File.join(ROOT, "work/audit/game-component-completeness.json")
OUTPUT = File.join(ROOT, "work/audit/component-key-integrity.json")
TYPES = %w[nativeProvider lifecycle rules flow scoring snapshot].freeze

abort "missing classification catalog" unless File.file?(CATALOG)
abort "missing component completeness evidence" unless File.file?(COMPLETENESS)

catalog = JSON.parse(File.read(CATALOG))
completeness = JSON.parse(File.read(COMPLETENESS))
detail_rows = completeness["games"] || completeness["rows"] || completeness["details"] || []
detail_by_code = detail_rows.each_with_object({}) do |row, memo|
  code = (row["code"] || row["gameCode"]).to_s.downcase
  memo[code] = row unless code.empty?
end

entries = []
catalog.fetch("rows").each do |game|
  code = game.fetch("code").to_s.downcase
  evidence = detail_by_code[code] || {}
  TYPES.each do |type|
    raw = evidence[type] || evidence[type.sub("nativeProvider", "native")]
    present = raw == true || raw.is_a?(String) && !raw.empty? || raw.is_a?(Array) && !raw.empty?
    entries << {
      "key" => "#{code}:#{type}",
      "gameId" => game["gameId"],
      "gameCode" => code,
      "componentType" => type,
      "present" => present,
      "evidence" => raw
    }
  end
end

duplicate_keys = entries.group_by { |entry| entry["key"] }.select { |_key, values| values.length > 1 }
duplicate_game_ids = catalog.fetch("rows").group_by { |game| game["gameId"] }.select { |_id, values| values.length > 1 }
missing = entries.reject { |entry| entry["present"] }
result = {
  "schemaVersion" => 1,
  "generatedAt" => Time.now.utc.iso8601,
  "gate" => {
    "passed" => duplicate_keys.empty? && duplicate_game_ids.empty? && missing.empty?,
    "startupEnforced" => false,
    "requirements" => ["unique component key", "unique game id", "component exists", "component type matches key"]
  },
  "summary" => {
    "games" => catalog.fetch("rows").length,
    "expectedComponentKeys" => entries.length,
    "presentComponentKeys" => entries.length - missing.length,
    "missingComponentKeys" => missing.length,
    "duplicateComponentKeys" => duplicate_keys.length,
    "duplicateGameIds" => duplicate_game_ids.length
  },
  "duplicateComponentKeys" => duplicate_keys,
  "duplicateGameIds" => duplicate_game_ids,
  "missingComponents" => missing,
  "entries" => entries,
  "limitations" => [
    "Static evidence gate; runtime ServiceLoader/bootstrap instantiation is not yet available for every enabled game.",
    "Type compatibility is proven only when completeness evidence identifies the expected SPI component type."
  ]
}

FileUtils.mkdir_p(File.dirname(OUTPUT))
File.write(OUTPUT, JSON.pretty_generate(result) + "\n")
puts JSON.generate(result["summary"].merge("passed" => result["gate"]["passed"]))
