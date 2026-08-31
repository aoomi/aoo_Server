#!/usr/bin/env ruby
# frozen_string_literal: true
require 'json'; require 'fileutils'; require 'open3'
root = File.expand_path('..', __dir__); jdk = File.expand_path('../.toolchains/jdk-26.0.2.1.jdk/Contents/Home', root)
stdout, stderr, status = Open3.capture3({ 'JAVA_HOME' => jdk, 'PATH' => "#{jdk}/bin:#{ENV['PATH']}" }, './mvnw', '-q', '-pl', 'server/Gateway', '-am', '-Dtest=WebSocketRequestGuardTest', '-Dsurefire.failIfNoSpecifiedTests=false', 'test', chdir: root)
session = File.read(File.join(root, 'server/Gateway/src/main/java/com/aoo/bcg/gateway/ConnectionSession.java'))
jdbc = File.read(File.join(root, 'server/Gateway/src/main/java/com/aoo/bcg/gateway/JdbcConnectionGenerationStore.java'))
presence = File.read(File.join(root, 'server/Gateway/src/main/java/com/aoo/bcg/gateway/ConnectionPresenceRegistry.java'))
checks = { session_carries_generation: session.include?('long generation'), generation_survives_sequence_accept: session.include?('connectionId, generation'),
  durable_allocator: jdbc.include?('FOR UPDATE') && jdbc.include?('aoo_connection_generation'), stale_close_guard: presence.include?('current.generation() != session.generation()'),
  migration_present: File.file?(File.join(root, 'database/migrations/V20260823_10__connection_generation.sql')), tests_passed: status.success? }
result = { task: 'SEAT09', passed: checks.values.all?, checks: checks, testStdout: stdout.strip, testStderr: stderr.strip }
out = File.join(root, 'work/audit/connection-generation.json'); FileUtils.mkdir_p(File.dirname(out)); File.write(out, JSON.pretty_generate(result) + "\n")
puts JSON.generate(result); exit(result[:passed] ? 0 : 1)
