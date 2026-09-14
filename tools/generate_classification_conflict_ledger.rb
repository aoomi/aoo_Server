#!/usr/bin/env ruby
# frozen_string_literal: true

require 'json'
require 'time'

root = File.expand_path('..', __dir__)
primary = JSON.parse(File.read(File.join(root, 'work/audit/primary-game-category-audit.json'), encoding: 'UTF-8'))
semantics = JSON.parse(File.read(File.join(root, 'work/audit/game-family-semantic-evidence.json'), encoding: 'UTF-8'))
regional = JSON.parse(File.read(File.join(root, 'work/audit/regional-variant-classification.json'), encoding: 'UTF-8'))
registration = JSON.parse(File.read(File.join(root, 'work/audit/registration-conservation-audit.json'), encoding: 'UTF-8'))
output_path = File.join(root, 'work/audit/classification-conflict-ledger.json')

primary_by_code = primary.fetch('rows').reject { |row| row['infrastructure'] }.to_h { |row| [row.fetch('code'), row] }
semantic_by_code = semantics.fetch('games').to_h { |row| [row.fetch('code'), row] }
regional_games = regional.fetch('families').flat_map { |family| family.fetch('members') }
regional_by_code = regional_games.to_h { |row| [row.fetch('code'), row] }
registration_by_code = registration.fetch('rows').to_h { |row| [row.fetch('code'), row] }

rows = primary_by_code.keys.sort.map do |code|
  category = primary_by_code.fetch(code)
  semantic = semantic_by_code.fetch(code, {})
  region = regional_by_code.fetch(code, {})
  registered = registration_by_code.fetch(code, {})
  conflicts = []
  heuristic = category.fetch('heuristicAdditionalCategories', [])
  conflicts << {type: 'CATEGORY_HEURISTIC_CONFLICT', values: heuristic} unless heuristic.empty? || heuristic.include?(category['declaredCategory'])
  conflicts << {type: 'FAMILY_SEMANTICS_INCOMPLETE'} unless semantic['semanticComplete']
  conflicts << {type: 'NO_DIRECT_GAME_SOURCE'} if semantic.fetch('directSourceFiles', 0).zero?
  conflicts << {type: 'REGION_MISSING'} if region['region'].nil? || region['region'].to_s.empty?
  conflicts << {type: 'DATABASE_REGISTRATION_MISSING'} unless registered['databaseRegistered']
  conflicts << {type: 'NATIVE_PROVIDER_MISSING'} unless registered['nativeProviderRegistered']
  conflicts << {type: 'ROUTE_REGISTRATION_MISSING'} unless registered['routeRegistered']
  conflicts << {type: 'CLIENT_COMPONENT_REGISTRATION_MISSING'} unless registered['clientComponentRegistered']
  {
    code: code, category: category['declaredCategory'], family: semantic['declaredFamily'], region: region['region'],
    conflicts: conflicts, conflictFree: conflicts.empty?
  }
end

type_counts = rows.flat_map { |row| row[:conflicts].map { |conflict| conflict[:type] } }.group_by(&:itself).transform_values(&:size).sort.to_h
summary = {
  gameCount: rows.size,
  conflictFree: rows.count { |row| row[:conflictFree] },
  conflictedGames: rows.count { |row| !row[:conflictFree] },
  conflictCount: rows.sum { |row| row[:conflicts].size },
  conflictTypeCounts: type_counts,
  duplicateCodes: rows.group_by { |row| row[:code] }.count { |_code, grouped| grouped.size > 1 }
}
report = {
  schemaVersion: 1, generatedAt: Time.now.utc.iso8601, summary: summary,
  invariant: 'Every gameplay code has exactly one category, one semantics-proven family, one valid region assignment and one closed registration chain.',
  games: rows
}
File.write(output_path, JSON.pretty_generate(report) + "\n", mode: 'w:UTF-8')

puts JSON.generate(summary)
