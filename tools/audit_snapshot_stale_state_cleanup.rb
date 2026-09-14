#!/usr/bin/env ruby
# frozen_string_literal: true

require 'json'
require 'time'
require 'fileutils'

root = File.expand_path('..', __dir__)
client = File.expand_path('../Client/assets', root)
output = File.join(root, 'work/audit/snapshot-stale-state-cleanup.json')
domains = {
  players: /(?:player|seat|pos)/i,
  cards: /(?:card|hand|tile|poker)/i,
  candidates: /(?:candidate|operation|action|prompt)/i,
  bubbles: /(?:bubble|voice|chat|message)/i,
  popups: /(?:popup|dialog|form)/i
}.freeze

rows = []
Dir.glob(File.join(client, '**/*.ts')).each do |path|
  text = File.read(path, encoding: 'UTF-8', invalid: :replace, undef: :replace)
  next unless text.match?(/(?:Snapshot|snapshot|Reconnect|reconnect|GetRoomInfo)/)
  relative = path.delete_prefix(client + '/')
  cleanup = text.match?(/(?:\.clear\s*\(|\.destroy\s*\(|reset\w*\s*\(|removeAllChildren|replaceSnapshot)/)
  covered = domains.keys.select { |domain| text.match?(domains.fetch(domain)) }
  rows << {path: relative, cleanupOperation: cleanup, coveredDomains: covered,
           allStaleDomainsCovered: cleanup && covered.size == domains.size}
end
closed = rows.count { |row| row[:allStaleDomainsCovered] }
summary = {snapshotConsumers: rows.size, fullCleanupConsumers: closed,
           incompleteConsumers: rows.size - closed,
           domainCoverage: domains.keys.to_h { |domain| [domain, rows.count { |row| row[:coveredDomains].include?(domain) }] },
           passed: rows.any? && closed == rows.size}
report = {schemaVersion: 1, generatedAt: Time.now.utc.iso8601,
          invariant: 'Applying a full player-view snapshot first removes stale players, cards, candidates, chat/voice bubbles and popups.',
          summary: summary, consumers: rows}
FileUtils.mkdir_p(File.dirname(output))
File.write(output, JSON.pretty_generate(report) + "\n", mode: 'w:UTF-8')
puts JSON.generate(summary)
exit(summary[:passed] ? 0 : 2) if ENV['AOO_ENFORCE_SNAPSHOT_CLEANUP_GATE'] == '1'
