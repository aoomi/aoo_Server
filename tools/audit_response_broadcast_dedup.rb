#!/usr/bin/env ruby
# frozen_string_literal: true

require 'json'
require 'time'
require 'fileutils'

root = File.expand_path('..', __dir__)
client = File.expand_path('../Client/assets', root)
output = File.join(root, 'work/audit/response-broadcast-dedup.json')
rows = []
Dir.glob(File.join(client, '**/*.ts')).each do |path|
  text = File.read(path, encoding: 'UTF-8', invalid: :replace, undef: :replace)
  requests = text.scan(/\.request(?:<[^>]+>)?\s*\(\s*['"]([^'"]+)/).flatten.uniq
  pushes = text.scan(/(?:registerPush|\.on)\s*\(\s*['"]([^'"]+)/).flatten.uniq
  next if requests.empty? || pushes.empty?
  rows << {path: path.delete_prefix(client + '/'), requestActions: requests, pushActions: pushes,
           changeDeduplicator: text.include?('BusinessChangeDeduplicator'),
           requestIdCorrelation: text.match?(/requestId/), eventSeqCorrelation: text.match?(/eventSeq|sequence/)}
end
closed = rows.count { |row| row[:changeDeduplicator] && row[:requestIdCorrelation] && row[:eventSeqCorrelation] }
summary = {dualChannelConsumers: rows.size, deduplicatedConsumers: closed,
           missingDeduplication: rows.size - closed,
           foundationPresent: File.file?(File.join(client, 'Common/Code/Runtime/state/BusinessChangeDeduplicator.ts')),
           passed: rows.any? && closed == rows.size}
report = {schemaVersion: 1, generatedAt: Time.now.utc.iso8601,
          invariant: 'A response and a broadcast for one authoritative change mutate client state exactly once.',
          summary: summary, consumers: rows}
FileUtils.mkdir_p(File.dirname(output))
File.write(output, JSON.pretty_generate(report) + "\n", mode: 'w:UTF-8')
puts JSON.generate(summary)
exit(summary[:passed] ? 0 : 2) if ENV['AOO_ENFORCE_RESPONSE_BROADCAST_DEDUP_GATE'] == '1'
