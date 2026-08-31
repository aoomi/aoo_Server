#!/usr/bin/env ruby
require 'json';require 'fileutils'
root=File.expand_path('..',__dir__); roots=[File.join(root,'server'),File.expand_path('../Client/assets/Common/Code/Runtime/CompatibilityApp',root)]
files=roots.flat_map{|d|Dir.glob(File.join(d,'**/*.{java,ts}'))}.reject{|f|f.include?('/build/')||f.include?('/target/')}
spin=files.select{|f|File.read(f).include?('onSpinWait')}
unsafe=spin.reject{|f|s=File.read(f);s.match?(/for\s*\([^;]*;[^;]*(?:<|deadline|budget)/)||s.include?('MAX_CLOCK_WAIT_NANOS')}
id=File.read(File.join(root,'server/GameCommon/src/main/java/com/aoo/bcg/common/id/DistributedIdGenerator.java'))
checks={'spin_sites_bounded'=>unsafe.empty?,'id_wait_blocks'=>id.include?('LockSupport.parkNanos'),'id_wait_deadline'=>id.include?('MAX_CLOCK_WAIT_NANOS'),
 'worker_queues_block'=>File.read(File.join(root,'server/AooKernel/src/main/java/BaseTask/SyncTask/SyncTaskQueue.java')).include?('acquireUninterruptibly')}
r={'task'=>'LOOP11','passed'=>checks.values.all?,'checks'=>checks,'spinSites'=>spin.map{|f|f.delete_prefix(root+'/')},'unsafe'=>unsafe}
out=File.join(root,'work/audit/loop11-busy-wait.json');FileUtils.mkdir_p(File.dirname(out));File.write(out,JSON.pretty_generate(r)+"\n");puts JSON.generate(r);exit(r['passed'] ? 0 : 1)
