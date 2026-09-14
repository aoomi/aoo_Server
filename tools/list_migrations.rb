#!/usr/bin/env ruby
# frozen_string_literal: true

require 'pathname'

root = Pathname.new(__dir__).parent
directory = root / 'database/migrations'
entries = directory.children.select { |path| path.extname == '.sql' }.map do |path|
  match = path.basename.to_s.match(/\AV(\d{8})_(\d{2})(?:_(\d+))?__([a-z0-9_]+)\.sql\z/)
  abort "invalid migration filename: #{path.basename}" unless match
  patch = match[3] ? Integer(match[3], 10) : 0
  [Integer(match[1], 10), Integer(match[2], 10), patch, path]
end

keys = entries.map { |date, sequence, patch, _| [date, sequence, patch] }
duplicates = keys.group_by(&:itself).select { |_, values| values.length > 1 }.keys
abort "duplicate migration versions: #{duplicates.inspect}" unless duplicates.empty?

entries.sort_by { |date, sequence, patch, _| [date, sequence, patch] }.each do |_, _, _, path|
  puts path
end
