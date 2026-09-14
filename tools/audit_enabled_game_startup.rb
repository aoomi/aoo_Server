#!/usr/bin/env ruby
# frozen_string_literal: true

require "json"
require "fileutils"
require "open3"
require "time"

ROOT = File.expand_path("..", __dir__)
CATALOG = File.join(ROOT, "server/Bootstrap/src/main/resources/game-catalog-528.tsv")
OUTPUT = File.join(ROOT, "work/audit/enabled-game-startup-audit.json")

lines = File.readlines(CATALOG, chomp: true)
header = lines.shift.split("\t", -1)
catalog = lines.map { |line| header.zip(line.split("\t", -1)).to_h }
service_files = Dir.glob(File.join(ROOT, "server/*/src/main/resources/META-INF/services/com.aoo.bcg.gamespi.GameProvider"))
native_classes = service_files.flat_map { |path| File.readlines(path, chomp: true).map(&:strip).reject(&:empty?) }
native_codes = native_classes.map { |name| name.split(".")[-2].to_s.downcase }.uniq

rows = catalog.map do |game|
  code = game["code"].to_s.downcase
  enabled = game["enabled"] == "1"
  native = native_codes.include?(code)
  {
    "gameId" => game["game_id"].to_i,
    "gameCode" => code,
    "enabled" => enabled,
    "providerType" => native ? "NATIVE_SERVICE_LOADER" : "CATALOG_BRIDGE",
    "providerDiscoverable" => native || File.file?(File.join(ROOT, "server/Bootstrap/src/main/java/com/aoo/bcg/bootstrap/CatalogGameProvider.java")),
    "realGameplayInstantiationProven" => false,
    "startupTestProven" => false
  }
end

java_version, java_status = Open3.capture2e("java", "-version")
reports = Dir.glob(File.join(ROOT, "server/Bootstrap/target/surefire-reports/TEST-*.xml")).map do |path|
  text = File.read(path)
  {
    "path" => path.delete_prefix(ROOT + "/"),
    "modifiedAt" => File.mtime(path).utc.iso8601,
    "tests" => text[/\btests="(\d+)"/, 1].to_i,
    "failures" => text[/\bfailures="(\d+)"/, 1].to_i,
    "errors" => text[/\berrors="(\d+)"/, 1].to_i
  }
end
enabled = rows.select { |row| row["enabled"] }
result = {
  "schemaVersion" => 1,
  "generatedAt" => Time.now.utc.iso8601,
  "gate" => {
    "passed" => !enabled.empty? && enabled.all? { |row| row["realGameplayInstantiationProven"] && row["startupTestProven"] },
    "freshBuildPassed" => false,
    "runtimeVerified" => false,
    "blocker" => "Maven enforcer requires Java [26,27); installed default is Java 25.0.4 and Java 17 only."
  },
  "summary" => {
    "catalogGames" => rows.length,
    "enabledGames" => enabled.length,
    "nativeProviders" => native_classes.length,
    "enabledCatalogBridges" => enabled.count { |row| row["providerType"] == "CATALOG_BRIDGE" },
    "realGameplayInstantiationProven" => enabled.count { |row| row["realGameplayInstantiationProven"] },
    "startupTestProven" => enabled.count { |row| row["startupTestProven"] },
    "surefireReports" => reports.length
  },
  "java" => { "commandSucceeded" => java_status.success?, "version" => java_version.lines.first.to_s.strip },
  "serviceRegistrations" => native_classes,
  "surefireReports" => reports,
  "games" => rows,
  "limitations" => [
    "Catalog bridge construction proves only generic room lifecycle wiring; it does not instantiate migrated gameplay.",
    "Existing Surefire reports are retained as historical evidence but are not fresh after the failed Java-26 toolchain gate.",
    "Closure requires a Java 26 environment plus process startup, health/readiness, room creation and teardown for every enabled game."
  ]
}

FileUtils.mkdir_p(File.dirname(OUTPUT))
File.write(OUTPUT, JSON.pretty_generate(result) + "\n")
puts JSON.generate(result["summary"].merge("passed" => result["gate"]["passed"], "blocker" => result["gate"]["blocker"]))
