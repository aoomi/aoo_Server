#!/usr/bin/env ruby
require 'json';require 'fileutils';root=File.expand_path('..',__dir__)
t=File.read(File.join(root,'server/GameCommon/src/test/java/com/aoo/bcg/common/loop/CpuRecoveryFaultInjectionTest.java'))
checks={'abnormal_work'=>t.include?('RuntimeWorkBudget'),'dependency_cycle'=>t.include?('AcyclicDependencyGraph'),'duplicate_message'=>t.include?('CausalDispatchGuard'),
 'fault_retry'=>t.include?('ControlledRetry'),'time_bound'=>t.include?('assertTimeoutPreemptively'),'worker_recovery'=>t.include?('worker-recovered')}
r={'task'=>'LOOP16','passed'=>checks.values.all?,'checks'=>checks,'test'=>'CpuRecoveryFaultInjectionTest'};out=File.join(root,'work/audit/loop16-cpu-recovery.json');FileUtils.mkdir_p(File.dirname(out));File.write(out,JSON.pretty_generate(r)+"\n");puts JSON.generate(r);exit(r['passed'] ? 0 : 1)
