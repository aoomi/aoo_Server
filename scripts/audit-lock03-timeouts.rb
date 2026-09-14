#!/usr/bin/env ruby
require 'json';require 'fileutils';root=File.expand_path('..',__dir__)
files=Dir.glob(File.join(root,'server/**/*.java')).reject{|f|f.include?('/build/')||f.include?('/target/')||f.include?('/test/')}
plain=[];files.each{|f|File.foreach(f).with_index(1){|line,n|plain<<{'file'=>f.delete_prefix(root+'/'),'line'=>n} if line.match?(/\.lock\s*\(\s*\)\s*;/)&&!f.end_with?('/CosMutex.java')}}
inactive_prefixes=['server/LegacyAccountServer/','server/LegacyCommon/','server/LegacyCommDef/',
 'server/gameServer/','server/AooKernel/']
active=plain.reject{|item|inactive_prefixes.any?{|prefix|item['file'].start_with?(prefix)}}
inactive=plain-active
cos=File.read(File.join(root,'server/AooKernel/src/main/java/BaseThread/CosMutex.java'));redis=File.read(File.join(root,'server/LegacyCommon/src/com/ddm/server/common/redis/DistributedRedisLock.java'))
checks={'kernel_timeout'=>cos.include?('tryLock(timeoutNanos'),'kernel_diagnostics'=>cos.include?('getQueueLength'),'kernel_safe_failure'=>cos.include?('mutex acquisition timed out'),'redis_bounded'=>redis.include?('attempt < 600')}
r={'task'=>'LOCK03','passed'=>checks.values.all?&&active.empty?,'modernized'=>checks.values.all?,
 'activeUntimedResidualCount'=>active.size,'inactiveLegacyResidualCount'=>inactive.size,
 'activeSample'=>active.first(100),'inactiveLegacySample'=>inactive.first(100),'checks'=>checks}
out=File.join(root,'work/audit/lock03-timeouts.json');FileUtils.mkdir_p(File.dirname(out));File.write(out,JSON.pretty_generate(r)+"\n");puts JSON.generate(r)
