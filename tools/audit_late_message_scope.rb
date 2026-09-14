#!/usr/bin/env ruby
# frozen_string_literal: true

require 'json'
require 'time'
require 'fileutils'

root = File.expand_path('..', __dir__)
client = File.expand_path('../Client/assets', root)
output = File.join(root, 'work/audit/late-message-scope.json')
rows = []
Dir.glob(File.join(client, '**/*.ts')).each do |path|
  text = File.read(path, encoding: 'UTF-8', invalid: :replace, undef: :replace)
  next unless text.match?(/(?:onmessage|onMessage|registerPush|\.on\s*\()/)
  rows << {path: path.delete_prefix(client + '/'), scopeGuard: text.include?('RoomMessageScope'),
           generationGuard: text.match?(/Generation|generation/), roomGuard: text.match?(/roomId|roomID/),
           versionGuard: text.match?(/playVersion/), lifecycleClose: text.match?(/(?:onDestroy|close|dispose)/)}
end
closed = rows.count { |row| row[:scopeGuard] && row[:generationGuard] && row[:roomGuard] && row[:versionGuard] && row[:lifecycleClose] }
summary = {messageConsumers: rows.size, fullyScopedConsumers: closed, unscopedConsumers: rows.size - closed,
           foundationPresent: File.file?(File.join(client, 'Common/Code/Runtime/state/RoomMessageScope.ts')),
           passed: rows.any? && closed == rows.size}
report = {schemaVersion: 1, generatedAt: Time.now.utc.iso8601,
          invariant: 'Every callback from an old connection, room or play version is discarded before state and UI side effects.',
          summary: summary, consumers: rows}
FileUtils.mkdir_p(File.dirname(output))
File.write(output, JSON.pretty_generate(report) + "\n", mode: 'w:UTF-8')
puts JSON.generate(summary)
exit(summary[:passed] ? 0 : 2) if ENV['AOO_ENFORCE_LATE_MESSAGE_SCOPE_GATE'] == '1'
