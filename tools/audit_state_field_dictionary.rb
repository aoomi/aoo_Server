#!/usr/bin/env ruby
# frozen_string_literal: true

require 'json'
require 'time'
require 'fileutils'

server_root = File.expand_path('..', __dir__)
client_root = File.expand_path('../Client/assets', server_root)
output = File.join(server_root, 'work/audit/state-field-dictionary.json')

rows = []
Dir.glob(File.join(client_root, '**/*.ts')).each do |path|
  next if path.include?('/temp/') || path.include?('/library/')
  lines = File.readlines(path, encoding: 'UTF-8', invalid: :replace, undef: :replace)
  current = nil
  depth = 0
  lines.each_with_index do |raw, index|
    line = raw.encode('UTF-8', invalid: :replace, undef: :replace, replace: '')
    if (match = line.match(/export\s+interface\s+(\w*(?:State|Snapshot|View|DTO)\w*)\s*\{/))
      current = match[1]
      depth = 1
      next
    end
    next unless current
    depth += line.count('{') - line.count('}')
    if (field = line.match(/^\s*(\w+)\??:\s*([^;]+);/))
      rows << {side: 'client', type: current, field: field[1], declaredType: field[2].strip,
               path: path.delete_prefix(client_root + '/'), line: index + 1,
               visibilityDocumented: line.include?('@visibility'), sourceEventDocumented: line.include?('@source'),
               serializationDocumented: line.include?('@serialization')}
    end
    current = nil if depth <= 0
  end
end

Dir.glob(File.join(server_root, 'server/**/*.java')).each do |path|
  next if path.include?('/target/') || path.include?('/build/')
  text = File.read(path, encoding: 'UTF-8', invalid: :replace, undef: :replace)
  text.scan(/record\s+(\w*(?:State|Snapshot|View|DTO)\w*)\s*\(([^)]*)\)/m).each do |name, fields|
    fields.split(',').each do |declaration|
      match = declaration.strip.match(/(?:[\w<>?,.\[\] ]+)\s+(\w+)$/)
      next unless match
      rows << {side: 'server', type: name, field: match[1], declaredType: declaration.strip,
               path: path.delete_prefix(server_root + '/'), visibilityDocumented: text.include?('@visibility'),
               sourceEventDocumented: text.include?('@source'), serializationDocumented: text.include?('@serialization')}
    end
  end
end

complete = rows.count { |row| row[:visibilityDocumented] && row[:sourceEventDocumented] && row[:serializationDocumented] }
summary = {fields: rows.size, clientFields: rows.count { |row| row[:side] == 'client' },
           serverFields: rows.count { |row| row[:side] == 'server' }, fullyDocumented: complete,
           missingMetadata: rows.size - complete, passed: rows.any? && complete == rows.size}
report = {schemaVersion: 1, generatedAt: Time.now.utc.iso8601,
          invariant: 'Every synchronized state field declares viewer visibility, authoritative source event and serialization rule.',
          summary: summary, fields: rows}
FileUtils.mkdir_p(File.dirname(output))
File.write(output, JSON.pretty_generate(report) + "\n", mode: 'w:UTF-8')
puts JSON.generate(summary)
exit(summary[:passed] ? 0 : 2) if ENV['AOO_ENFORCE_STATE_DICTIONARY_GATE'] == '1'
