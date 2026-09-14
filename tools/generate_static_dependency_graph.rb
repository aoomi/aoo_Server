#!/usr/bin/env ruby
require 'json';require 'time'
baseline=JSON.parse(File.read('work/audit/audit-source-baseline.json'))
files=baseline.fetch('files').map{|e|e['path']}.select{|p|p.end_with?('.java','.ts','.vue','.js','.mjs')}
edges=[];entries=[];inheritance=[];annotations=[]
files.each do |path|
 begin;text=File.read(path,encoding:'UTF-8');rescue Encoding::InvalidByteSequenceError,Encoding::UndefinedConversionError,Errno::ENOENT;next;end
 text.each_line.with_index(1) do |line,no|
  if line =~ /^\s*import\s+(?:type\s+)?(?:.*?\s+from\s+)?["']?([^"';]+)["']?\s*;?/
   edges << {'source'=>path,'line'=>no,'kind'=>'import','target'=>$1.strip}
  elsif line =~ /^\s*import\s+([\w.]+)(?:\.\*)?\s*;/
   edges << {'source'=>path,'line'=>no,'kind'=>'import','target'=>$1}
  end
  entries << {'path'=>path,'line'=>no,'kind'=>'main'} if line.match?(/public\s+static\s+void\s+main\s*\(/)
  entries << {'path'=>path,'line'=>no,'kind'=>'framework-entry'} if line.match?(/@(RestController|Controller|Component|Service|Configuration|Scheduled|EventListener|MessageMapping|ServerEndpoint)\b/)
  if line =~ /\b(class|interface)\s+(\w+)\s+(extends|implements)\s+([^\{]+)/
   inheritance << {'path'=>path,'line'=>no,'symbol'=>$2,'relation'=>$3,'targets'=>$4.strip}
  end
  annotations << {'path'=>path,'line'=>no,'annotation'=>$1} if line =~ /^\s*@([A-Za-z_]\w*)/
 end
end
out={'schemaVersion'=>1,'generatedAt'=>Time.now.utc.iso8601,'coverage'=>{'files'=>files.length,'edgeKinds'=>['import'],'knownLimitations'=>['method invocation resolution not implemented','reflection and generated sources require separate scanners']},'imports'=>edges,'entries'=>entries,'inheritance'=>inheritance,'annotations'=>annotations}
File.write('work/audit/static-dependency-graph.json',JSON.generate(out)+"\n")
