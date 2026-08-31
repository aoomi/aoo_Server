#!/usr/bin/env ruby
# frozen_string_literal: true

require "json"
require "fileutils"
require "time"

ROOT = File.expand_path("..", __dir__)
SOURCES = {
  "server" => File.join(ROOT, "server/LegacyCommon/conf/jsonData/gamecreate.json"),
  "client" => File.expand_path("../Client/assets/Common/Config/Legacy/assets/jsonData/gameCreate.json", ROOT)
}.freeze
OUTPUT = File.join(ROOT, "work/audit/configuration-combination-audit.json")
RELATION_FIELDS = %w[Requires Require DependsOn Dependencies Conflicts Excludes Mutex MutualExclusion].freeze

def indexes(value)
  value.to_s.split(",").map(&:strip).reject(&:empty?).map { |entry| Integer(entry, exception: false) }.compact
end

def references(value)
  case value
  when Array then value.map(&:to_s)
  when String then value.split(",").map(&:strip).reject(&:empty?)
  else []
  end
end

datasets = {}
violations = []
relations = []
SOURCES.each do |name, path|
  abort "missing #{name} game-create configuration" unless File.file?(path)
  rows = JSON.parse(File.read(path)).values
  datasets[name] = rows
  rows.group_by { |row| row["GameName"].to_s.downcase }.each do |game, game_rows|
    duplicate_keys = game_rows.group_by { |row| row["Key"] }.select { |_key, grouped| grouped.length > 1 }
    duplicate_keys.each_key { |key| violations << { "source" => name, "game" => game, "key" => key, "type" => "DUPLICATE_KEY" } }
    game_rows.each do |row|
      key = row["Key"].to_s
      count = row["ToggleCount"].to_i
      toggle_type = row["ToggleType"].to_i
      defaults = indexes(row["ShowIndexs"])
      violations << { "source" => name, "game" => game, "key" => key, "type" => "INVALID_TOGGLE_COUNT", "value" => count } if count <= 0
      violations << { "source" => name, "game" => game, "key" => key, "type" => "INVALID_TOGGLE_TYPE", "value" => toggle_type } unless [0, 1].include?(toggle_type)
      invalid_defaults = defaults.reject { |index| index.between?(0, count) }
      unless invalid_defaults.empty?
        violations << { "source" => name, "game" => game, "key" => key, "type" => "DEFAULT_OUT_OF_RANGE", "values" => invalid_defaults }
      end
      if toggle_type == 0 && defaults.length > 1
        violations << { "source" => name, "game" => game, "key" => key, "type" => "MULTIPLE_DEFAULTS_FOR_SINGLE_CHOICE", "values" => defaults }
      end
      descriptions = row["ToggleDesc"].to_s.split(",", -1)
      if descriptions.length != count && !row["ToggleDesc"].to_s.empty?
        violations << { "source" => name, "game" => game, "key" => key, "type" => "DESCRIPTION_COUNT_MISMATCH", "expected" => count, "actual" => descriptions.length }
      end
      RELATION_FIELDS.each do |field|
        references(row[field]).each do |target|
          relations << { "source" => name, "game" => game, "from" => key, "to" => target, "kind" => field }
        end
      end
    end
  end
end

server_rows = datasets.fetch("server")
client_rows = datasets.fetch("client")
fingerprint = lambda do |row|
  [row["GameName"], row["Key"], row["ToggleType"], row["ToggleCount"], row["ShowIndexs"], row["ToggleDesc"]]
end
server_set = server_rows.map(&fingerprint).to_h { |value| [value, true] }
client_set = client_rows.map(&fingerprint).to_h { |value| [value, true] }
drift = {
  "serverOnly" => (server_set.keys - client_set.keys),
  "clientOnly" => (client_set.keys - server_set.keys)
}

nodes = relations.map { |relation| [relation["game"], relation["from"]] }.uniq
edges = relations.group_by { |relation| [relation["game"], relation["from"]] }
cycles = []
visit = lambda do |node, stack, seen|
  if stack.include?(node)
    cycles << (stack[stack.index(node)..] + [node])
    next
  end
  next if seen[node]
  seen[node] = true
  (edges[node] || []).each { |edge| visit.call([edge["game"], edge["to"]], stack + [node], seen) }
end
seen = {}
nodes.each { |node| visit.call(node, [], seen) }

publication_sources = Dir.glob(File.join(ROOT, "server/ConfigCenter/src/main/java/**/*.java"))
publication_evaluator = publication_sources.any? do |path|
  File.read(path).match?(/(?:dependency|mutual|exclusive|conflict|combination).*(?:validate|evaluate)|(?:validate|evaluate).*(?:dependency|mutual|exclusive|conflict|combination)/i)
end

result = {
  "schemaVersion" => 1,
  "generatedAt" => Time.now.utc.iso8601,
  "gate" => {
    "passed" => violations.empty? && drift.values.all?(&:empty?) && !relations.empty? && cycles.empty? && publication_evaluator,
    "publicationEvaluatorPresent" => publication_evaluator,
    "runtimeVerified" => false
  },
  "summary" => {
    "serverRows" => server_rows.length,
    "clientRows" => client_rows.length,
    "games" => server_rows.map { |row| row["GameName"] }.uniq.length,
    "structuralViolations" => violations.length,
    "declaredRelations" => relations.length,
    "dependencyCycles" => cycles.length,
    "serverOnlyRows" => drift["serverOnly"].length,
    "clientOnlyRows" => drift["clientOnly"].length,
    "publicationEvaluatorPresent" => publication_evaluator
  },
  "violations" => violations,
  "relations" => relations,
  "cycles" => cycles.uniq,
  "drift" => drift,
  "limitations" => [
    "No declared relation is interpreted as missing evidence, not as proof that every option is independent.",
    "Dynamic and legacy code-only constraints require explicit schema migration before exhaustive combination generation.",
    "All valid and invalid combinations still require server-side pre-publication evaluation and property-based tests."
  ]
}

FileUtils.mkdir_p(File.dirname(OUTPUT))
File.write(OUTPUT, JSON.pretty_generate(result) + "\n")
puts JSON.generate(result["summary"].merge("passed" => result["gate"]["passed"]))
