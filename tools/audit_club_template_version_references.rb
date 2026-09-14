#!/usr/bin/env ruby
# frozen_string_literal: true

require "json"
require "fileutils"
require "time"

ROOT = File.expand_path("..", __dir__)
OUTPUT = File.join(ROOT, "work/audit/club-template-version-reference-audit.json")
SCHEMA_FILES = Dir.glob(File.join(ROOT, "database/**/*.sql"))
JAVA_FILES = Dir.glob(File.join(ROOT, "server/**/*.java"))

template_terms = /(?:bigUnionRoomConfig|club[_A-Za-z]*room[_A-Za-z]*config|union[_A-Za-z]*room[_A-Za-z]*config)/i
schemas = SCHEMA_FILES.each_with_object([]) do |path, rows|
  text = File.read(path, encoding: "UTF-8", invalid: :replace, undef: :replace)
  next unless text.match?(template_terms)
  rows << {
    "path" => path.delete_prefix(ROOT + "/"),
    "hasGameId" => text.match?(/game_?id/i),
    "hasPlayVersion" => text.match?(/(?:play|game|profile)_?version/i),
    "hasProfileForeignKey" => text.match?(/FOREIGN KEY[\s\S]{0,300}REFERENCES\s+game_profile_version/i),
    "hasRestrictDelete" => text.match?(/ON DELETE\s+RESTRICT/i),
    "hasCompatibilityState" => text.match?(/compatib|deprecated|disabled|status/i)
  }
end

code_refs = JAVA_FILES.each_with_object([]) do |path, rows|
  text = File.read(path, encoding: "UTF-8", invalid: :replace, undef: :replace)
  next unless text.match?(template_terms)
  rows << {
    "path" => path.delete_prefix(ROOT + "/"),
    "readsPlayVersion" => text.match?(/(?:get|set)?PlayVersion|playVersion/i),
    "checksEnabledVersion" => text.match?(/(?:enabled|active|disabled).{0,100}(?:version|profile)|(?:version|profile).{0,100}(?:enabled|active|disabled)/i),
    "hasCompatibilityPolicy" => text.match?(/compatib|upgrade.*version|version.*upgrade/i),
    "canDelete" => text.match?(/delete|remove/i),
    "canDisable" => text.match?(/status|disable/i)
  }
end

profile_schema = SCHEMA_FILES.map { |path| [path, File.read(path, encoding: "UTF-8", invalid: :replace, undef: :replace)] }
                              .select { |_path, text| text.match?(/CREATE TABLE IF NOT EXISTS\s+game_profile_version/i) }
profile_delete_restricted = profile_schema.any? { |_path, text| text.match?(/REFERENCES\s+game_profile_version[\s\S]{0,120}ON DELETE\s+RESTRICT/i) }
violations = []
violations << "NO_TEMPLATE_SCHEMA_DISCOVERED" if schemas.empty?
violations << "TEMPLATE_MISSING_PLAY_VERSION" unless schemas.any? { |row| row["hasPlayVersion"] }
violations << "TEMPLATE_MISSING_PROFILE_FOREIGN_KEY" unless schemas.any? { |row| row["hasProfileForeignKey"] }
violations << "PROFILE_DELETE_NOT_RESTRICTED_BY_TEMPLATE" unless profile_delete_restricted
violations << "SAVE_PATH_DOES_NOT_BIND_PLAY_VERSION" unless code_refs.any? { |row| row["readsPlayVersion"] }
violations << "NO_DISABLED_VERSION_GUARD" unless code_refs.any? { |row| row["checksEnabledVersion"] }
violations << "NO_TEMPLATE_COMPATIBILITY_POLICY" unless code_refs.any? { |row| row["hasCompatibilityPolicy"] }

result = {
  "schemaVersion" => 1,
  "generatedAt" => Time.now.utc.iso8601,
  "gate" => { "passed" => violations.empty?, "runtimeVerified" => false },
  "summary" => {
    "templateSchemaFiles" => schemas.length,
    "templateCodeFiles" => code_refs.length,
    "versionedSchemaFiles" => schemas.count { |row| row["hasPlayVersion"] },
    "profileForeignKeys" => schemas.count { |row| row["hasProfileForeignKey"] },
    "versionAwareCodeFiles" => code_refs.count { |row| row["readsPlayVersion"] },
    "violations" => violations.length
  },
  "violations" => violations,
  "schemas" => schemas,
  "codeReferences" => code_refs,
  "requiredLifecycle" => {
    "delete" => "RESTRICT while any template, room, record or replay references the version",
    "disable" => "prevent new template/room creation but preserve existing-room restore and replay",
    "compatibility" => "explicit version converter or immutable historical execution; never silently substitute active version"
  },
  "limitations" => [
    "Static evidence only; live foreign-key metadata and existing orphan rows require a database snapshot.",
    "Legacy JSON may contain an undeclared version field; that does not replace a typed indexed foreign key."
  ]
}

FileUtils.mkdir_p(File.dirname(OUTPUT))
File.write(OUTPUT, JSON.pretty_generate(result) + "\n")
puts JSON.generate(result["summary"].merge("passed" => result["gate"]["passed"]))
