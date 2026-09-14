#!/usr/bin/env ruby
# frozen_string_literal: true

require "json"
require "fileutils"
require "time"

ROOT = File.expand_path("..", __dir__)
CLIENT = File.expand_path("../Client/assets", ROOT)
SERVER = File.join(ROOT, "server")
OUTPUT = File.join(ROOT, "work/audit/create-ui-rule-validator-mapping.json")

def game_code(path)
  normalized = path.tr("\\", "/")
  match = normalized.match(%r{/CompatibilityApp/([^/]+)/}) || normalized.match(%r{/Runtime/([^/]+)/})
  match && match[1].downcase
end

client_fields = Hash.new { |hash, key| hash[key] = [] }
Dir.glob(File.join(CLIENT, "**/*.ts")).sort.each do |path|
  text = File.read(path, encoding: "UTF-8", invalid: :replace, undef: :replace)
  next unless text.match?(/CreateRoom|createRoom|bRoomConfigure|sendPack/)
  relative = path.delete_prefix(CLIENT + "/")
  code = game_code(path) || "shared"
  text.each_line.with_index(1) do |line, number|
    keys = []
    line.scan(/(?:sendPack|bRoomConfigure|roomConfig|ruleConfig|cfg)\s*(?:\?\.)?\.\s*([A-Za-z_$][\w$]*)/) { |m| keys << m[0] }
    line.scan(/(?:sendPack|bRoomConfigure|roomConfig|ruleConfig|cfg)\s*\[\s*["']([^"']+)["']\s*\]/) { |m| keys << m[0] }
    # Native create-room controllers translate a UI option key through one()/many()
    # into the authoritative packet field. Record the packet field as the wire key
    # and retain the UI key in the evidence excerpt.
    line.scan(/([A-Za-z_$][\w$]*)\s*:\s*(?:one|many)\(\s*["']([^"']+)["']/) do |packet_field, ui_field|
      client_fields[[code, packet_field]] << {
        "path" => relative, "line" => number, "uiField" => ui_field,
        "excerpt" => line.strip[0, 240]
      }
    end
    keys.uniq.each do |field|
      client_fields[[code, field]] << { "path" => relative, "line" => number, "excerpt" => line.strip[0, 240] }
    end
  end
end

server_reads = Hash.new { |hash, key| hash[key] = [] }
validators = []
Dir.glob(File.join(SERVER, "**/*.java")).sort.each do |path|
  text = File.read(path, encoding: "UTF-8", invalid: :replace, undef: :replace)
  relative = path.delete_prefix(ROOT + "/")
  text.each_line.with_index(1) do |line, number|
    line.scan(/(?:immutableRules\(\)|rules|config|configure)\s*\.\s*(?:get|getOrDefault|containsKey)\s*\(\s*["']([^"']+)["']/) do |match|
      server_reads[match[0]] << { "path" => relative, "line" => number, "excerpt" => line.strip[0, 240] }
    end
    if line.match?(/\b(?:validate|Validator|require[A-Z]|check[A-Z])\b/)
      validators << { "path" => relative, "line" => number, "excerpt" => line.strip[0, 240] }
    end
  end
end

rows = client_fields.map do |(code, field), occurrences|
  reads = server_reads[field]
  related_validators = reads.flat_map do |read|
    validators.select { |validator| validator["path"] == read["path"] && (validator["line"] - read["line"]).abs <= 20 }
  end.uniq
  {
    "gameCode" => code,
    "uiField" => field,
    "clientOccurrences" => occurrences,
    "serverRuleReads" => reads,
    "serverValidators" => related_validators,
    "mapped" => !reads.empty?,
    "validated" => !related_validators.empty?
  }
end.sort_by { |row| [row["gameCode"], row["uiField"]] }

server_only = server_reads.keys.reject { |field| client_fields.keys.any? { |_code, candidate| candidate == field } }.sort
result = {
  "schemaVersion" => 1,
  "generatedAt" => Time.now.utc.iso8601,
  "gate" => {
    "passed" => !rows.empty? && rows.all? { |row| row["mapped"] && row["validated"] } && server_only.empty?,
    "bidirectional" => true,
    "runtimeVerified" => false
  },
  "summary" => {
    "clientRuleFields" => rows.length,
    "mappedFields" => rows.count { |row| row["mapped"] },
    "validatedFields" => rows.count { |row| row["validated"] },
    "unmappedFields" => rows.count { |row| !row["mapped"] },
    "unvalidatedFields" => rows.count { |row| !row["validated"] },
    "serverOnlyFields" => server_only.length,
    "validatorSites" => validators.length
  },
  "mappings" => rows,
  "serverOnlyFields" => server_only.map { |field| { "field" => field, "reads" => server_reads[field] } },
  "limitations" => [
    "Static mapping covers explicit create-room config property access; computed keys require runtime instrumentation.",
    "A nearby validator site is candidate evidence, not proof of semantic range/type equivalence.",
    "Runtime UI interaction and rejected invalid-value tests remain required before closure."
  ]
}

FileUtils.mkdir_p(File.dirname(OUTPUT))
File.write(OUTPUT, JSON.pretty_generate(result) + "\n")
puts JSON.generate(result["summary"].merge("passed" => result["gate"]["passed"]))
