#!/usr/bin/env ruby
require 'json'
require 'open3'
root = File.expand_path('..', __dir__)
paths = %w[server/GameSPI/src/main/java server/GameCommon/src/main/java server/Mahjong/src/main/java server/Poker/src/main/java server/Gateway/src/main/java server/Bootstrap/src/main/java server/ZYPK/src/business server/ZJH/src/business]
files = paths.flat_map { |path| Dir.glob(File.join(root, path, '**/*.java')) }
enum_names = files.flat_map { |file| File.read(file).scan(/\benum\s+(\w+)/).flatten }.uniq
direct = []
files.each do |file|
  source = File.read(file)
  enum_names.each { |name| direct << "#{file.sub(root + '/', '')}:#{name}" if source.include?("#{name}.valueOf(") }
end
direct.reject! { |entry| entry.start_with?('server/GameSPI/src/main/java/com/aoo/bcg/gamespi/StrictEnumDecoder.java:') }
java_home = File.expand_path('../.toolchains/jdk-26.0.2.1.jdk/Contents/Home', root)
env = {'JAVA_HOME' => java_home, 'PATH' => "#{java_home}/bin:#{ENV['PATH']}"}
stdout, stderr, status = Open3.capture3(env, './mvnw', '-pl', 'server/Bootstrap,server/ZYPK,server/Mahjong', '-am', 'test', '-q', chdir: root)
checks = {'no_direct_enum_value_of_at_unified_boundaries' => direct.empty?, 'strict_decoder_tests_passed' => status.success?}
evidence = {'task' => 'TYPE07', 'passed' => checks.values.all?, 'checks' => checks,
            'directDecoders' => direct, 'buildStdout' => stdout, 'buildStderr' => stderr}
audit = File.join(root, 'work/audit'); Dir.mkdir(File.join(root, 'work')) unless Dir.exist?(File.join(root, 'work')); Dir.mkdir(audit) unless Dir.exist?(audit)
File.write(File.join(audit, 'strict-enum-decoding.json'), JSON.pretty_generate(evidence))
puts JSON.generate(evidence); exit(evidence['passed'] ? 0 : 1)
