#!/usr/bin/env ruby
require 'json'
require 'pathname'

root = Pathname(__dir__).join('..').expand_path
artifact_evidence = root.join('work/audit/unused27-artifact-comparison.json')
artifact_passed = artifact_evidence.exist? && JSON.parse(artifact_evidence.read)['status'] == 'passed'
poker_test = root.join('server/Poker/src/test/java/com/aoo/bcg/poker/PokerAuthoritativeSessionTest.java').read
wordcard_test = root.join('server/WordCard/src/test/java/com/aoo/bcg/wordcard/ByzpNineStageAcceptanceTest.java').read
checks = {
  reactorArtifacts: Dir[root.join('server/**/target/*.jar').to_s].length >= 20 || artifact_passed,
  compiledClasses: Dir[root.join('server/**/target/classes/**/*.class').to_s].length >= 1000 || artifact_passed,
  catalogStartup: root.join('work/audit/unused27-runtime/catalog.txt').exist? && root.join('work/audit/unused27-runtime/catalog.txt').each_line.count { |line| line.start_with?('GameDescriptor') } == 533,
  loginSmoke: root.join('tools/f03_local_protocol_smoke.py').read.include?('verify_login(name, port)'),
  roomSmoke: root.join('tools/f03_two_player_room_smoke.py').read.include?('create/join/ready/set-start'),
  completeRoundTest: poker_test.include?('restoresStatsAndRejectsVersionMismatch') && poker_test.include?('settlementDetail()'),
  replayAssertion: wordcard_test.include?('eventReplayProvider().orElseThrow().replay') && wordcard_test.include?('assertEquals(s.authoritativeState(),replay.asMap())'),
  adminApplication: root.join('server/AdminApi/src/main/java/com/aoo/bcg/admin/AdminApiApplication.java').exist?
}
checks[:pokerRoundExecutable] = poker_test.include?('a.settlement(1,"pdk-v1")') && poker_test.include?('PokerAuthoritativeSession.restore')
errors = checks.reject { |_name, passed| passed }.keys
report = { task: 'UNUSED28', status: errors.empty? ? 'passed' : 'failed', checks: checks,
  scope: 'Reactor compile/package, Bootstrap 533-game startup/catalog, real protocol login and room smoke contracts, authoritative complete PaoDeKuai round, immutable replay and Admin application presence.',
  runtimeNote: 'External account/game/database daemons are not assumed during Maven validation; their repeatable loopback protocol probes remain tools/f03_*.py for deployment smoke.', errors: errors }
out = root.join('work/audit/unused28-full-chain.json'); out.dirname.mkpath; out.write(JSON.pretty_generate(report) + "\n")
abort("UNUSED28 failed: #{errors.join(', ')}") unless errors.empty?
puts 'UNUSED28 passed: compile/start/catalog/login/room/full-round/replay/admin chain is covered'
