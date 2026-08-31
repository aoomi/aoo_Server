#!/usr/bin/env ruby
require 'json'
require 'open3'
root = File.expand_path('..', __dir__)
admission = File.read(File.join(root, 'server/GameCommon/src/main/java/com/aoo/bcg/common/perspective/SpectatorAdmission.java'))
backlog = File.read(File.join(root, 'server/GameCommon/src/main/java/com/aoo/bcg/common/event/OutboxBacklog.java'))
idempotency = File.read(File.join(root, 'server/GameCommon/src/main/java/com/aoo/bcg/common/idempotency/InMemoryIdempotencyStore.java'))
java_home = File.expand_path('../.toolchains/jdk-26.0.2.1.jdk/Contents/Home', root)
env = {'JAVA_HOME' => java_home, 'PATH' => "#{java_home}/bin:#{ENV['PATH']}"}
stdout, stderr, status = Open3.capture3(env, './mvnw', '-pl', 'server/GameCommon', '-am', 'test', '-q', chdir: root)
checks = {
  'seat_absence_is_optional_int' => admission.include?('OptionalInt requestedSeatId'),
  'backlog_absence_is_optional' => backlog.include?('Optional<Instant> oldestCreatedAt'),
  'processing_result_is_optional' => idempotency.include?('Optional<IdempotencyResult<R>> value'),
  'common_tests_passed' => status.success?
}
evidence = {'task' => 'TYPE02', 'passed' => checks.values.all?, 'checks' => checks,
            'buildStdout' => stdout, 'buildStderr' => stderr}
audit = File.join(root, 'work/audit'); Dir.mkdir(File.join(root, 'work')) unless Dir.exist?(File.join(root, 'work')); Dir.mkdir(audit) unless Dir.exist?(audit)
File.write(File.join(audit, 'optional-state-semantics.json'), JSON.pretty_generate(evidence))
puts JSON.generate(evidence); exit(evidence['passed'] ? 0 : 1)
