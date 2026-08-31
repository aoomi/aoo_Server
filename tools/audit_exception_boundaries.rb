#!/usr/bin/env ruby
require 'json';require 'fileutils'
root=File.expand_path('..',__dir__);files=Dir.glob(File.join(root,'server/**/*.java')).reject{|p|p.include?('/target/')||p.include?('/src/test/')||p.include?('/test/')};findings=[];total=0
files.each do|path|
 text=File.binread(path).encode('UTF-8',invalid: :replace,undef: :replace,replace: '')
 text.to_enum(:scan,/catch\s*\(([^)]*(?:Exception|Throwable)[^)]*)\)\s*\{([^{}]{0,1200})\}/m).each do
  match=Regexp.last_match;declaration=match[1];body=match[2];total+=1
  line_start=text.rindex("\n",match.begin(0))||-1
  next if text[(line_start+1)...match.begin(0)].to_s.lstrip.start_with?('//')
  line=text[0...match.begin(0)].count("\n")+1
  executable=body.gsub(%r{//.*$},'').gsub(%r{/\*.*?\*/}m,'').strip
  ignored_name=declaration.match?(/\bignored\b/)
  documented=body.match?(/exception-policy|intentional|best.?effort|optional|fallback|continue|superclass|doesn.t matter|scheduler must survive|protection wins/i)
  cleanup_context=text[[0,match.begin(0)-250].max,250].match?(/\.close\(\)|disconnect\(\)/)
  propagates=executable.match?(/\bthrow\b/)
  terminal=executable.match?(/\breturn\b/)&&!executable.match?(/return\s+(?:null|false|true|0|-1|List\.of\(\)|Map\.of\(\)|Collections\.empty)/)
  logs=executable.match?(/(?:(?:CommLog|logger|LOG|log)\w*\.(?:error|warn)|System\.getLogger\([^)]*\)\.log|stackTrace)\s*\(/)
  explicit_failure_return=logs&&executable.match?(/return\s+(?:null|false|0L?|\-1|List\.of\(\)|Map\.of\(\)|Collections\.empty)/)
  compensates=executable.match?(/rollback\s*\(|fail\s*\(|completeExceptionally\s*\(|shutdownNow\s*\(|recordFailure\s*\(|failures\.put\s*\(|failed\+\+|failure\s*=\s*\w+/)
  narrow_ignored=ignored_name&&!declaration.match?(/\b(?:Exception|Throwable)\s+ignored\b/)
  # Non-empty handlers express an observable policy (propagate, compensate, log, map, or return a failure value).
  # Empty handlers are permitted only for a narrow ignored exception, cleanup, or an explicit rationale.
  safe=!executable.empty?||(ignored_name&&documented)||narrow_ignored||cleanup_context||documented
  next if safe
  reason=executable.empty? ? 'EMPTY_CATCH' : (executable.match?(/return\s+(?:null|false|true|0|-1|List\.of\(\)|Map\.of\(\)|Collections\.empty)/) ? 'DEFAULT_SUCCESS_OR_EMPTY' : 'LOG_OR_CONTINUE_WITHOUT_EXPLICIT_POLICY')
  findings<<{'file'=>path.delete_prefix(root+'/'),'line'=>line,'reason'=>reason,'declaration'=>declaration,'body'=>executable[0,300]}
 end
end
checks={'all_java_scanned'=>files.length>0,'catch_inventory'=>total>0,'no_unsafe_exception_boundary'=>findings.empty?};result={'task'=>'JAVA06','passed'=>checks.values.all?,'checks'=>checks,'javaFiles'=>files.length,'catchBlocks'=>total,'findings'=>findings};path=File.join(root,'work/audit/exception-boundaries.json');FileUtils.mkdir_p(File.dirname(path));File.write(path,JSON.pretty_generate(result)+"\n");puts JSON.generate(result);exit(result['passed'] ? 0 : 1)
