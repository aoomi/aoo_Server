#!/usr/bin/env ruby
# frozen_string_literal: true

require 'json'
require 'time'
require 'fileutils'

root = File.expand_path('..', __dir__)
client = File.expand_path('../Client/assets', root)
output = File.join(root, 'work/audit/client-authority-boundary.json')
patterns = {
  legalCandidate: /(?:canPlay|canHu|canGang|canPeng|legal(?:Move|Action)|candidate)/i,
  turnDecision: /(?:nextPlayer|nextSeat|opPos|currentTurn)\s*(?:=|\()/i,
  scoreCalculation: /(?:calculate|compute|calc)(?:Score|Point|Fan|Multiple)/i,
  resultDecision: /(?:isWinner|checkWin|judgeWin|settlementResult)/i,
  randomAuthority: /(?:Math\.random|shuffle\s*\()/
}.freeze
findings = []
Dir.glob(File.join(client, '**/*.ts')).each do |path|
  text = File.read(path, encoding: 'UTF-8', invalid: :replace, undef: :replace)
  relative = path.delete_prefix(client + '/')
  patterns.each do |kind, pattern|
    findings << {kind: kind, path: relative} if text.match?(pattern)
  end
end
counts = patterns.keys.to_h { |kind| [kind, findings.count { |row| row[:kind] == kind }] }
summary = counts.merge(affectedFiles: findings.map { |row| row[:path] }.uniq.size,
                       violations: findings.size, passed: findings.empty?)
report = {schemaVersion: 1, generatedAt: Time.now.utc.iso8601,
          invariant: 'Client computes presentation-only projections; legal actions, turn, score, result and randomness remain server authoritative.',
          summary: summary, candidates: findings,
          reviewRule: 'Each candidate needs proof that it only renders server-provided values; otherwise move it behind the authoritative server command/state boundary.'}
FileUtils.mkdir_p(File.dirname(output))
File.write(output, JSON.pretty_generate(report) + "\n", mode: 'w:UTF-8')
puts JSON.generate(summary)
exit(summary[:passed] ? 0 : 2) if ENV['AOO_ENFORCE_CLIENT_AUTHORITY_GATE'] == '1'
