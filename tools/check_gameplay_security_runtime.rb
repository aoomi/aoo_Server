#!/usr/bin/env ruby
# frozen_string_literal: true

require 'json'
require 'open3'
require 'time'

ROOT = File.expand_path('..', __dir__)
REPORT = File.join(ROOT, 'docs/generated/tool06-gameplay-security.json')
STATIC_GATES = %w[
  tools/check_hidden_card_boundaries.rb
  tools/check_game_random_boundaries.rb
  tools/check_gameplay_security_boundaries.rb
].freeze
TESTS = %w[
  PerspectiveDispatcherTest
  SeededGameRandomSourceTest
  PokerAuthoritativeSessionTest
  GameWebSocketRouterTest
  SecureWsTicketServiceTest
].freeze

def run(*command)
  stdout, stderr, status = Open3.capture3(*command, chdir: ROOT)
  { command: command.join(' '), passed: status.success?, exitCode: status.exitstatus,
    stdout: stdout.lines.last(20).join, stderr: stderr.lines.last(20).join }
end

results = STATIC_GATES.map { |gate| run('ruby', gate) }
results << run(
  './mvnw', '-Dexec.skip=true',
  '-pl', 'server/GameCommon,server/Poker,server/Gateway', '-am',
  "-Dtest=#{TESTS.join(',')}", '-Dsurefire.failIfNoSpecifiedTests=false', 'test'
)
passed = results.all? { |result| result[:passed] }
report = {
  schemaVersion: 1,
  generatedAt: Time.now.utc.iso8601,
  task: 'TOOL06',
  passed: passed,
  assurance: {
    staticSourceGates: STATIC_GATES,
    executableTests: TESTS,
    guarantees: [
      'opponent hidden cards are redacted at the authoritative session boundary',
      'private perspective payloads reach only their authenticated owner',
      'server-owned seeded randomness is deterministic and auditable',
      'write requests are idempotent and execute/commit exactly once',
      'forged room/version, reused tickets, wrong Origin and excessive ticket lifetime are rejected'
    ]
  },
  results: results
}
File.write(REPORT, JSON.pretty_generate(report) + "\n")
puts "gameplay-security-runtime: #{passed ? 'passed' : 'failed'}"
exit(passed ? 0 : 1)
