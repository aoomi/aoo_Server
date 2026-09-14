#!/usr/bin/env ruby
require 'json';require 'pathname';require 'time'
source=JSON.parse(File.read('work/audit/source-root-baseline.json'))
def git_pointer(root)
 g=Pathname(root).join('.git');return {'present'=>false} unless g.exist?
 if g.file?
  target=g.read.strip.sub(/^gitdir:\s*/,'');g=Pathname(target);g=Pathname(root).join(g) unless g.absolute?
 end
 head=g.join('HEAD');return {'present'=>true,'head'=>nil} unless head.file?
 value=head.read.strip; result={'present'=>true,'head'=>value}
 if value.start_with?('ref: ')
  ref=value.delete_prefix('ref: ');rf=g.join(ref);result['ref']=ref;result['commit']=rf.file? ? rf.read.strip : nil
 else result['commit']=value end
 result
end
roots=source.fetch('roots').map do |e|
 root=e['absolutePath']; markers=e.fetch('buildMarkers',[])
 {'name'=>e['name'],'absolutePath'=>root,'exists'=>e['exists'],'vcs'=>e['exists'] ? git_pointer(root):{'present'=>false},'buildMarkers'=>markers,'buildabilityStatus'=>markers.empty? ? 'unproven-no-root-marker':'unverified','frozenAt'=>Time.now.utc.iso8601}
end
missing=roots.select{|e|e['exists'] && (!e.dig('vcs','commit') || e['buildabilityStatus']!='verified')}.map{|e|e['name']}
File.write('work/audit/source-provenance-baseline.json',JSON.pretty_generate({'schemaVersion'=>1,'generatedAt'=>Time.now.utc.iso8601,'roots'=>roots,'unproven'=>missing})+"\n")
exit missing.empty? ? 0:3
