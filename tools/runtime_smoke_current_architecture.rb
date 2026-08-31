#!/usr/bin/env ruby
# frozen_string_literal: true

require 'json'
require 'open3'
require 'time'

ROOT = File.expand_path('..', __dir__)
REPORT = File.join(ROOT, 'docs/generated/tool07-runtime-smoke.json')
TESTS = %w[
  BootstrapAPPTest
  GameProviderServiceLoaderTest
  CatalogLifecycleContractTest
  RuntimeGameRoomRegistryTest
  PokerAuthoritativeSessionTest
  MahjongAuthoritativeSessionTest
].freeze

command = [
  './mvnw', '-Dexec.skip=true',
  '-pl', 'server/Bootstrap,server/Gateway,server/Poker,server/Mahjong', '-am',
  "-Dtest=#{TESTS.join(',')}", '-Dsurefire.failIfNoSpecifiedTests=false', 'test'
]
stdout, stderr, status = Open3.capture3(*command, chdir: ROOT)
test_summary = stdout.lines.grep(/Tests run:|BUILD (?:SUCCESS|FAILURE)/).map(&:strip)
passed = status.success? && test_summary.any? { |line| line.include?('BUILD SUCCESS') }
report = {
  schemaVersion: 1,
  generatedAt: Time.now.utc.iso8601,
  task: 'TOOL07',
  passed: passed,
  entrypoint: 'com.aoo.bcg.bootstrap.BootstrapAPP/loadRegistry',
  scope: [
    'current Bootstrap provider discovery and 533-game catalog',
    'production ServiceLoader provider assembly without duplicate registrations',
    'runtime room creation/resolution lifecycle',
    'authoritative poker and mahjong command execution with state assertions'
  ],
  exclusions: [
    'legacy build/*.jar class-loading scripts',
    'fixed success output',
    'external production infrastructure'
  ],
  command: command.join(' '),
  exitCode: status.exitstatus,
  testSummary: test_summary,
  stdoutTail: stdout.lines.last(40).join,
  stderrTail: stderr.lines.last(40).join
}
File.write(REPORT, JSON.pretty_generate(report) + "\n")
puts "runtime-smoke-current-architecture: #{passed ? 'passed' : 'failed'}"
exit(passed ? 0 : 1)
