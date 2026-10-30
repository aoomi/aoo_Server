#!/usr/bin/env ruby
# frozen_string_literal: true

require 'json'

root = File.expand_path('../../..', __dir__)
path = File.join(root, 'release-input', 'identity', 'room-rule-play-identities.json')
catalog = JSON.parse(File.read(path, encoding: 'UTF-8'))
plays = catalog.fetch('plays')

expected = {
  'quanguo2998' => 5,
  'quanguo2997' => 9,
  'chengdu2999' => 2999
}

actual = expected.to_h do |game_code, game_id|
  entry = plays.find { |play| play['gameCode'] == game_code }
  abort("missing room-rule identity: #{game_code}") unless entry
  abort("#{game_code} is not canonical gameId=#{game_id}") unless entry['gameId'] == game_id
  [game_code, entry['gameId']]
end

abort('legacy specialized gameIds remain in the active room-rule registry') if plays.any? do |play|
  expected.key?(play['gameCode']) && [2997, 2998].include?(play['gameId'])
end

abort('historical gameId=630 must not be active for chengdu2999') if plays.any? do |play|
  play['gameCode'] == 'chengdu2999' && play['gameId'] == 630
end

neijiang = plays.find { |play| play['gameCode'] == 'neijiang2002' }
abort('missing canonical Neijiang identity') unless neijiang&.fetch('gameId', nil) == 2002

current_pdk_codes = (2001..2028).map { |game_id| plays.find { |play| play['gameId'] == game_id } }
abort('current PDK identity count must be 28') unless current_pdk_codes.compact.length == 28
current_pdk_codes.each do |play|
  expected_version = "#{play.fetch('gameCode')}-v1.0.0"
  abort("#{play['gameCode']} must use #{expected_version}") unless play['playVersion'] == expected_version
  abort("#{play['gameCode']} cannot use a compatibility playVersion") if play['playVersion'].start_with?('legacy-')
end

manifest_path = File.join(root, 'server', 'GameSPI', 'src', 'main', 'resources', 'current-game-identities.tsv')
manifest_lines = File.readlines(manifest_path, chomp: true, encoding: 'UTF-8')
abort('current identity manifest header is invalid') unless manifest_lines.shift == "gameId\tgameCode\tplayVersion\tfamily"
manifest = manifest_lines.reject { |line| line.empty? || line.start_with?('#') }.map do |line|
  game_id, game_code, play_version, family = line.split("\t", -1)
  abort('current identity manifest row is invalid') unless [game_id, game_code, play_version, family].all? { |value| value && !value.empty? }
  { 'gameId' => Integer(game_id), 'gameCode' => game_code, 'playVersion' => play_version, 'family' => family }
end
abort('current identity manifest must contain 32 rows') unless manifest.length == 32
source_by_id = plays.to_h { |play| [play.fetch('gameId'), play] }
manifest.each do |entry|
  source = source_by_id[entry['gameId']]
  abort("current identity manifest source missing #{entry['gameId']}") unless source
  %w[gameCode playVersion family].each do |field|
    abort("current identity manifest drift #{entry['gameId']} #{field}") unless source[field] == entry[field]
  end
end

puts JSON.generate(ok: true, specialized: actual, neijiang2002: neijiang['gameId'])
