#!/usr/bin/env ruby
require 'json'; require 'fileutils'
root = File.expand_path('..', __dir__)
catalog = File.read(File.join(root, 'server/GameCommon/src/main/java/com/aoo/bcg/common/retry/RetryPolicyCatalog.java'))
controlled = File.read(File.join(root, 'server/GameCommon/src/main/java/com/aoo/bcg/common/retry/ControlledRetry.java'))
outbox = File.read(File.join(root, 'server/GameCommon/src/main/java/com/aoo/bcg/common/event/JdbcOutboxRepository.java'))
domains = %w[HTTP DATABASE MQ CONFIG DOWNLOAD COMPENSATION]
checks = {
  'all_domains' => domains.all? { |d| catalog.include?("Domain.#{d}") },
  'bounded_attempts' => controlled.include?('maxAttempts') && catalog.include?('attempts'),
  'exponential_backoff' => controlled.include?('1L<<'),
  'jitter' => controlled.include?('jitterRatio') && catalog.include?('0.20'),
  'elapsed_budget' => controlled.include?('maximumElapsed'),
  'mq_dead_letter' => catalog.include?('FailureDisposition.DEAD_LETTER') && outbox.include?("'DEAD'"),
  'compensation_failure_ledger' => catalog.include?('FailureDisposition.FAILURE_LEDGER')
}
result = {'task'=>'LOOP07', 'passed'=>checks.values.all?, 'checks'=>checks,
          'policySource'=>'RetryPolicyCatalog', 'executionBoundary'=>'ControlledRetry'}
path = File.join(root, 'work/audit/loop07-retries.json'); FileUtils.mkdir_p(File.dirname(path))
File.write(path, JSON.pretty_generate(result) + "\n"); puts JSON.generate(result)
exit(result['passed'] ? 0 : 1)
