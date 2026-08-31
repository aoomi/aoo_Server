#!/usr/bin/env ruby
# frozen_string_literal: true
require 'json';require 'time'
root=File.expand_path('..',__dir__);client=File.expand_path('../Client/build/web-desktop',root);baseline=JSON.parse(File.read(File.join(root,'config/artifact-size-baseline.json')))
def tree(path);Dir.glob(File.join(path,'**/*')).select{|entry|File.file?(entry)}.sum{|entry|File.size(entry)}end
current={};if Dir.exist?(client);current['clientBuild']=tree(client);current['clientAssets']=tree(File.join(client,'assets'));Dir.glob(File.join(client,'assets/*')).select{|path|File.directory?(path)}.each{|path|current["bundle:#{File.basename(path)}"]=tree(path)};end
current['serverJars']=Dir.glob(File.join(root,'server/*/target/*.jar')).sum{|path|File.size(path)}
limit=baseline['maximumGrowthPercent'];comparisons=baseline['artifacts'].map do|name,bytes|
 actual=current[name];{artifact:name,baselineBytes:bytes,currentBytes:actual,changePercent:actual ? ((actual-bytes)*100.0/bytes).round(3):nil,withinBudget:actual&&actual<=bytes*(1+limit/100.0)}
end
checks={currentClientBuildPresent:Dir.exist?(client),allBaselineArtifactsPresent:comparisons.all?{|row|row[:currentBytes]},allWithinGrowthBudget:comparisons.all?{|row|row[:withinBudget]},perCommitCiHistory:false,containerImageApplicable:baseline['containerImage']!=nil}
report={schemaVersion:1,generatedAt:Time.now.utc.iso8601,task:'LARGE08',passed:checks.values.all?,baseline:'config/artifact-size-baseline.json',maximumGrowthPercent:limit,comparisons:comparisons,checks:checks,blockers:['Creator clean build is not reproducible in CI, so per-commit Client package trend cannot be enforced','No container image artifact exists to measure']}
File.write(File.join(root,'docs/generated/large08-package-growth.json'),JSON.pretty_generate(report)+"\n");puts "LARGE08 #{report[:passed] ? 'PASS':'FAIL'} artifacts=#{comparisons.length}";exit(report[:passed] ? 0:1)
