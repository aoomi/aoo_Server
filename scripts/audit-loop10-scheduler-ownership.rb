#!/usr/bin/env ruby
require 'json'; require 'fileutils'
root=File.expand_path('..',__dir__); client=File.expand_path('../Client',root)
timer_files=Dir.glob(File.join(client,'assets/Common/Code/Runtime/CompatibilityApp/**/*.ts')).select{|f|File.read(f).include?('setInterval')}
uncleared=timer_files.reject{|f|File.read(f).include?('clearInterval')}
relay=File.read(File.join(root,'server/GameCommon/src/main/java/com/aoo/bcg/common/event/ScheduledOutboxRelay.java'))
registry=File.read(File.join(root,'server/GameCommon/src/main/java/com/aoo/bcg/common/time/UniqueScheduledTaskRegistry.java'))
checks={'client_intervals_owned'=>uncleared.empty?,'relay_start_idempotent'=>relay.include?('compareAndSet(false, true)'),
 'unique_owner_registry'=>registry.include?('compute(ownerKey'),'registry_cancellation'=>registry.include?('task.cancel(false)')}
r={'task'=>'LOOP10','passed'=>checks.values.all?,'checks'=>checks,'clientIntervalFiles'=>timer_files.size,'uncleared'=>uncleared}
out=File.join(root,'work/audit/loop10-scheduler-ownership.json');FileUtils.mkdir_p(File.dirname(out));File.write(out,JSON.pretty_generate(r)+"\n");puts JSON.generate(r);exit(r['passed'] ? 0 : 1)
