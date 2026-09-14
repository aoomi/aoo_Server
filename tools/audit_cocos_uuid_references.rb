#!/usr/bin/env ruby
require 'json';require 'find';require 'time';require 'set'
server_root=File.expand_path('..',__dir__);root=ENV.fetch('AOO_CLIENT_ASSETS',File.expand_path('../Client/assets',server_root));uuid_paths=Hash.new{|h,k|h[k]=[]};parse_errors=[]
collect_uuids=lambda do |value,path|
 case value
 when Hash
  uuid=value['uuid'];uuid_paths[uuid] << path if uuid.is_a?(String) && !uuid.empty?
  value.each_value{|child|collect_uuids.call(child,path)}
 when Array
  value.each{|child|collect_uuids.call(child,path)}
 end
end
Find.find(root) do |p|
 next unless p.end_with?('.meta')
 begin
  data=JSON.parse(File.read(p));collect_uuids.call(data,p)
 rescue JSON::ParserError,Encoding::InvalidByteSequenceError => e
  parse_errors << {'path'=>p,'error'=>e.message[0,160]}
 end
end
engine_builtin=Set.new(%w[eca5d2f2-8ef6-41c2-bbe6-f9c79d09c432 3a7bb79f-32fd-422e-ada2-96f518fed422 0a386790-4e52-42c3-8584-e58b770ecf3d 2086c170-7025-4208-93b9-89d5b64c3391 388c294c-9b17-4c3f-b89f-e1dc0eef1d80 5c3bb932-6c3c-468f-88a9-c8c61d458641 9797aa57-b97a-40cc-98e3-60464473b04a a23235d1-15db-4b95-8439-a2e005bfff91]);known=uuid_paths.keys.to_set;refs=[];missing=[];engine_refs=[]
Find.find(root) do |p|
 next unless p.end_with?('.prefab','.scene','.json')
 begin
  text=File.read(p);text.scan(/"__uuid__"\s*:\s*"([0-9a-fA-F-]{20,40})"/).flatten.each do |u|
   item={'source'=>p,'uuid'=>u};refs<<item;if engine_builtin.include?(u);engine_refs<<item;elsif !known.include?(u);missing<<item;end
  end
 rescue StandardError;next;end
end
uuid_paths.each_value(&:uniq!)
duplicates=uuid_paths.select{|_,v|v.length>1}.map{|u,v|{'uuid'=>u,'paths'=>v}}
out={'schemaVersion'=>1,'generatedAt'=>Time.now.utc.iso8601,'summary'=>{'metaUuids'=>known.length,'directReferences'=>refs.length,'engineBuiltinReferences'=>engine_refs.length,'missingDirectReferences'=>missing.length,'duplicateMetaUuids'=>duplicates.length,'metaParseErrors'=>parse_errors.length},'engineBuiltinUuids'=>engine_builtin.to_a.sort,'duplicates'=>duplicates,'missing'=>missing,'parseErrors'=>parse_errors,'limitations'=>['runtime-computed resource paths require load instrumentation']}
File.write('work/audit/cocos-uuid-reference-audit.json',JSON.generate(out)+"\n")
exit(missing.empty? && duplicates.empty? && parse_errors.empty? ? 0:4)
