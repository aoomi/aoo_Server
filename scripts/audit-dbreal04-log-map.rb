#!/usr/bin/env ruby
# frozen_string_literal: true

require 'json'
require 'pathname'

ROOT = Pathname.new(__dir__).parent
SOURCE = ROOT / 'database/original/clark_log_qh.sql'
MIGRATIONS = ROOT / 'database/migrations'
OUTPUT = ROOT / 'docs/generated/clark-log-qh-object-map.json'

abort 'DBREAL04 failed: immutable source dump is missing' unless SOURCE.file?
abort 'DBREAL04 failed: source dump must be read-only' unless (SOURCE.stat.mode & 0o222).zero?

sql = SOURCE.read
blocks = sql.scan(/CREATE TABLE\s+`([^`]+)`\s*\((.*?)\)\s*ENGINE\s*=\s*([^\s;]+)/im)
abort 'DBREAL04 failed: no source tables parsed' if blocks.empty?

canonical_sql = MIGRATIONS.children.select { |p| p.extname == '.sql' }.sort.map(&:read).join("\n")
canonical_tables = canonical_sql.scan(/CREATE TABLE(?: IF NOT EXISTS)?\s+([a-zA-Z0-9_]+)/i).flatten.uniq.sort
required_targets = %w[aoo_admin_audit aoo_ledger aoo_room_event aoo_settlement perspective_replay_event aoo_data_lifecycle_policy]
missing_targets = required_targets.reject { |name| canonical_tables.include?(name) }
abort "DBREAL04 failed: canonical targets missing: #{missing_targets.join(', ')}" unless missing_targets.empty?

def classification(name)
  case name
  when /login|online|exception|examine|maintain|change.?city|player.?data/i
    ['SECURITY_AND_OPERATOR_AUDIT', 'aoo_admin_audit', 'security and operator evidence']
  when /charge|card|gold|score|sports.?point|award|rebate|profit|prize.?pool|xi.?pai/i
    ['ASSET_AND_LEDGER', 'aoo_ledger', 'immutable asset movement ledger']
  when /room|game|match/i
    ['ROOM_AND_GAME_EVENT', 'aoo_room_event', 'authoritative room event journal']
  when /record|result|settle/i
    ['SETTLEMENT_AND_REPLAY', 'aoo_settlement', 'settlement plus perspective replay']
  else
    ['REVIEWED_ARCHIVE_ONLY', nil, 'legacy report or aggregate; immutable reference only']
  end
end

rows = blocks.map do |name, body, engine|
  category, target, rationale = classification(name)
  indexes = body.scan(/(?:PRIMARY KEY|UNIQUE KEY\s+`([^`]+)`|KEY\s+`([^`]+)`)/i).map do |pair|
    pair.compact.first || 'PRIMARY'
  end.uniq.sort
  {
    'legacyTable' => name,
    'category' => category,
    'canonicalTarget' => target,
    'disposition' => target ? 'MAPPED_TO_CANONICAL_MODEL' : 'REVIEWED_ARCHIVE_ONLY',
    'legacyIndexCount' => indexes.length,
    'legacyIndexes' => indexes,
    'legacyEngine' => engine,
    'retentionAuthority' => target ? 'aoo_data_lifecycle_policy' : 'database/original immutable reference policy',
    'rationale' => rationale
  }
end

target_counts = rows.each_with_object(Hash.new(0)) { |row, out| out[row['canonicalTarget'] || 'archive-only'] += 1 }
ledger = {
  'schemaVersion' => 1,
  'task' => 'DBREAL04',
  'source' => SOURCE.relative_path_from(ROOT).to_s,
  'sourceReadOnly' => true,
  'sourceDatabaseBrand' => 'clark_log_qh (legacy immutable reference only)',
  'canonicalBrandRule' => 'runtime database objects use aoo_* or explicitly approved canonical names',
  'legacyTableCount' => rows.length,
  'mappedTableCount' => rows.count { |row| row['canonicalTarget'] },
  'reviewedArchiveOnlyCount' => rows.count { |row| row['canonicalTarget'].nil? },
  'canonicalTargets' => required_targets,
  'canonicalTargetCounts' => target_counts.sort.to_h,
  'retention' => {
    'runtimePolicyTable' => 'aoo_data_lifecycle_policy',
    'replayMigration' => 'V20260822_09__replay_retention.sql',
    'legacyDumpRuntimeReachable' => false,
    'rule' => 'runtime rows follow policy-table retention; original dump remains read-only and release-excluded'
  },
  'tables' => rows.sort_by { |row| row['legacyTable'].downcase }
}

OUTPUT.dirname.mkpath
OUTPUT.write(JSON.pretty_generate(ledger) + "\n")
abort 'DBREAL04 failed: table conservation mismatch' unless ledger['legacyTableCount'] == 225
abort 'DBREAL04 failed: an object lacks disposition' unless rows.all? { |row| row['disposition'] && row['retentionAuthority'] }
puts "DBREAL04 passed: #{rows.length} legacy log tables mapped (#{ledger['mappedTableCount']} canonical, #{ledger['reviewedArchiveOnlyCount']} archive-only)"
