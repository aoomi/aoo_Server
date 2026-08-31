#!/usr/bin/env ruby
require 'json';require 'find';require 'digest';require 'fileutils'
root=File.expand_path('..',__dir__);generated=[]
Find.find(File.join(root,'server')) do|p|
  if File.directory?(p) && %w[target build package_hall].include?(File.basename(p))
    files=Dir.glob(File.join(p,'**/*')).select{|f|File.file?(f)}
    generated<<{path:p.delete_prefix(root+'/'),files:files.length,bytes:files.sum{|f|File.size(f)},sampleHashes:files.first(20).map{|f|{path:f.delete_prefix(root+'/'),sha256:Digest::SHA256.file(f).hexdigest}}}
    Find.prune
  end
end
release_policy=JSON.parse(File.read(File.join(root,'config/release-allowlist.json')))
mixed=generated.select{|row|row[:path].include?('package_hall')||row[:path].include?('/target')}
report={schemaVersion:1,generatedDirectories:generated,mixedRoleDirectories:mixed.map{|r|r[:path]},releaseForbidden:release_policy['forbidden'],sourceGeneratedRolesSeparated:mixed.empty?,passed:mixed.empty?}
out=File.join(root,'docs/generated/pkg01-generated-role.json');FileUtils.mkdir_p(File.dirname(out));File.write(out,JSON.pretty_generate(report)+"\n");abort "PKG01 blocked: #{mixed.length} generated/package directories remain mixed in source tree" unless report[:passed]
