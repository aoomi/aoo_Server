#!/usr/bin/env ruby
# frozen_string_literal: true
require 'json'; require 'fileutils'; require 'open3'
root = File.expand_path('..', __dir__); jdk = File.expand_path('../.toolchains/jdk-26.0.2.1.jdk/Contents/Home', root)
stdout, stderr, status = Open3.capture3({ 'JAVA_HOME' => jdk, 'PATH' => "#{jdk}/bin:#{ENV['PATH']}" }, './mvnw', '-q', '-pl', 'server/GameCommon', '-am', '-Dtest=PerspectiveDispatcherTest', '-Dsurefire.failIfNoSpecifiedTests=false', 'test', chdir: root)
source = File.read(File.join(root, 'server/GameCommon/src/main/java/com/aoo/bcg/common/perspective/SpectatorAdmissionCoordinator.java'))
checks = { explicit_states: %w[SPECTATING RESERVED_FOR_NEXT_ROUND PLAYER].all? { |state| Dir.glob(File.join(root, 'server/GameCommon/src/main/java/com/aoo/bcg/common/perspective/*.java')).any? { |path| File.read(path).include?(state) } },
  next_round_only: source.include?('Math.addExact(currentRoundNo, 1)') && source.include?('effectiveRoundNo() != roundNo'),
  authority_assigns_seat: source.include?('room.assign('), perspective_role_cropped: source.include?('ViewerRole.SPECTATOR'), tests_passed: status.success? }
result = { task: 'SEAT10', passed: checks.values.all?, checks: checks, testStdout: stdout.strip, testStderr: stderr.strip }
out = File.join(root, 'work/audit/spectator-promotion.json'); FileUtils.mkdir_p(File.dirname(out)); File.write(out, JSON.pretty_generate(result) + "\n")
puts JSON.generate(result); exit(result[:passed] ? 0 : 1)
