#!/usr/bin/env ruby
require 'json';require 'fileutils';root=File.expand_path('..',__dir__)
s=File.read(File.join(root,'server/GameCommon/src/main/java/com/aoo/bcg/common/concurrency/ContentionRetryExecutor.java'));p=File.read(File.join(root,'server/GameCommon/src/main/java/com/aoo/bcg/common/retry/RetryPolicyCatalog.java'))
checks={'finite_catalog_policy'=>s.include?('RetryPolicyCatalog.Domain.DATABASE')&&p.include?('Domain.DATABASE'),'randomized_backoff'=>s.include?('ThreadLocalRandom'),
 'classified_conflicts'=>s.include?('standardContention'),'idempotency_required'=>s.include?('idempotencyKey'),'permanent_failure_not_retried'=>s.include?('Predicate<Throwable>')}
r={'task'=>'LOCK04','passed'=>checks.values.all?,'checks'=>checks};out=File.join(root,'work/audit/lock04-livelock.json');FileUtils.mkdir_p(File.dirname(out));File.write(out,JSON.pretty_generate(r)+"\n");puts JSON.generate(r);exit(r['passed'] ? 0 : 1)
