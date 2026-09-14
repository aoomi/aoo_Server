#!/usr/bin/env ruby
require'json';root=File.expand_path('../../Client/assets',__dir__);changed=0
Dir.glob(File.join(root,'**/Spine/**/skeleton.json.meta')).sort.each do|skeleton_meta|
 atlas_meta=skeleton_meta.sub('/Spine/','/Atlas/').sub('/skeleton.json.meta','/skeleton.atlas.meta');next unless File.file?(atlas_meta);s=JSON.parse(File.read(skeleton_meta));a=JSON.parse(File.read(atlas_meta));s['userData']||={};next if s['userData']['atlasUuid']==a.fetch('uuid');s['userData']['atlasUuid']=a.fetch('uuid');File.write(skeleton_meta,JSON.pretty_generate(s)+"\n");changed+=1
end
puts "updated=#{changed}"
