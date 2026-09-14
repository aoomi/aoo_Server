#!/usr/bin/env ruby
require 'json';require 'fileutils';root=File.expand_path('..',__dir__);files=Dir.glob(File.join(root,'server/**/{src,source}/**/*.java'));annotated=[];self_calls=[]
files.each do |path|
 text=File.binread(path).encode('UTF-8',invalid: :replace,undef: :replace,replace: '')
 next unless text.include?('@Transactional')
 names=text.scan(/@Transactional(?:\([^)]*\))?\s+(?:public|protected|private)?\s*[\w<>?, .\[\]]+\s+(\w+)\s*\(/m).flatten
 annotated << {'file'=>path.delete_prefix(root+'/'),'methods'=>names}
 names.each do |name|
  calls=text.scan(/(?<![\w.])#{Regexp.escape(name)}\s*\(/).length
  self_calls << {'file'=>path.delete_prefix(root+'/'),'method'=>name,'callCount'=>calls-1} if calls>1
 end
end
checks={'all_java_scanned'=>files.length>0,'transactional_inventory'=>true,'no_self_invocation'=>self_calls.empty?}
result={'task'=>'JAVA01','passed'=>checks.values.all?,'checks'=>checks,'javaFiles'=>files.length,'annotatedClasses'=>annotated,'selfInvocationFindings'=>self_calls}
path=File.join(root,'work/audit/transactional-self-invocation.json');FileUtils.mkdir_p(File.dirname(path));File.write(path,JSON.pretty_generate(result)+"\n");puts JSON.generate(result);exit(result['passed'] ? 0 : 1)
