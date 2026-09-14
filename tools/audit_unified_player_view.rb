#!/usr/bin/env ruby
# frozen_string_literal: true
require 'json'; require 'fileutils'; require 'open3'
root = File.expand_path('..', __dir__); jdk = File.expand_path('../.toolchains/jdk-26.0.2.1.jdk/Contents/Home', root)
stdout, stderr, status = Open3.capture3({ 'JAVA_HOME' => jdk, 'PATH' => "#{jdk}/bin:#{ENV['PATH']}" }, './mvnw', '-q', '-pl', 'server/GameSPI', '-am', '-Dtest=PlayerViewDtoTest', '-Dsurefire.failIfNoSpecifiedTests=false', 'test', chdir: root)
source_path = 'server/GameSPI/src/main/java/com/aoo/bcg/gamespi/PlayerViewDto.java'; source = File.read(File.join(root, source_path))
fields = %w[playerId seatId nickname avatarUrl owner dealer online ready trusteeship]
checks = { all_fields_present: fields.all? { |field| source.include?(field) }, immutable_record: source.include?('record PlayerViewDto'),
  shared_map_projection: source.include?('Map<String, Object> toMap()'), tests_passed: status.success? }
result = { task: 'SEAT12', passed: checks.values.all?, checks: checks, dto: source_path, consumers: %w[lobby club room reconnect settlement], testStdout: stdout.strip, testStderr: stderr.strip }
out = File.join(root, 'work/audit/unified-player-view.json'); FileUtils.mkdir_p(File.dirname(out)); File.write(out, JSON.pretty_generate(result) + "\n")
puts JSON.generate(result); exit(result[:passed] ? 0 : 1)
