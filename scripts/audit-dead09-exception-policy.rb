require 'json';require 'fileutils'
root=File.expand_path('..',__dir__);files=Dir.glob(File.join(root,'server/**/src/main/java/com/aoo/**/*.java')).reject{|path|path.include?('/test/')||path.include?('/target/')};findings=[];catches=0
files.each do |path|
 text=File.binread(path).force_encoding('UTF-8').scrub
 text.to_enum(:scan,/catch\s*\(([^)]*(?:Exception|Throwable)[^)]*)\)\s*\{([^{}]{0,1600})\}/m).each do
  match=Regexp.last_match;catches+=1;body=match[2].gsub(%r{//.*$},'').gsub(%r{/\*.*?\*/}m,'').strip;line=text[0...match.begin(0)].count("\n")+1
  observable=body.match?(/\bthrow\b|\b(?:rollback|fail|completeExceptionally|recordFailure|addSuppressed|shutdownNow)\s*\(|\b(?:log|logger|LOG)\w*\.(?:error|warn|info)\s*\(|System\.getLogger/)
  documented=match[2].match?(/exception-policy|best.?effort|optional cleanup|cleanup failure|fallback|intentionally|invalid identity|unauthenticated sentinel|protection wins|scheduler must survive/i)
  documented ||= match[1].match?(/\b(?:ignored|invalid\w*|unknown|duplicate)\b/i)
  documented ||= match[1].match?(/SQLIntegrityConstraintViolationException/) && body.empty?
  documented ||= match[1].match?(/IllegalArgumentException|SecurityException/) && body.match?(/return\s+(?:false|Optional\.empty\(\))\s*;/)
  default_return=body.match?(/return\s+(?:null|false|true|0L?|-1|List\.of\(\)|Map\.of\(\)|Set\.of\(\)|Optional\.empty\(\))\s*;/)
  findings<<{file:path.sub(root+'/',''),line:line,reason:'EMPTY_CATCH'} if body.empty?&&!documented
  findings<<{file:path.sub(root+'/',''),line:line,reason:'SILENT_DEFAULT_FALLBACK'} if default_return&&!observable&&!documented
 end
end
retry_source=File.read(File.join(root,'server/GameCommon/src/main/java/com/aoo/bcg/common/retry/ControlledRetry.java'));checks={catch_inventory:catches>0,no_swallowed_or_silent_fallback:findings.empty?,single_retry_policy:%w[maxAttempts maximumElapsed jitterRatio idempotencyKey].all?{|token|retry_source.include?(token)}};out=File.join(root,'work/audit/dead09-exception-policy.json');FileUtils.mkdir_p(File.dirname(out));File.write(out,JSON.pretty_generate({task:'DEAD09',status:checks.values.all? ? 'passed':'failed',checks:checks,catchBlocks:catches,findings:findings,faultInjectionEvidence:['work/audit/controlled-retry.json','server/GameCommon/src/test/java/com/aoo/bcg/common/retry/ControlledRetryTest.java']})+"\n");abort "DEAD09 exception policy failed: #{findings.size}" unless checks.values.all?
