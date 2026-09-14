#!/usr/bin/env ruby
require 'json'
require 'find'
require 'digest'
require 'fileutils'

root = File.expand_path('..', __dir__)
client = File.expand_path('../Client', root)
assets = File.join(client, 'assets')
extensions = %w[.prefab .scene .png .jpg .jpeg .webp .json .ts .mp3 .wav .ogg]
records = []
Find.find(assets) do |path|
  next unless File.file?(path)
  next unless extensions.include?(File.extname(path).downcase)
  rel = path.delete_prefix(client + '/')
  marker = rel[/\b(?:Portrait|Landscape|Native)\b/i]
  next unless marker
  normalized = rel.gsub(/\b(?:Portrait|Landscape|Native)\b/i, '{variant}')
  category = case File.extname(path).downcase
             when '.prefab', '.scene' then 'layout'
             when '.ts' then 'interaction-or-business'
             else 'resource'
             end
  records << {path: rel, variant: marker.downcase, normalizedPath: normalized, category: category, bytes: File.size(path), sha256: Digest::SHA256.file(path).hexdigest}
end
groups = records.group_by { |row| row[:normalizedPath] }.map do |key, rows|
  {normalizedPath: key, variants: rows.map { |row| row[:variant] }.uniq.sort, categories: rows.map { |row| row[:category] }.uniq.sort, files: rows.map { |row| row[:path] }}
end
report = {
  schemaVersion: 1,
  records: records,
  groups: groups,
  summary: records.group_by { |row| row[:category] }.transform_values(&:length),
  classificationComplete: records.all? { |row| %w[layout resource interaction-or-business].include?(row[:category]) },
  pairedGroups: groups.count { |group| group[:variants].length > 1 },
  singleVariantGroups: groups.count { |group| group[:variants].length == 1 }
}
out = File.join(root, 'docs/generated/layout01-variant-classification.json')
FileUtils.mkdir_p(File.dirname(out))
File.write(out, JSON.pretty_generate(report) + "\n")
abort 'LAYOUT01 failed: unclassified variant asset' unless report[:classificationComplete]
puts "LAYOUT01 PASS: #{records.length} variant assets classified into layout/resource/interaction-business"
