#!/usr/bin/env ruby
# frozen_string_literal: true
require 'json'; require 'fileutils'; require 'open3'
root = File.expand_path('..', __dir__); jdk = File.expand_path('../.toolchains/jdk-26.0.2.1.jdk/Contents/Home', root)
stdout, stderr, status = Open3.capture3({ 'JAVA_HOME' => jdk, 'PATH' => "#{jdk}/bin:#{ENV['PATH']}" }, './mvnw', '-q', '-pl', 'server/GameCommon', '-am', '-Dtest=AuthoritativeRoomTest', '-Dsurefire.failIfNoSpecifiedTests=false', 'test', chdir: root)
source = File.read(File.join(root, 'server/GameCommon/src/main/java/com/aoo/bcg/common/room/ReadyStateRegistry.java'))
events = %w[JOINED LEFT ROUND_STARTED ROUND_SETTLED RECONNECTED ROOM_DISSOLVED TABLE_SWITCHED]
event_source = File.read(File.join(root, 'server/GameCommon/src/main/java/com/aoo/bcg/common/room/ReadyLifecycleEvent.java'))
checks = { lifecycle_events_complete: events.all? { |event| event_source.include?(event) }, reconnect_preserves_authority: source.include?('case RECONNECTED'),
  global_boundaries_clear: source.include?('ROUND_STARTED, ROUND_SETTLED, ROOM_DISSOLVED'), seat_boundaries_clear: source.include?('JOINED, LEFT, TABLE_SWITCHED'), tests_passed: status.success? }
result = { task: 'SEAT07', passed: checks.values.all?, checks: checks, testStdout: stdout.strip, testStderr: stderr.strip }
out = File.join(root, 'work/audit/ready-lifecycle-contract.json'); FileUtils.mkdir_p(File.dirname(out)); File.write(out, JSON.pretty_generate(result) + "\n")
puts JSON.generate(result); exit(result[:passed] ? 0 : 1)
