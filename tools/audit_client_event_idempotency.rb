#!/usr/bin/env ruby
# frozen_string_literal: true

require 'json'
require 'time'
require 'fileutils'

root = File.expand_path('..', __dir__)
client = File.expand_path('../Client/assets', root)
output = File.join(root, 'work/audit/client-event-idempotency.json')
rows = []
Dir.glob(File.join(client, '**/*.ts')).each do |path|
  text = File.read(path, encoding: 'UTF-8', invalid: :replace, undef: :replace)
  next unless text.match?(/(?:onEvent|onMessage|OnMessage|registerPush|\.on\s*\()/)
  has_effect = text.match?(/(?:Animation|Tween|tween\(|\.play\(|instantiate\(|show\()/)
  next unless has_effect
  rows << {path: path.delete_prefix(client + '/'), eventSeqCheck: text.match?(/eventSeq|lastSeq|sequence/),
           effectDeduplicator: text.include?('EventEffectDeduplicator'),
           roomScope: text.match?(/roomId|roomID/)}
end
closed = rows.count { |row| row[:eventSeqCheck] && row[:effectDeduplicator] && row[:roomScope] }
summary = {effectConsumers: rows.size, idempotentConsumers: closed, missingIdempotency: rows.size - closed,
           deduplicatorPresent: File.file?(File.join(client, 'Common/Code/Runtime/state/EventEffectDeduplicator.ts')),
           passed: rows.any? && closed == rows.size}
report = {schemaVersion: 1, generatedAt: Time.now.utc.iso8601,
          invariant: 'Every event-derived UI effect is keyed by roomId, eventSeq and effect key and runs at most once.',
          summary: summary, consumers: rows}
FileUtils.mkdir_p(File.dirname(output))
File.write(output, JSON.pretty_generate(report) + "\n", mode: 'w:UTF-8')
puts JSON.generate(summary)
exit(summary[:passed] ? 0 : 2) if ENV['AOO_ENFORCE_CLIENT_EVENT_IDEMPOTENCY_GATE'] == '1'
