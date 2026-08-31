#!/usr/bin/env ruby
require 'json';require 'open3';require 'fileutils'
root=File.expand_path('..',__dir__);modules=Dir.glob(File.join(root,'server/*/target/runtime/lib')).sort;findings=[];summaries=[];cache={}
modules.each do|lib|
  owners=Hash.new{|h,k|h[k]=[]};jars=Dir.glob(File.join(lib,'*.jar')).sort
  jars.each do|jar|;key=[File.basename(jar),File.size(jar)];classes=cache[key];unless classes;stdout,_,status=Open3.capture3('unzip','-Z1',jar);classes=status.success? ? stdout.lines.map(&:strip).grep(/\.class\z/).reject{|n|n.start_with?('META-INF/versions/')} : [];cache[key]=classes;end;classes.each{|klass|owners[klass]<<File.basename(jar)};end
  duplicates=owners.select{|_,v|v.uniq.length>1}.map{|k,v|{class:k,jars:v.uniq}};summaries<<{module:lib.delete_prefix(root+'/'),jars:jars.length,classes:owners.length,duplicateClasses:duplicates.length};findings.concat(duplicates.map{|d|d.merge(module:lib.delete_prefix(root+'/'))})
end
report={schemaVersion:1,modules:summaries,duplicates:findings,passed:findings.empty?};out=File.join(root,'docs/generated/pkg05-duplicate-jar-classes.json');FileUtils.mkdir_p(File.dirname(out));File.write(out,JSON.pretty_generate(report)+"\n");abort "PKG05 blocked: #{findings.length} duplicate runtime classes" unless report[:passed];puts 'PKG05 PASS: no duplicate runtime classes'
