#!/usr/bin/env ruby
# frozen_string_literal: true

require 'json'
require 'time'
require 'fileutils'

root = File.expand_path('..', __dir__)
client = File.expand_path('../Client/assets', root)
output = File.join(root, 'work/audit/scene-message-buffering.json')
rows = []
Dir.glob(File.join(client, '**/*.ts')).each do |path|
  text = File.read(path, encoding: 'UTF-8', invalid: :replace, undef: :replace)
  next unless text.match?(/(?:onMessage|registerPush|\.on\s*\(|queued|queue|buffer)/i)
  relative = path.delete_prefix(client + '/')
  rows << {path: relative, sceneBuffer: text.include?('SceneMessageBuffer'), bounded: text.match?(/MAX_|capacity|\.length\s*[>=]/),
           roomFilter: text.match?(/roomId|roomID/), versionFilter: text.match?(/playVersion/),
           generationFilter: text.match?(/Generation|generation/), sequenceOrdering: text.match?(/eventSeq|lastSeq|sequence/)}
end
closed = rows.count { |row| row[:sceneBuffer] && row[:bounded] && row[:roomFilter] && row[:versionFilter] && row[:generationFilter] && row[:sequenceOrdering] }
summary = {queueOrMessageConsumers: rows.size, fullyScopedBuffers: closed, incomplete: rows.size - closed,
           foundationPresent: File.file?(File.join(client, 'Common/Code/Runtime/state/SceneMessageBuffer.ts')),
           passed: rows.any? && closed == rows.size}
report = {schemaVersion: 1, generatedAt: Time.now.utc.iso8601,
          invariant: 'Messages buffered before scene readiness are bounded, ordered and filtered by room, play version and connection generation.',
          summary: summary, consumers: rows}
FileUtils.mkdir_p(File.dirname(output))
File.write(output, JSON.pretty_generate(report) + "\n", mode: 'w:UTF-8')
puts JSON.generate(summary)
exit(summary[:passed] ? 0 : 2) if ENV['AOO_ENFORCE_SCENE_BUFFER_GATE'] == '1'
