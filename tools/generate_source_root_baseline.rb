#!/usr/bin/env ruby
require 'json'; require 'digest'; require 'pathname'; require 'time'
server_root=File.expand_path('..',__dir__);aoo_root=File.expand_path('..',server_root);test_root=ENV.fetch('AOO_SOURCE_ROOT',File.expand_path('../Test',aoo_root))
ROOTS={
'aoo_server'=>server_root,'aoo_client'=>File.join(aoo_root,'Client'),'aoo_admin'=>File.join(aoo_root,'Admin'),'qh_dfmj_reference'=>File.join(test_root,'QH_DFMJ'),'test_source_root'=>test_root,'split_backend_reference'=>File.join(test_root,'情怀后端原代码/Server_game_split'),'new_backend_reference'=>File.join(test_root,'新情怀服务端源码')}.freeze
MARKERS=%w[pom.xml package.json pnpm-lock.yaml package-lock.json build.gradle settings.gradle build.xml project.json].freeze
entries=ROOTS.map do |name,root|
 p=Pathname(root); e={'name'=>name,'absolutePath'=>root,'exists'=>p.directory?}
 if e['exists']
  children=p.children.sort_by(&:to_s).map{|c|st=c.stat;{'name'=>c.basename.to_s,'type'=>c.directory? ? 'directory':'file','size'=>c.file? ? st.size : nil,'modifiedAt'=>st.mtime.utc.iso8601}}
  e['topLevelEntries']=children
  e['topLevelSha256']=Digest::SHA256.hexdigest(children.map{|x|x.values.join("\0")}.join("\n"))
  e['buildMarkers']=MARKERS.select{|m|p.join(m).file?}
  e['lastModifiedAt']=p.stat.mtime.utc.iso8601
 end
 e
end
missing=entries.reject{|e|e['exists']}.map{|e|e['name']}
report={'schemaVersion'=>2,'generatedAt'=>Time.now.utc.iso8601,'scope'=>'absolute roots and deterministic top-level baseline; deep inventories are separate task evidence','roots'=>entries,'summary'=>{'declared'=>entries.length,'present'=>entries.count{|e|e['exists']},'missing'=>missing}}
File.write(ARGV.fetch(0,'work/audit/source-root-baseline.json'),JSON.pretty_generate(report)+"\n")
exit missing.empty? ? 0:2
