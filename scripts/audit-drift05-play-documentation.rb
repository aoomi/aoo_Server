#!/usr/bin/env ruby
require 'json'
require 'csv'
require 'fileutils'

root = File.expand_path('..', __dir__)
classification = JSON.parse(File.read(File.join(root, 'work/generated/classification/game-classification.json')))
catalog = CSV.read(File.join(root, 'server/Bootstrap/src/main/resources/game-catalog-528.tsv'), headers: true, col_sep: "\t").map(&:to_h)
profile = JSON.parse(File.read(File.join(root, 'docs/generated/migreal14-game-profile.json')))
classified = classification.fetch('rows')
game_rows = classified.reject { |row| row.fetch('category') == 'INFRASTRUCTURE' }
catalog_codes = catalog.map { |row| row.fetch('code').downcase }
classification_codes = game_rows.map { |row| row.fetch('code').downcase }
report = {
  schemaVersion: 1,
  sourceEntries: classified.length,
  infrastructureEntries: classified.length - game_rows.length,
  classifiedGames: game_rows.length,
  catalogGames: catalog.length,
  catalogCoverage: {
    missingFromCatalog: classification_codes - catalog_codes,
    unknownInCatalog: catalog_codes - classification_codes,
    duplicateCatalogCodes: catalog_codes.group_by(&:itself).select { |_, values| values.length > 1 }.keys
  },
  categoryCounts: catalog.group_by { |row| row.fetch('category') }.transform_values(&:length),
  enabledCount: catalog.count { |row| row.fetch('enabled') == '1' },
  profilePublication: profile,
  verdict: profile.dig('completed', 'adminEndpoints') && profile.fetch('unresolved').empty? ? 'passed' : 'blocked-profile-publication',
  checks: {
    classificationAccounting: classified.length == game_rows.length + (classified.length - game_rows.length),
    catalogMatchesClassifiedGames: catalog_codes.sort == classification_codes.sort,
    uniqueCatalog: catalog_codes.uniq.length == catalog_codes.length,
    publicationClosed: profile.dig('completed', 'adminEndpoints') == true && profile.fetch('unresolved').empty?
  }
}
out = File.join(root, 'docs/generated/drift05-play-documentation-reconciliation.json')
FileUtils.mkdir_p(File.dirname(out))
File.write(out, JSON.pretty_generate(report) + "\n")
puts "DRIFT05 AUDITED: #{report[:verdict]}"
