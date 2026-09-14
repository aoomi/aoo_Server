#!/usr/bin/env ruby
# frozen_string_literal: true

require "json"
require "fileutils"
require "time"

ROOT = File.expand_path("..", __dir__)
OUTPUT = File.join(ROOT, "work/audit/data-lifecycle-policy-audit.json")
SQL_FILES = Dir.glob(File.join(ROOT, "database/migrations/*.sql")).sort

tables = []
SQL_FILES.each do |path|
  text = File.read(path, encoding: "UTF-8", invalid: :replace, undef: :replace)
  text.scan(/CREATE TABLE IF NOT EXISTS\s+`?([A-Za-z0-9_]+)`?\s*\((.*?)\)\s*ENGINE/im) do |name, body|
    tables << {
      "table" => name,
      "source" => path.delete_prefix(ROOT + "/"),
      "softDelete" => body.match?(/(?:deleted_at|is_deleted|delete_status|revoked_at|member_status|\bstatus\b)/i),
      "archiveMarker" => body.match?(/archive|cold_storage|retention/i),
      "foreignKeys" => body.scan(/FOREIGN KEY\s*\([^)]+\)\s*REFERENCES\s+`?([A-Za-z0-9_]+)`?(?:\s*\([^)]+\))?(?:\s+ON DELETE\s+(CASCADE|RESTRICT|SET NULL|NO ACTION))?/i).map { |target, action| { "target" => target, "onDelete" => action.to_s.upcase } },
      "timeColumns" => body.scan(/\b([A-Za-z0-9_]*(?:created|updated|published|expired|occurred|granted|resolved|processed|captured)[A-Za-z0-9_]*|[A-Za-z0-9_]*_at)\b/i).flatten.uniq
    }
  end
end

policy_sql = SQL_FILES.map { |path| File.read(path, encoding: "UTF-8", invalid: :replace, undef: :replace) }.join("\n")
seeded_types = policy_sql.scan(/INSERT\s+INTO\s+aoo_data_lifecycle_policy[\s\S]{0,1000}?VALUES\s*\(\s*["']([^"']+)/i).flatten.uniq
delete_actions = tables.flat_map { |table| table["foreignKeys"].map { |fk| fk.merge("table" => table["table"]) } }
unspecified_fks = delete_actions.select { |fk| fk["onDelete"].empty? }
hard_delete_sites = Dir.glob(File.join(ROOT, "server/**/*.java")).each_with_object([]) do |path, rows|
  text = File.read(path, encoding: "UTF-8", invalid: :replace, undef: :replace)
  text.each_line.with_index(1) do |line, number|
    next unless line.match?(/\bDELETE\s+(?:\w+\s+FROM|FROM)\s+[A-Za-z0-9_]+/i)
    rows << { "path" => path.delete_prefix(ROOT + "/"), "line" => number, "sql" => line.strip[0, 300] }
  end
end

objects = tables.reject { |table| table["table"] == "aoo_data_lifecycle_policy" }.map do |table|
  explicit_policy = seeded_types.include?(table["table"])
  strategy = if table["softDelete"] then "SOFT_DELETE_CANDIDATE"
             elsif table["archiveMarker"] then "ARCHIVE_CANDIDATE"
             else "UNSPECIFIED" end
  table.merge("policySeeded" => explicit_policy, "inferredStrategy" => strategy,
              "complete" => explicit_policy && table["foreignKeys"].all? { |fk| !fk["onDelete"].empty? })
end

result = {
  "schemaVersion" => 1,
  "generatedAt" => Time.now.utc.iso8601,
  "gate" => { "passed" => !objects.empty? && objects.all? { |object| object["complete"] }, "runtimeVerified" => false },
  "summary" => {
    "tables" => tables.length,
    "domainObjects" => objects.length,
    "seededPolicies" => seeded_types.length,
    "completeObjects" => objects.count { |object| object["complete"] },
    "unspecifiedForeignKeyActions" => unspecified_fks.length,
    "hardDeleteSites" => hard_delete_sites.length
  },
  "objects" => objects,
  "seededPolicyTypes" => seeded_types,
  "foreignKeyActions" => delete_actions,
  "unspecifiedForeignKeyActions" => unspecified_fks,
  "hardDeleteSites" => hard_delete_sites,
  "requiredDecisionPerObject" => %w[retention hotArchive legalHold anonymization softDelete restore purge referentialAction audit],
  "limitations" => [
    "A status column is only a soft-delete candidate until state values, query filters and restore semantics are explicit.",
    "Database metadata and scheduled archive/purge execution require a live environment audit."
  ]
}

FileUtils.mkdir_p(File.dirname(OUTPUT))
File.write(OUTPUT, JSON.pretty_generate(result) + "\n")
puts JSON.generate(result["summary"].merge("passed" => result["gate"]["passed"]))
