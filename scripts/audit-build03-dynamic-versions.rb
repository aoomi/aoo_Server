#!/usr/bin/env ruby
require 'json'; require 'fileutils'
root=File.expand_path('..',__dir__); poms=Dir.glob(File.join(root,'{pom.xml,server/*/pom.xml}')); findings=[]
poms.each{|p|t=File.read(p);t.scan(/(?:LATEST|RELEASE|\[[^<]+|\+|\$\{[^}]*dynamic[^}]*\})/).each{|v|findings<<{file:p.delete_prefix(root+'/'),value:v}}}
scripts=Dir.glob(File.join(root,'tools/*')).select{|p|File.file?(p)}; local=scripts.select{|p|File.read(p,encoding:'UTF-8',invalid: :replace,undef: :replace,replace:'').match?(/\.m2\/repository|target\/classes|target\/dependency/)}.map{|p|p.delete_prefix(root+'/')}
revision=File.read(File.join(root,'pom.xml'))[/<revision>([^<]+)<\/revision>/,1]
report={schemaVersion:1,revision:revision,dynamicExternalVersions:findings,localClasspathScripts:local,checks:{noDynamicExternalVersions:findings.empty?,releaseRevision:!revision.to_s.end_with?('-SNAPSHOT'),noUntrackedLocalClasspath:local.empty?},verdict:'blocked-release-version-and-local-classpath'}
out=File.join(root,'docs/generated/build03-dynamic-versions.json');FileUtils.mkdir_p(File.dirname(out));File.write(out,JSON.pretty_generate(report)+"\n");puts "BUILD03 AUDITED: #{report[:verdict]}"
