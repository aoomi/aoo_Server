#!/usr/bin/env ruby
require 'json'; require 'open3'; require 'fileutils'
root = File.expand_path('..', __dir__)
source = File.read(File.join(root, 'server/GameCommon/src/main/java/com/aoo/bcg/common/cache/CacheRecoveryCoordinator.java'))
jdk=File.expand_path('../.toolchains/jdk-26.0.2.1.jdk/Contents/Home',root)
env = {'JAVA_HOME'=>jdk, 'PATH'=>"#{jdk}/bin:#{ENV['PATH']}"}
stdout, stderr, status = Open3.capture3(env, './mvnw', '-pl', 'server/GameCommon', '-am', '-Dtest=CacheRecoveryCoordinatorTest', '-Dsurefire.failIfNoSpecifiedTests=false', 'test', '-q', chdir: root)
checks = {
  'per_domain_concurrency' => source.include?('Semaphore'),
  'origin_rate_limit' => source.include?('nextPermitNanos'),
  'circuit_breaker' => source.include?('openUntilNanos'),
  'priority_warmup' => source.include?('Comparator.comparing(WarmupKey::priority)'),
  'jittered_batches' => source.include?('ThreadLocalRandom') && source.include?('batchSize'),
  'tests_passed' => status.success?
}
result = {'task'=>'KEY12', 'passed'=>checks.values.all?, 'checks'=>checks, 'testStdout'=>stdout.strip, 'testStderr'=>stderr.strip}
path = File.join(root, 'work/audit/cache-recovery-protection.json'); FileUtils.mkdir_p(File.dirname(path)); File.write(path, JSON.pretty_generate(result) + "\n")
puts JSON.generate(result); exit(result['passed'] ? 0 : 1)
