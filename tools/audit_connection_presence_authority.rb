#!/usr/bin/env ruby
# frozen_string_literal: true
require 'json'; require 'fileutils'; require 'open3'
root = File.expand_path('..', __dir__); jdk = File.expand_path('../.toolchains/jdk-26.0.2.1.jdk/Contents/Home', root)
stdout, stderr, status = Open3.capture3({ 'JAVA_HOME' => jdk, 'PATH' => "#{jdk}/bin:#{ENV['PATH']}" }, './mvnw', '-q', '-pl', 'server/Gateway', '-am', '-Dtest=WebSocketRequestGuardTest', '-Dsurefire.failIfNoSpecifiedTests=false', 'test', chdir: root)
source = File.read(File.join(root, 'server/Gateway/src/main/java/com/aoo/bcg/gateway/ConnectionPresenceRegistry.java'))
checks = { connection_context_keyed: source.include?('Key(String userId, String roomId, int seatId)'), online_on_connect: source.include?('Status.ONLINE'),
  offline_on_matching_disconnect: source.include?('Status.OFFLINE') && source.include?('current.connectionId().equals(session.connectionId())'),
  stale_callback_rejected: source.include?('session.generation() < current.generation()'), tests_passed: status.success? }
result = { task: 'SEAT08', passed: checks.values.all?, checks: checks, legacyPolicy: 'legacy online flags excluded from new protocol authority', testStdout: stdout.strip, testStderr: stderr.strip }
out = File.join(root, 'work/audit/connection-presence-authority.json'); FileUtils.mkdir_p(File.dirname(out)); File.write(out, JSON.pretty_generate(result) + "\n")
puts JSON.generate(result); exit(result[:passed] ? 0 : 1)
