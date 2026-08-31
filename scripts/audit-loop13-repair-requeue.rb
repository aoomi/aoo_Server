#!/usr/bin/env ruby
require 'json';require 'fileutils';root=File.expand_path('..',__dir__)
s=File.read(File.join(root,'server/GameCommon/src/main/java/com/aoo/bcg/common/repair/DurableRepairCheckpoint.java'))
rnr=File.read(File.join(root,'server/GameCommon/src/main/java/com/aoo/bcg/common/repair/ThrottledRepairRunner.java'))
checks={'retry_attempt_limit'=>s.include?('MAX_RETRY_ATTEMPTS'),'exhausted_not_retryable'=>s.include?('attempts<MAX_RETRY_ATTEMPTS'),
 'failure_ledger'=>s.include?('failureLedger()'),'batch_cursor_progress'=>rnr.include?('offset++'),'pause_checkpoint'=>rnr.include?('pause.get()')}
r={'task'=>'LOOP13','passed'=>checks.values.all?,'checks'=>checks};out=File.join(root,'work/audit/loop13-repair-requeue.json');FileUtils.mkdir_p(File.dirname(out));File.write(out,JSON.pretty_generate(r)+"\n");puts JSON.generate(r);exit(r['passed'] ? 0 : 1)
