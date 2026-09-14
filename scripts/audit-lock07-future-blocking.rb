require 'json';require 'fileutils'
root=File.expand_path('..',__dir__); findings=[]
Dir.glob(File.join(root,'server/**/*.{java,kt}')).reject{|p|p.include?('/test/')||p.include?('/build/')||p.include?('/target/')}.each do|path|
 s=File.read(path,encoding:'UTF-8',invalid: :replace,undef: :replace)
 # A submitted future synchronously joined before the enclosing method returns can exhaust a bounded pool.
 direct=s.match?(/(?:submit|supplyAsync|runAsync)\s*\([^;]{0,600}?\)\s*\.(?:get|join)\s*\(/m)
 assigned=s.scan(/(?:Future(?:<[^;=]+>)?|CompletableFuture<[^;=]+>|var)\s+(\w+)\s*=\s*[^;]*(?:submit|supplyAsync|runAsync)\s*\([^;]*;/m).flatten.any?{|name|s.match?(/\b#{Regexp.escape(name)}\s*\.(?:get|join)\s*\(/)}
 findings<<path.delete_prefix(root+'/') if direct||assigned
end
findings.uniq!
checks={static_detection:true,no_submit_then_wait:findings.empty?,bounded_pool_exhaustion_test:File.exist?(File.join(root,'server/GameCommon/src/test/java/com/aoo/bcg/common/concurrency/FutureBlockingGuardTest.java'))}
out={task:'LOCK07',passed:checks.values.all?,checks:checks,findings:findings.take(100),findingCount:findings.size};FileUtils.mkdir_p(File.join(root,'work/audit'));File.write(File.join(root,'work/audit/lock07-future-blocking.json'),JSON.pretty_generate(out));puts JSON.generate(out);abort('LOCK07 failed')unless out[:passed]
