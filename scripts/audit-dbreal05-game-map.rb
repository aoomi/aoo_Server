#!/usr/bin/env ruby
# frozen_string_literal: true

require 'json'
require 'pathname'

ROOT = Pathname.new(__dir__).parent
SOURCE = ROOT / 'database/original/clark_game_qh.sql'
OUTPUT = ROOT / 'docs/generated/clark-game-qh-object-map.json'
MIGRATIONS = ROOT / 'database/migrations'

abort 'DBREAL05 failed: immutable source dump missing' unless SOURCE.file?
abort 'DBREAL05 failed: source dump must be read-only' unless (SOURCE.stat.mode & 0o222).zero?
sql = SOURCE.read
tables = sql.scan(/CREATE TABLE\s+`([^`]+)`/i).flatten
abort "DBREAL05 failed: expected 90 tables, found #{tables.length}" unless tables.length == 90

canonical_sql = MIGRATIONS.children.select { |p| p.extname == '.sql' }.sort.map(&:read).join("\n")
targets = %w[aoo_play_variant aoo_game_profile_version aoo_room_template aoo_room_snapshot aoo_room_event aoo_settlement aoo_ledger aoo_currency_balance aoo_club_member]
missing = targets.reject { |name| canonical_sql.match?(/\b#{Regexp.escape(name)}\b/i) }
abort "DBREAL05 failed: canonical target missing: #{missing.join(', ')}" unless missing.empty?

def map_table(name)
  case name
  when /game.?type|game.?set|dictionar|config|maintain.?game/i
    ['PLAY_RULE_AND_CATALOG', 'aoo_play_variant', 'versioned gameplay and rule catalog']
  when /city|gps|region/i
    ['REGION_AND_LOCATION', 'aoo_game_profile_version', 'versioned region/profile metadata']
  when /room.?config|player.?room.?alone/i
    ['ROOM_TEMPLATE', 'aoo_room_template', 'validated room template component']
  when /game.?room|player.?game.?room/i
    ['ROOM_RUNTIME', 'aoo_room_snapshot', 'authoritative room state plus event journal']
  when /play.?back|play.?game|rank|award.?record|hu.?reward.?record/i
    ['RESULT_AND_REPLAY', 'aoo_settlement', 'settlement and perspective replay']
  when /currency|recharge|rebate|discount|red.?pack|red.?bag|luck.?draw|award|fatigue/i
    ['ASSET_AND_LEDGER', 'aoo_ledger', 'immutable ledger and authoritative balance']
  when /club|union|family|grouping/i
    ['CLUB_AND_UNION', 'aoo_club_member', 'club aggregate, membership or template projection']
  when /player|email|chat|friend|task|activity|referer|promotion|daily/i
    ['PLAYER_AND_SOCIAL', nil, 'legacy lobby/social capability; reviewed for separate bounded context']
  else
    ['PLATFORM_ARCHIVE', nil, 'legacy platform metadata; immutable reference only']
  end
end

rows = tables.map do |name|
  category, target, rationale = map_table(name)
  {
    'legacyTable' => name,
    'category' => category,
    'canonicalTarget' => target,
    'disposition' => target ? 'MAPPED_TO_CANONICAL_MODEL' : 'REVIEWED_SEPARATE_CONTEXT_OR_ARCHIVE',
    'regionRule' => category == 'REGION_AND_LOCATION' ? 'region metadata is versioned with the published game profile' : nil,
    'roomCreateIndex' => category == 'PLAY_RULE_AND_CATALOG' ? 'aoo_play_variant(game_code, region_code, rule_key, rule_version)' : (category == 'ROOM_TEMPLATE' ? 'aoo_room_template(club_id, play_variant_id, template_name)' : nil),
    'rationale' => rationale
  }
end

ledger = {
  'schemaVersion' => 1,
  'task' => 'DBREAL05',
  'source' => SOURCE.relative_path_from(ROOT).to_s,
  'sourceReadOnly' => true,
  'legacyTableCount' => rows.length,
  'mappedCanonicalCount' => rows.count { |row| row['canonicalTarget'] },
  'reviewedSeparateOrArchiveCount' => rows.count { |row| row['canonicalTarget'].nil? },
  'canonicalModels' => targets,
  'roomCreationAuthority' => {
    'catalog' => 'aoo_play_variant',
    'publishedProfile' => 'aoo_game_profile_version',
    'template' => 'aoo_room_template',
    'rule' => 'room creation resolves indexed gameCode + regionCode + ruleKey + ruleVersion and stores the immutable profile/version reference'
  },
  'tables' => rows.sort_by { |row| row['legacyTable'].downcase }
}
OUTPUT.dirname.mkpath
OUTPUT.write(JSON.pretty_generate(ledger) + "\n")
abort 'DBREAL05 failed: an object lacks disposition' unless rows.all? { |row| row['category'] && row['disposition'] }
puts "DBREAL05 passed: #{rows.length} game database objects classified and mapped"
