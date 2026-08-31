#!/usr/bin/env ruby
# frozen_string_literal: true

require 'json'
require 'time'
require 'fileutils'

root = File.expand_path('..', __dir__)
client = File.expand_path('../Client/assets', root)
output = File.join(root, 'work/audit/operation-ui-invariant.json')
rows = []
Dir.glob(File.join(client, '**/*.ts')).each do |path|
  text = File.read(path, encoding: 'UTF-8', invalid: :replace, undef: :replace)
  next unless text.match?(/(?:opPos|operationSeat|currentTurn)/i) && text.match?(/(?:Button|interactable|\.active\s*=|enabled)/)
  rows << {path: path.delete_prefix(client + '/'), invariantCall: text.include?('assertOperationUi'),
           eventSeq: text.match?(/eventSeq|sequence/), allowedActions: text.match?(/allowedActions|candidate|operations/),
           telemetry: text.match?(/(?:traceId|telemetry|metric|console\.error)/)}
end
closed = rows.count { |row| row[:invariantCall] && row[:eventSeq] && row[:allowedActions] && row[:telemetry] }
summary = {operationUiFiles: rows.size, assertedFiles: closed, missingAssertion: rows.size - closed,
           foundationPresent: File.file?(File.join(client, 'Common/Code/Runtime/state/OperationUiInvariant.ts')),
           passed: rows.any? && closed == rows.size}
report = {schemaVersion: 1, generatedAt: Time.now.utc.iso8601,
          invariant: 'Enabled operation buttons exactly equal server allowed actions when and only when operationSeatId equals localSeatId.',
          summary: summary, files: rows}
FileUtils.mkdir_p(File.dirname(output))
File.write(output, JSON.pretty_generate(report) + "\n", mode: 'w:UTF-8')
puts JSON.generate(summary)
exit(summary[:passed] ? 0 : 2) if ENV['AOO_ENFORCE_OPERATION_UI_GATE'] == '1'
