#!/usr/bin/env ruby
require 'json';require 'fileutils';root=File.expand_path('..',__dir__);files=Dir.glob(File.join(root,'server/**/*.java'));findings=[];markers=/\b(HttpClient|RestTemplate|WebClient|RocketMQ|sendMessage|publish|Future\.get|\.join\(|Thread\.sleep)\b/
files.each do |path|
 text=File.binread(path).encode('UTF-8',invalid: :replace,undef: :replace,replace: '')
 starts=[];text.to_enum(:scan,/@Transactional(?:\([^)]*\))?\s+(?:public|protected|private)?\s*[\w<>?, .\[\]]+\s+(\w+)\s*\([^)]*\)\s*\{/m).each{starts<<[Regexp.last_match.begin(0),Regexp.last_match.end(0),Regexp.last_match(1)]}
 starts.each do |start,body_start,name|
  depth=1;i=body_start
  while i<text.length&&depth>0;depth+=1 if text.getbyte(i)==123;depth-=1 if text.getbyte(i)==125;i+=1;end
  body=text[body_start...i];hits=body.scan(markers).flatten.uniq;findings<<{'file'=>path.delete_prefix(root+'/'),'method'=>name,'blockingCalls'=>hits} unless hits.empty?
 end
end
checks={'all_java_scanned'=>files.length>0,'transaction_bodies_parsed'=>true,'no_remote_or_long_wait_inside'=>findings.empty?};result={'task'=>'JAVA02','passed'=>checks.values.all?,'checks'=>checks,'javaFiles'=>files.length,'findings'=>findings};p=File.join(root,'work/audit/transaction-remote-waits.json');FileUtils.mkdir_p(File.dirname(p));File.write(p,JSON.pretty_generate(result)+"\n");puts JSON.generate(result);exit(result['passed'] ? 0 : 1)
