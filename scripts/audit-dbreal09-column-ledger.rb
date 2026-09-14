#!/usr/bin/env ruby
# frozen_string_literal: true

require 'json'
require 'pathname'

ROOT = Pathname.new(__dir__).parent
OUTPUT = ROOT / 'docs/generated/legacy-database-column-migration-ledger.json'

sources = {
  'db_zle_qh' => ['database/original/db_zle_qh.sql', 'docs/generated/db-zle-qh-object-map.json'],
  'clark_log_qh' => ['database/original/clark_log_qh.sql', 'docs/generated/clark-log-qh-object-map.json'],
  'clark_game_qh' => ['database/original/clark_game_qh.sql', 'docs/generated/clark-game-qh-object-map.json']
}

migration_sql = Dir[(ROOT / 'database/migrations/*.sql').to_s].sort.map { |path| File.read(path) }.join("\n")
canonical_columns = {}
migration_sql.scan(/CREATE TABLE(?: IF NOT EXISTS)?\s+([A-Za-z0-9_]+)\s*\((.*?)\)\s*ENGINE/im).each do |table, body|
  canonical_columns[table.downcase] = body.scan(/^\s*([a-zA-Z][a-zA-Z0-9_]*)\s+/).flatten.reject do |column|
    %w[PRIMARY UNIQUE KEY CONSTRAINT CHECK FOREIGN].include?(column.upcase)
  end
end

aliases = {
  'id' => %w[ledger_id settlement_id audit_id profile_id session_id],
  'userid' => %w[user_id player_id], 'uid' => %w[user_id player_id], 'pid' => %w[player_id],
  'playerid' => %w[player_id], 'roomid' => %w[room_id], 'gameid' => %w[game_id],
  'clubid' => %w[club_id], 'unionid' => %w[club_id], 'setid' => %w[set_id],
  'roundno' => %w[round_no], 'version' => %w[version profile_version play_version template_version],
  'createtime' => %w[created_at], 'createdtime' => %w[created_at], 'updatetime' => %w[updated_at],
  'status' => %w[status member_status], 'balance' => %w[balance balance_after],
  'score' => %w[delta balance], 'value' => %w[delta], 'type' => %w[event_type resource_type scope_type],
  'content' => %w[event_payload result_payload profile_payload state_payload rule_payload payload],
  'data' => %w[event_payload result_payload profile_payload state_payload rule_payload payload]
}

def normalized(name)
  name.gsub(/([a-z0-9])([A-Z])/, '\\1_\\2').downcase.gsub(/[^a-z0-9]/, '')
end

def table_targets(entry)
  value = entry['canonicalTargets'] || entry['canonicalTarget']
  Array(value).compact
end

entries = []
sources.each do |database, (sql_path, map_path)|
  table_map_json = JSON.parse((ROOT / map_path).read)
  table_rows = table_map_json['entries'] || table_map_json['tables']
  table_map = table_rows.each_with_object({}) do |entry, out|
    name = entry['sourceTable'] || entry['legacyTable']
    out[name.downcase] = entry
  end
  sql = (ROOT / sql_path).read
  sql.scan(/CREATE TABLE\s+`([^`]+)`\s*\((.*?)\)\s*ENGINE/im).each do |table, body|
    table_entry = table_map.fetch(table.downcase)
    targets = table_targets(table_entry)
    columns = body.scan(/^\s*`([^`]+)`\s+([^,\n]+)/).map { |name, type| [name, type.strip] }
    columns.each do |column, type|
      if targets.empty?
        entries << {
          'sourceDatabase' => database, 'sourceTable' => table, 'sourceColumn' => column,
          'sourceType' => type, 'targetTable' => nil, 'targetColumn' => nil,
          'disposition' => 'ARCHIVE_OR_SEPARATE_CONTEXT', 'transform' => 'none; source remains immutable reference'
        }
        next
      end
      target = targets.find { |candidate| canonical_columns.key?(candidate.downcase) } || targets.first
      available = canonical_columns.fetch(target.downcase, [])
      norm = normalized(column)
      direct = available.find { |candidate| normalized(candidate) == norm }
      mapped = direct || Array(aliases[norm]).find { |candidate| available.include?(candidate) }
      payload = %w[event_payload result_payload profile_payload state_payload rule_payload payload].find { |candidate| available.include?(candidate) }
      if mapped
        disposition = 'DIRECT_OR_NORMALIZED_COLUMN_MIGRATION'
        target_column = mapped
        transform = mapped == column ? 'identity' : "normalize #{column} to #{mapped}"
      elsif payload
        disposition = 'MERGE_INTO_VERSIONED_PAYLOAD'
        target_column = "#{payload}.legacy.#{column}"
        transform = 'preserve typed legacy value under namespaced JSON key'
      else
        disposition = 'BLOCKED_EXPLICIT_TRANSFORM_REQUIRED'
        target_column = nil
        transform = 'define domain transform before importing this column; never silently discard'
      end
      entries << {
        'sourceDatabase' => database, 'sourceTable' => table, 'sourceColumn' => column,
        'sourceType' => type, 'targetTable' => target, 'targetColumn' => target_column,
        'disposition' => disposition, 'transform' => transform
      }
    end
  end
end

by_disposition = entries.each_with_object(Hash.new(0)) { |entry, out| out[entry['disposition']] += 1 }
report = {
  'schemaVersion' => 1,
  'task' => 'DBREAL09',
  'sourceDatabaseCount' => sources.length,
  'sourceTableCount' => entries.map { |entry| [entry['sourceDatabase'], entry['sourceTable']] }.uniq.length,
  'sourceColumnCount' => entries.length,
  'dispositionCounts' => by_disposition.sort.to_h,
  'policy' => 'every source column is mapped, merged, archived/separated, or explicitly blocked; implicit loss is forbidden',
  'entries' => entries.sort_by { |entry| [entry['sourceDatabase'], entry['sourceTable'].downcase, entry['sourceColumn'].downcase] }
}
OUTPUT.dirname.mkpath
OUTPUT.write(JSON.pretty_generate(report) + "\n")
abort 'DBREAL09 failed: expected 427 source tables' unless report['sourceTableCount'] == 427
abort 'DBREAL09 failed: expected 6261 source columns' unless report['sourceColumnCount'] == 6261
abort 'DBREAL09 failed: incomplete disposition' unless entries.all? { |entry| entry['disposition'] && entry['transform'] }
puts "DBREAL09 passed: #{entries.length} columns across #{report['sourceTableCount']} tables have explicit dispositions"
