#!/usr/bin/env ruby
# frozen_string_literal: true
require 'json';require 'fileutils';require 'time'
root=File.expand_path('..',__dir__)
publisher=File.read(File.join(root,'server/GameCommon/src/main/java/com/aoo/bcg/common/event/RocketMqOutboxPublisher.java'),encoding:'UTF-8')
consumer=File.read(File.join(root,'server/GameCommon/src/main/java/com/aoo/bcg/common/event/RocketMqOutboxConsumer.java'),encoding:'UTF-8')
event=File.read(File.join(root,'server/GameCommon/src/main/java/com/aoo/bcg/common/event/OutboxEvent.java'),encoding:'UTF-8')
checks={aggregateIdentity:event.match?(/aggregateType.*aggregateId/),partitionKey:publisher.match?(/aggregateType\(\).*aggregateId\(\)/),queueSelector:publisher.include?('MessageQueueSelector'),stableHash:publisher.include?('Math.floorMod'),orderlyListener:consumer.include?('MessageListenerOrderly'),queueScopedRetry:consumer.include?('SUSPEND_CURRENT_QUEUE_A_MOMENT'),persistentDedup:consumer.include?('consumedEvents.claim')}
report={generatedAt:Time.now.utc.iso8601,invariant:'all events of one aggregate use one stable broker partition and are consumed sequentially with persistent deduplication',checks:checks,passed:checks.values.all?};output=File.join(root,'work/audit/aggregate-partition-ordering.json');FileUtils.mkdir_p(File.dirname(output));File.write(output,JSON.pretty_generate(report)+"\n");puts JSON.generate(checks:checks.length,passedChecks:checks.values.count(true),passed:report[:passed]);exit(report[:passed] ? 0 : 1)
