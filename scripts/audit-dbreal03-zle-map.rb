#!/usr/bin/env ruby
require 'digest'
require 'fileutils'
require 'json'

root = File.expand_path('..', __dir__)
source = File.join(root, 'database/original/db_zle_qh.sql')
text = File.binread(source).force_encoding(Encoding::UTF_8).scrub
tables = text.scan(/CREATE\s+TABLE\s+(?:IF\s+NOT\s+EXISTS\s+)?[\x60"]?([^\x60"\s(]+)/i).flatten.uniq.sort
classify = lambda do |table|
  case table
  when /(?:gametype|hutype|gamesettings|public_gamelist|category|area)/i
    ['MAPPED_GAME_PROFILE', %w[aoo_game_profile_version aoo_play_variant], %w[database/migrations/V20260822_03__game_profile_publication.sql]]
  when /(?:club|family|guild)/i
    ['MAPPED_CLUB_AND_TEMPLATE', %w[aoo_club_member aoo_room_template], %w[database/migrations/V20260822_01__business_closure.sql database/migrations/V20260822_14__scalable_club_members.sql]]
  when /(?:roomcard|recharge|money|tixian|caiwu|fencheng|goods|drawback|wxnotify|pay)/i
    ['MAPPED_LEDGER_AND_BALANCE', %w[aoo_ledger aoo_currency_balance], %w[database/migrations/V20260822_01__business_closure.sql database/migrations/V20260822_04_2__currency_balance.sql]]
  when /(?:admin|employee|agents|agency|vice_president|top_agents|onelevel_agents)/i
    ['MAPPED_ADMIN_CONTROL', %w[aoo_admin_audit admin_operator_permission admin_control_resource], %w[database/migrations/V20260822_02__admin_control_plane.sql database/migrations/V20260822_04__admin_permissions.sql]]
  when /(?:account|player|online|login|disable)/i
    ['MAPPED_ACCOUNT_SESSION', %w[aoo_session], %w[database/migrations/V20260822_01__business_closure.sql database/migrations/V20260822_15__account_login_index.sql]]
  else
    ['REVIEWED_ARCHIVE_ONLY', [], []]
  end
end
entries = tables.map do |table|
  disposition, targets, migrations = classify.call(table)
  {
    sourceTable: table, disposition: disposition, canonicalTargets: targets, migrationEvidence: migrations,
    archiveReason: disposition == 'REVIEWED_ARCHIVE_ONLY' ? 'legacy CMS/reporting/support object is outside authoritative login-hall-club-room-game-billing runtime; retained for offline historical lookup' : nil
  }
end
ledger = {
  schemaVersion: 1, task: 'DBREAL03', source: 'database/original/db_zle_qh.sql',
  sourceSha256: Digest::SHA256.file(source).hexdigest,
  immutableMode: format('%04o', File.stat(source).mode & 0o777), entries: entries
}
ledger_path = File.join(root, 'docs/generated/db-zle-qh-object-map.json')
FileUtils.mkdir_p(File.dirname(ledger_path))
File.write(ledger_path, JSON.pretty_generate(ledger) + "\n")
checks = {
  all_source_tables_extracted: tables.length == 112,
  every_table_dispositioned: entries.all? { |entry| !entry[:disposition].empty? },
  mapped_tables_have_existing_migrations: entries.reject { |entry| entry[:disposition] == 'REVIEWED_ARCHIVE_ONLY' }.all? { |entry| !entry[:canonicalTargets].empty? && entry[:migrationEvidence].all? { |path| File.file?(File.join(root, path)) } },
  archive_tables_have_reason: entries.select { |entry| entry[:disposition] == 'REVIEWED_ARCHIVE_ONLY' }.all? { |entry| !entry[:archiveReason].empty? },
  source_is_read_only: (File.stat(source).mode & 0o222).zero?,
  source_runtime_and_release_isolated: File.read(File.join(root, '.releaseignore')).include?('database/original/')
}
counts = entries.group_by { |entry| entry[:disposition] }.transform_values(&:length)
result = { task: 'DBREAL03', passed: checks.values.all?, checks: checks, tableCount: tables.length, dispositionCounts: counts, ledger: 'docs/generated/db-zle-qh-object-map.json' }
out = File.join(root, 'work/audit/dbreal03-zle-map.json')
FileUtils.mkdir_p(File.dirname(out))
File.write(out, JSON.pretty_generate(result) + "\n")
puts "DBREAL03 #{result[:passed] ? 'passed' : 'failed'}: #{tables.length} db_zle_qh tables dispositioned"
exit(result[:passed] ? 0 : 1)
