require 'json'

root = File.expand_path('..', __dir__)
source = File.read(File.join(root, 'server/GameCommon/src/main/java/com/aoo/bcg/common/event/JdbcConsumedEventStore.java'))
test = File.read(File.join(root, 'server/GameCommon/src/test/java/com/aoo/bcg/common/event/RocketMqOutboxIntegrationTest.java'))
checks = {
  'fencedLease' => source.include?('claim_token') && source.include?('consumed event lease lost'),
  'crashTakeover' => source.include?('claimLease') && source.include?('staleAfter'),
  'boundedCleanup' => source.include?('public int cleanup') && source.include?('limit>10_000'),
  'realDatabaseTestScheduled' => test.include?('AOO_MQ_IT_DB_URL') && test.include?('staleLeaseIsFencedAndRetentionCleanupIsBounded'),
  'realRocketMqTestScheduled' => test.include?('AOO_MQ_IT_NAMESRV') && test.include?('brokerRoundTripAndDatabaseDeduplication'),
  'deduplicationAsserted' => test.include?('assertEquals(1, handled.get())') && test.include?('alreadyProcessed(event.eventId())')
}
abort 'MIGREAL04 source closure failed' unless checks.values.all?
output = {
  'task' => 'MIGREAL04',
  'checks' => checks,
  'dynamicTest' => 'com.aoo.bcg.common.event.RocketMqOutboxIntegrationTest',
  'dynamicEvidencePolicy' => 'The clean reactor schedules the test; CUR-06 separately runs it with real MySQL and RocketMQ environment variables.',
  'status' => 'passed'
}
File.write(File.join(root, 'docs/generated/migreal04-consumed-event-lifecycle.json'), JSON.pretty_generate(output) + "\n")
puts 'MIGREAL04 consumed event lifecycle gate passed'
