#!/usr/bin/env ruby
require 'json'
require 'open3'
root = File.expand_path('..', __dir__)
request = File.read(File.join(root, 'server/GameSPI/src/main/java/com/aoo/bcg/gamespi/GameCommandRequest.java'))
result_source = File.read(File.join(root, 'server/GameSPI/src/main/java/com/aoo/bcg/gamespi/GameCommandResult.java'))
payload = File.read(File.join(root, 'server/GameSPI/src/main/java/com/aoo/bcg/gamespi/CommandPayload.java'))
java_home = File.expand_path('../.toolchains/jdk-26.0.2.1.jdk/Contents/Home', root)
env = {'JAVA_HOME' => java_home, 'PATH' => "#{java_home}/bin:#{ENV['PATH']}"}
stdout, stderr, status = Open3.capture3(env, './mvnw', '-pl', 'server/Bootstrap,server/Gateway,server/ZYPK,server/ZJH', '-am', 'test', '-q', chdir: root)
checks = {
  'request_uses_typed_payload' => request.include?('int seatId, CommandPayload body'),
  'result_uses_typed_payload' => result_source.include?('requestId, CommandPayload body'),
  'strict_typed_accessors' => %w[requireInt requireLong requireString requireIntList].all? { |name| payload.include?(name) },
  'immutable_boundary' => payload.include?('Map.copyOf'),
  'impacted_modules_passed' => status.success?
}
evidence = {'task' => 'TYPE01', 'passed' => checks.values.all?, 'checks' => checks,
            'buildStdout' => stdout, 'buildStderr' => stderr}
audit = File.join(root, 'work/audit'); Dir.mkdir(File.join(root, 'work')) unless Dir.exist?(File.join(root, 'work')); Dir.mkdir(audit) unless Dir.exist?(audit)
File.write(File.join(audit, 'typed-command-payload.json'), JSON.pretty_generate(evidence))
puts JSON.generate(evidence)
exit(evidence['passed'] ? 0 : 1)
