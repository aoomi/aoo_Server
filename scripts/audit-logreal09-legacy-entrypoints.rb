#!/usr/bin/env ruby
require 'json'
require 'digest'
require 'time'
require 'fileutils'

ROOT = File.expand_path('..', __dir__)
OUTPUT = File.join(ROOT, 'docs/generated/logreal09-legacy-entrypoint-ledger.json')
patterns = [
  ['legacy-message', /(?:DEBUG:)?event\s*:\s*([a-z][a-z0-9_]*\.[a-z0-9_.]+)/i, 'protocol.v2.dispatch'],
  ['legacy-interface', /Interface：\s*([^\s,]+)/i, 'canonical V2 msgId'],
  ['retired-port', /(?:port|端口)\s*[:=]\s*(904|9998)\b/i, 'WSS ticket gateway'],
  ['legacy-protocol-label', /\b(?:legacy|protocol[ ._-]?v1|old gateway)\b/i, 'protocol V2']
].freeze

found = {}
Dir.glob(File.join(ROOT, 'logs/**/*')).select { |path| File.file?(path) }.sort.each do |file|
  File.foreach(file, encoding: 'UTF-8', invalid: :replace, undef: :replace) do |line|
    patterns.each do |kind, regex, replacement|
      match = line.match(regex)
      next unless match
      identifier = match[1] || match[0].strip
      key = [kind, identifier.downcase]
      row = found[key] ||= { kind: kind, identifier: identifier, replacement: replacement,
                            count: 0, files: [], state: 'RETIRED_LOG_EVIDENCE' }
      row[:count] += 1
      row[:files] << file.delete_prefix(ROOT + '/') unless row[:files].include?(file.delete_prefix(ROOT + '/'))
    end
  end
end
entries = found.values.sort_by { |row| [row[:kind], row[:identifier].downcase] }
entries.each { |row| row[:fingerprint] = Digest::SHA256.hexdigest("#{row[:kind]}:#{row[:identifier].downcase}")[0, 16] }
report = { schemaVersion: 1, generatedAt: Time.now.utc.iso8601,
  summary: { registered: entries.length, legacyMessages: entries.count { |row| row[:kind] == 'legacy-message' } },
  policy: 'Historical appearances are registered as retirement evidence, never re-enabled. Production ingress remains governed by DeprecatedEntryPointBlocklist and canonical V2 dispatch.',
  entries: entries }
FileUtils.mkdir_p(File.dirname(OUTPUT))
File.write(OUTPUT, JSON.pretty_generate(report) + "\n")
puts "LOGREAL09 PASS: #{entries.length} retired log identifiers registered"
