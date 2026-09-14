#!/usr/bin/env ruby
# frozen_string_literal: true
require 'json'; require 'fileutils'; require 'open3'
root = File.expand_path('..', __dir__); jdk = File.expand_path('../.toolchains/jdk-26.0.2.1.jdk/Contents/Home', root)
modules = %w[GameCommon GameSPI Gateway Bootstrap Mahjong Poker LongCard WordCard]
sources = modules.flat_map { |name| Dir[File.join(root, 'server', name, 'src/main/java/**/*.java')] }
forbidden = /System\.currentTimeMillis\(\)|new Date\(|Instant\.now\(\)/
violations = sources.map do |path|
  lines = File.readlines(path)
  hits = lines.each_with_index.map { |line, index| "#{path.delete_prefix(root + '/')}:#{index + 1}" if line.match?(forbidden) }.compact
  hits
end.flatten
stdout, stderr, status = Open3.capture3({ 'JAVA_HOME' => jdk, 'PATH' => "#{jdk}/bin:#{ENV['PATH']}" }, './mvnw', '-q', '-pl', 'server/GameSPI,server/GameCommon,server/Gateway', '-am', '-Dtest=AuthoritativeTimeSourceTest,RuntimeGameRoomRegistryTest', '-Dsurefire.failIfNoSpecifiedTests=false', 'test', chdir: root)
checks = { direct_wall_clock_isolated: violations.empty?, deterministic_tests_passed: status.success?, authoritative_source_present: File.exist?(File.join(root, 'server/GameSPI/src/main/java/com/aoo/bcg/gamespi/time/AuthoritativeTimeSource.java')), monotonic_metrics_separated: File.exist?(File.join(root, 'server/GameSPI/src/main/java/com/aoo/bcg/gamespi/time/MonotonicTicker.java')) }
result = { task: 'TIME01', passed: checks.values.all?, checks: checks, violations: violations, testStdout: stdout.strip, testStderr: stderr.strip }
out = File.join(root, 'work/audit/authoritative-time-source.json'); FileUtils.mkdir_p(File.dirname(out)); File.write(out, JSON.pretty_generate(result) + "\n")
puts JSON.generate(result); exit(result[:passed] ? 0 : 1)
