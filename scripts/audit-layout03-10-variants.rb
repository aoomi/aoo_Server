#!/usr/bin/env ruby
require 'json'; require 'find'; require 'fileutils'; require 'digest'
root=File.expand_path('..',__dir__); client=File.expand_path('../Client',root); assets=File.join(client,'assets'); out=File.join(root,'docs/generated'); FileUtils.mkdir_p(out)
base=JSON.parse(File.read(File.join(out,'layout01-variant-classification.json'))); records=base['records'];
emit=lambda{|id,data|File.write(File.join(out,"#{id}.json"),JSON.pretty_generate({schemaVersion:1}.merge(data))+"\n")}
resource=records.select{|r|r['category']=='resource'}; code=records.select{|r|r['category']=='interaction-or-business'}
emit.call('layout03-resource-theme',{variantResources:resource,themeMapFiles:Dir.glob(File.join(assets,'**/*theme*.{json,ts}')).map{|p|p.delete_prefix(client+'/')},completeThemeMapping:false,passed:false})
emit.call('layout04-business-uniqueness',{variantBusinessFiles:code,sharedPresenterProof:false,passed:code.empty?})
tests=Dir.glob(File.join(client,'tests/**/*')).select{|p|File.file?(p)}
emit.call('layout05-rotation-state',{testFiles:tests.map{|p|p.delete_prefix(client+'/')},requiredStates:%w[input popup game cardSelection timer scroll],automatedCoverage:false,passed:false})
safe_hits=[];Dir.glob(File.join(assets,'**/*.ts')).each{|p|File.foreach(p).with_index(1){|l,i|safe_hits<<{path:p.delete_prefix(client+'/'),line:i+1} if l.match?(/SafeArea|safe.?area|visibleSize|windowSize/i)}}
emit.call('layout06-safe-area',{implementationHits:safe_hits,notchRoundedCornerHomeIndicatorBrowserToolbarMatrix:false,passed:false})
emit.call('layout07-variant-matrix',{testFiles:tests.map{|p|p.delete_prefix(client+'/')},requiredProfiles:%w[landscape portrait 16:9 4:3 ultrawide mobile-browser],realClickCoverage:false,passed:false})
archives=Dir.glob(File.join(client,'development/archived-orientation-folders/**/*')).select{|p|File.file?(p)}; active_refs=JSON.parse(File.read(File.join(out,'carch08-dynamic-archive-references.json')))['hits'];
emit.call('layout08-archive-exclusion',{archiveFiles:archives.length,activeReferences:active_refs,outsideAssets:true,passed:active_refs.empty?})
pairs=JSON.parse(File.read(File.join(out,'layout02-layout-reuse.json')))['duplicatedStructures']; baseline=pairs.map{|p|p['native']||p['normalizedPath']}.compact.sort
emit.call('layout09-variant-growth-gate',{baseline:baseline,baselineCount:baseline.length,policy:'No new complete portrait/landscape/native Prefab pair may be added beyond this baseline.',passed:true})
emit.call('layout10-performance',{requiredMetrics:%w[layoutRecalculationMs frameRate memory touchLatency],measuredProfiles:[],passed:false})
puts "LAYOUT03-LAYOUT10 audited: resources=#{resource.length}, business=#{code.length}, duplicateBaseline=#{baseline.length}"
