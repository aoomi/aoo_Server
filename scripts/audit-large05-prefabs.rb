#!/usr/bin/env ruby
# frozen_string_literal: true
require 'digest';require 'json';require 'time'
root=File.expand_path('..',__dir__);assets=File.expand_path('../Client/assets',root);paths=Dir.glob(File.join(assets,'**/*.prefab')).select{|path|File.size(path)>=1024*1024}.sort
rows=paths.map do|path|
 data=JSON.parse(File.read(path));objects=data.is_a?(Array) ? data:[data];types=objects.grep(Hash).map{|value|value['__type__']}.compact
 relative=path.delete_prefix(assets+'/');counterpart=if relative.include?('/Native/') then relative.sub('/Native/','/').sub(%r{/skin-([^/]+)/},'/skin-\1/') else nil end
 {path:relative,bytes:File.size(path),sha256:Digest::SHA256.file(path).hexdigest,nodes:types.count('cc.Node'),components:types.count{|type|type!='cc.Node'&&type!='cc.Prefab'&&type!='cc.PrefabInfo'},prefabInstances:types.count{|type|type.include?('PrefabInstance')},nativeVariant:relative.include?('/Native/'),counterpart:counterpart}
rescue JSON::ParserError=>error
 {path:path.delete_prefix(assets+'/'),bytes:File.size(path),parseError:error.message}
end
pairs=rows.select{|row|row[:nativeVariant]&&row[:counterpart]}.map do|row|
 target=File.join(assets,row[:counterpart]);{native:row[:path],counterpart:row[:counterpart],counterpartExists:File.file?(target),identical:File.file?(target)&&Digest::SHA256.file(target)==row[:sha256]}
end
checks={allParse:rows.none?{|row|row[:parseError]},nodeBudget:rows.all?{|row|row[:nodes].to_i<=1000},componentBudget:rows.all?{|row|row[:components].to_i<=1200},nativePairsNotByteDuplicates:pairs.none?{|pair|pair[:identical]},subPrefabExtraction:rows.all?{|row|row[:bytes]<2*1024*1024||row[:prefabInstances].to_i>0},instantiationBenchmark:false}
report={schemaVersion:1,generatedAt:Time.now.utc.iso8601,task:'LARGE05',passed:checks.values.all?,thresholdBytes:1024*1024,budgets:{nodes:1000,components:1200,sourceBytes:2*1024*1024},prefabs:rows,pairs:pairs,checks:checks,blockers:['large Native/default prefab variants remain byte-identical','large monoliths lack extracted sub-Prefab instances','Creator runtime instantiation p50/p99 benchmark evidence is absent']}
File.write(File.join(root,'docs/generated/large05-prefabs.json'),JSON.pretty_generate(report)+"\n");puts "LARGE05 #{report[:passed] ? 'PASS':'FAIL'} prefabs=#{rows.length} pairs=#{pairs.length}";exit(report[:passed] ? 0:1)
