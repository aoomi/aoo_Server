require 'json';require 'fileutils';require 'digest'
root=File.expand_path('..',__dir__);files=Dir.glob(File.join(root,'server/**/src/test/java/**/*.java')).reject{|path|path.include?('/target/')};findings=[];tests=[];hashes=Hash.new{|h,k|h[k]=[]}
files.each do |path|
 body=File.binread(path).force_encoding('UTF-8').scrub
 findings<<{file:path.sub(root+'/',''),reason:'DISABLED_TEST'} if body.match?(/@Disabled\b|@Ignore\b/)
 positions=[];body.to_enum(:scan,/@(?:Test|ParameterizedTest|RepeatedTest)\b/).each{positions<<Regexp.last_match.begin(0)}
 positions.each_with_index do |position,index|
  segment=body[position...(positions[index+1]||body.length)];declaration=segment.match(/@(?:Test|ParameterizedTest|RepeatedTest)\b(?:\([^)]*\))?\s*(?:public\s+|protected\s+|private\s+)?(?:void|[A-Za-z_$][\w$<>?, ]*)\s+(\w+)\s*\([^)]*\)\s*\{/m);next unless declaration
  name=declaration[1];code=segment;id="#{path.sub(root+'/','')}:#{name}";tests<<id
  effective=code.match?(/\bassert(?:Equals|NotEquals|True|False|Null|NotNull|Same|NotSame|Throws|DoesNotThrow|All|ArrayEquals|IterableEquals|InstanceOf|Timeout|LinesMatch)\s*\(|\bfail\s*\(|\bverify\s*\(/)
  findings<<{test:id,reason:'NO_OBSERVABLE_ASSERTION'} unless effective
  normalized=code.gsub(/\s+/,' ').gsub(/\b\d+\b/,'#').strip;hashes[Digest::SHA256.hexdigest(normalized)]<<id if normalized.length>20
 end
end
hashes.each_value{|ids|findings<<{tests:ids,reason:'DUPLICATE_TEST_BODY'} if ids.size>1};checks={tests_discovered:tests.any?,no_disabled_or_assertionless_or_duplicate:findings.empty?};out=File.join(root,'work/audit/dead13-test-effectiveness.json');FileUtils.mkdir_p(File.dirname(out));File.write(out,JSON.pretty_generate({task:'DEAD13',status:checks.values.all? ? 'passed':'failed',checks:checks,testCount:tests.size,findings:findings})+"\n");abort "DEAD13 ineffective tests: #{findings.size}" unless checks.values.all?
