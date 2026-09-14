#!/usr/bin/env ruby
# frozen_string_literal: true
require 'json'; require 'fileutils'; require 'open3'
root = File.expand_path('..', __dir__)
stdout, stderr, status = Open3.capture3({ 'JAVA_HOME' => File.expand_path('../.toolchains/jdk-26.0.2.1.jdk/Contents/Home', root),
  'PATH' => "#{File.expand_path('../.toolchains/jdk-26.0.2.1.jdk/Contents/Home/bin', root)}:#{ENV['PATH']}" },
  './mvnw', '-q', '-pl', 'server/GameCommon', '-am', '-Dtest=AuthoritativeRoomTest', '-Dsurefire.failIfNoSpecifiedTests=false', 'test', chdir: root)
source = File.read(File.join(root, 'server/GameCommon/src/main/java/com/aoo/bcg/common/room/RoomOwnership.java'))
checks = { disconnect_stable: source.include?('OWNER_OFFLINE'), explicit_transfer: source.include?('void transfer('),
  explicit_leave_policy: source.include?('ownerLeaving('), durable_restore: source.include?('RoomOwnership restore('),
  dissolve_terminal: source.include?('DISSOLVED'), tests_passed: status.success? }
result = { task: 'SEAT05', passed: checks.values.all?, checks: checks, testStdout: stdout.strip, testStderr: stderr.strip }
out = File.join(root, 'work/audit/room-owner-lifecycle.json'); FileUtils.mkdir_p(File.dirname(out)); File.write(out, JSON.pretty_generate(result) + "\n")
puts JSON.generate(result); exit(result[:passed] ? 0 : 1)
