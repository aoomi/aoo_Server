#!/usr/bin/env ruby
require 'json';require 'find';require 'digest';require 'fileutils';require 'pathname'
root=File.expand_path('..',__dir__);repo=File.expand_path('~/.m2/repository');jars=Dir.glob(File.join(root,'server/**/target/{lib,runtime/lib}/**/*.jar')).sort
reactor=Dir.glob(File.join(root,'server/**/target/*.jar')).reject{|p|p.end_with?('-sources.jar','-javadoc.jar')}
repo_by_name=Hash.new{|h,k|h[k]=[]};Dir.glob(File.join(repo,'**/*.jar')).each{|p|repo_by_name[File.basename(p)]<<p}
rows=jars.map do|jar|
  sha=Digest::SHA256.file(jar).hexdigest;matches=repo_by_name[File.basename(jar)].select{|p|Digest::SHA256.file(p).hexdigest==sha};coords=matches.map do|p|
    rel=Pathname.new(p).relative_path_from(Pathname.new(repo)).to_s.split('/');next if rel.length<4;{groupId:rel[0...-3].join('.'),artifactId:rel[-3],version:rel[-2]}
  end.compact
  reactor_matches=reactor.select{|p|File.basename(p)==File.basename(jar)&&Digest::SHA256.file(p).hexdigest==sha}.map{|p|p.delete_prefix(root+'/')}
  {path:jar.delete_prefix(root+'/'),bytes:File.size(jar),sha256:sha,repositoryCoordinates:coords,reactorArtifacts:reactor_matches}
end
unresolved=rows.select{|r|r[:repositoryCoordinates].empty?&&r[:reactorArtifacts].empty?};report={schemaVersion:1,jars:rows,jarCount:rows.length,unresolved:unresolved.map{|r|r[:path]},consistent:unresolved.empty?,passed:unresolved.empty?};out=File.join(root,'docs/generated/pkg02-target-lib-inventory.json');FileUtils.mkdir_p(File.dirname(out));File.write(out,JSON.pretty_generate(report)+"\n");abort "PKG02 blocked: #{unresolved.length} target/lib jars not resolved to exact repository/reactor artifacts" unless report[:passed];puts "PKG02 PASS: #{rows.length} target/lib jars mapped to exact repository or reactor artifacts"
