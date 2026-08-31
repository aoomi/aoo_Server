#!/usr/bin/env ruby
require 'json';require 'fileutils';root=File.expand_path('..',__dir__)
files=Dir.glob(File.join(root,'server/**/*.java')).reject{|f|f.include?('/build/')||f.include?('/target/')||f.include?('/test/')}
banned=/getConnection\s*\(|execute(?:Query|Update|Batch)\s*\(|\.send\s*\(|\.publish\s*\(|\.request\s*\(|Thread\.sleep\s*\(|\.await\s*\(|\.join\s*\(/
findings=[]
files.each do |f|
 s=File.read(f)
 s.to_enum(:scan,/synchronized\s*(?:\([^)]*\)|[^\{\n]+)\s*\{/).each do
  start=Regexp.last_match.begin(0);window=s[start,2500]||'';depth=0;finish=nil;window.each_char.with_index{|c,i|depth+=1 if c=='{';depth-=1 if c=='}';if depth==0&&i>0;finish=i;break;end};body=window[0..(finish||[window.length-1,2499].min)];if body.match?(banned);line=s[0...start].count("\n")+1;findings<<{"file"=>f.delete_prefix(root+'/'),"line"=>line};end
 end
 s.to_enum(:scan,/\b\w+(?:\.\w+)*\.lock\s*\(\s*\)/).each do
  start=Regexp.last_match.begin(0);tail=s[start,5000]||'';unlock=tail.index(/\b\w+(?:\.\w+)*\.unlock\s*\(\s*\)/);next unless unlock;body=tail[0...unlock];if body.match?(banned);line=s[0...start].count("\n")+1;findings<<{"file"=>f.delete_prefix(root+'/'),"line"=>line};end
 end
end
r={'task'=>'LOCK02','passed'=>findings.empty?,'status'=>findings.empty? ? 'passed':'legacy-blocked','findingCount'=>findings.size,'sample'=>findings.first(100),'policy'=>'docs/持锁边界规范.md'}
out=File.join(root,'work/audit/lock02-boundaries.json');FileUtils.mkdir_p(File.dirname(out));File.write(out,JSON.pretty_generate(r)+"\n");puts JSON.generate(r)
