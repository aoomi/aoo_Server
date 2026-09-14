#!/usr/bin/env ruby
# frozen_string_literal: true

require 'json'

classification, game_list, output = ARGV
abort 'usage: generate_backend_game_catalog.rb CLASSIFICATION_TSV GAMELIST_JSON OUTPUT_TSV' unless output

lines = File.readlines(classification, encoding: 'UTF-8', chomp: true)
header = lines.shift.split("\t", -1)
rows = lines.map { |line| header.zip(line.split("\t", -1)).to_h }
              .reject { |row| row.fetch('category') == 'INFRASTRUCTURE' }
games = JSON.parse(File.read(game_list, encoding: 'UTF-8')).values
canonical = ->(code) { code.to_s.downcase.gsub('qh', 'aoo') }
by_code = games.to_h { |game| [canonical.call(game.fetch('gameName')), game] }
reserved_ids = by_code.values.map { |game| game.fetch('id').to_i }.to_h { |id| [id, true] }
next_migration_id = 900_001
native_ids = {516 => 'cdxzmj', 629 => 'njpdk', 628 => 'scjymj', 618 => 'xcpdk', 9 => 'zjh', 62 => 'zypk'}

missing_codes = []
catalog = rows.map do |row|
  code = canonical.call(row.fetch('module'))
  game = by_code[code]
  unless game
    missing_codes << code
    next
  end
  if native_ids.key?(game.fetch('id').to_i) && native_ids.fetch(game.fetch('id').to_i) != code
    next_migration_id += 1 while reserved_ids[next_migration_id]
    game = game.merge('id' => next_migration_id, 'isOpen' => 0)
    reserved_ids[next_migration_id] = true
    next_migration_id += 1
  end
  region = game.fetch('region', 'unpublished').to_s
  scope = region == 'all' ? 'NATIONAL' : 'PROVINCE'
  [game.fetch('id'), code, row.fetch('module'), row.fetch('category'), row.fetch('family'), scope,
   scope == 'NATIONAL' ? '' : region, '', '1.0.0', game.fetch('isOpen', 0), code.upcase]
end.compact

abort "unregistered game codes: #{missing_codes.sort.join(',')}" unless missing_codes.empty?

ids = catalog.map(&:first)
codes = catalog.map { |row| row[1] }
abort 'catalog must contain exactly 528 games' unless catalog.size == 528
abort 'duplicate game id in catalog' unless ids.uniq.size == ids.size
abort 'duplicate game code in catalog' unless codes.uniq.size == codes.size
abort 'legacy qh/QH name generated' if catalog.flatten.any? { |value| value.to_s.match?(/qh/i) }

File.open(output, 'w:UTF-8') do |file|
  file.puts %w[gameId code displayName category family regionScope provinceCode cityCode version enabled sourceModule].join("\t")
  catalog.sort_by(&:first).each { |row| file.puts row.join("\t") }
end

counts = catalog.group_by { |row| row[3] }.transform_values(&:size)
expected = {'MAHJONG' => 364, 'POKER' => 150, 'WORD_CARD' => 10, 'LONG_CARD' => 4}
abort "category mismatch: #{counts}" unless counts == expected
warn "games=#{catalog.size} " + counts.sort.map { |key, value| "#{key}=#{value}" }.join(' ')
