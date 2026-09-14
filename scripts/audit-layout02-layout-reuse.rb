#!/usr/bin/env ruby
require 'json'
require 'fileutils'

root = File.expand_path('..', __dir__)
source = JSON.parse(File.read(File.join(root, 'docs/generated/layout01-variant-classification.json')))
client = File.expand_path('../Client', root)
layout_groups = source.fetch('groups').select { |group| group.fetch('categories').include?('layout') }
native_layouts = source.fetch('records').select { |row| row['category'] == 'layout' && row['variant'] == 'native' }
native_pairs = native_layouts.map do |row|
  counterpart = row['path'].gsub('/Native/', '/')
  {native: row['path'], counterpart: counterpart}
end.select { |row| File.file?(File.join(client, row[:counterpart])) }
duplicated_structures = layout_groups.select { |group| group.fetch('variants').length > 1 } + native_pairs
report = {
  schemaVersion: 1,
  layoutGroups: layout_groups.length,
  multiVariantLayoutGroups: duplicated_structures.length,
  duplicatedStructures: duplicated_structures,
  acceptedReuseMechanisms: %w[Widget Layout SafeArea layout-data],
  replacementProofComplete: false,
  passed: duplicated_structures.empty?
}
out = File.join(root, 'docs/generated/layout02-layout-reuse.json')
FileUtils.mkdir_p(File.dirname(out))
File.write(out, JSON.pretty_generate(report) + "\n")
abort "LAYOUT02 blocked: #{duplicated_structures.length} layout groups retain complete variants without reuse proof" unless report[:passed]
