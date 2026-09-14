#!/usr/bin/env ruby
# frozen_string_literal: true
require 'json'; require 'fileutils'; require 'open3'
root = File.expand_path('..', __dir__); client = File.expand_path('../Client', root); jdk = File.expand_path('../.toolchains/jdk-26.0.2.1.jdk/Contents/Home', root)
mout, merr, mstatus = Open3.capture3({ 'JAVA_HOME' => jdk, 'PATH' => "#{jdk}/bin:#{ENV['PATH']}" }, './mvnw', '-q', '-pl', 'server/GameCommon', '-am', '-Dtest=PlayerExitCleanupCoordinatorTest', '-Dsurefire.failIfNoSpecifiedTests=false', 'test', chdir: root)
nout, nerr, nstatus = Open3.capture3('node', 'tests/unit/room-exit-cleanup.test.mjs', chdir: client)
server_source = File.read(File.join(root, 'server/GameCommon/src/main/java/com/aoo/bcg/common/room/PlayerExitCleanupCoordinator.java'))
server_steps = %w[connection seat candidates timers chatTargets clientMappingPush]; client_steps = %w[closeConnectionScope clearRoomStore clearSceneBuffer clearOperationCandidates cancelTimers clearChatTargets clearSeatMapping]
client_source = File.read(File.join(client, 'assets/Common/Code/Runtime/state/RoomExitCleanup.ts'))
checks = { server_steps_complete: server_steps.all? { |name| server_source.include?(name) }, client_steps_complete: client_steps.all? { |name| client_source.include?(name) },
  server_retry_test: mstatus.success?, client_all_steps_test: nstatus.success? }
result = { task: 'SEAT11', passed: checks.values.all?, checks: checks, serverTest: mout.strip, serverError: merr.strip, clientTest: nout.strip, clientError: nerr.strip }
out = File.join(root, 'work/audit/player-exit-cleanup.json'); FileUtils.mkdir_p(File.dirname(out)); File.write(out, JSON.pretty_generate(result) + "\n")
puts JSON.generate(result); exit(result[:passed] ? 0 : 1)
