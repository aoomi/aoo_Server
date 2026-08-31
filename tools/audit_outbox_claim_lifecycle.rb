#!/usr/bin/env ruby
# frozen_string_literal: true
require 'json';require 'fileutils';require 'time'
root=File.expand_path('..',__dir__)
artifacts=%w[database/migrations/V20260823_02__outbox_claim_dlq_lifecycle.sql server/GameCommon/src/main/java/com/aoo/bcg/common/event/JdbcOutboxRepository.java server/GameCommon/src/main/java/com/aoo/bcg/common/event/OutboxRelay.java]
text=artifacts.to_h{|path|[path,File.read(File.join(root,path),encoding:'UTF-8')]}
checks={skipLocked:text.values.any?{|v|v.include?('SKIP LOCKED')},claimLease:text.values.any?{|v|v.include?('locked_until')},ownership:text.values.any?{|v|v.include?('locked_by')},retryLimit:text.values.any?{|v|v.include?('maxAttempts')},deadLetter:text.values.any?{|v|v.include?("'DEAD'")},backoff:text.values.any?{|v|v.include?('next_attempt_at')},cleanup:text.values.any?{|v|v.include?('purgePublishedBefore')},rollback:text.values.any?{|v|v.include?('rollback()')}}
report={generatedAt:Time.now.utc.iso8601,artifacts:artifacts,checks:checks,passed:checks.values.all?};output=File.join(root,'work/audit/outbox-claim-lifecycle.json');FileUtils.mkdir_p(File.dirname(output));File.write(output,JSON.pretty_generate(report)+"\n");puts JSON.generate(checks:checks.length,passedChecks:checks.values.count(true),passed:report[:passed]);exit(report[:passed] ? 0 : 1)
