require 'json';require 'fileutils';require 'rexml/document';require 'set'
root=File.expand_path('..',__dir__);queue=['pom.xml'];projects={}
text_at=lambda{|doc,path|REXML::XPath.first(doc,path)&.text&.strip}
until queue.empty?
  rel=queue.shift;next if projects.key?(rel);doc=REXML::Document.new(File.read(File.join(root,rel)));base=File.dirname(rel);artifact=text_at.call(doc,'/*[local-name()="project"]/*[local-name()="artifactId"]');packaging=text_at.call(doc,'/*[local-name()="project"]/*[local-name()="packaging"]')||'jar';deps=[];REXML::XPath.each(doc,'/*[local-name()="project"]/*[local-name()="dependencies"]/*[local-name()="dependency"]'){|d|g=text_at.call(d,'./*[local-name()="groupId"]');a=text_at.call(d,'./*[local-name()="artifactId"]');deps<<a if g=='com.aoo.bcg'};modules=[];REXML::XPath.each(doc,'/*[local-name()="project"]/*[local-name()="modules"]/*[local-name()="module"]'){|m|child=File.join(base,m.text.strip,'pom.xml');child=child.sub(%r{^\./},'');modules<<child;queue<<child if File.exist?(File.join(root,child))};projects[rel]={artifact:artifact,packaging:packaging,deps:deps,modules:modules}
end
owners=projects.group_by{|_,p|p[:artifact]};duplicates=owners.select{|a,v|a&&v.length>1}.keys;artifacts=projects.values.map{|p|p[:artifact]}.to_set;graph=projects.to_h{|f,p|[p[:artifact],p[:deps].select{|d|artifacts.include?(d)}]};visiting=Set.new;visited=Set.new;cycles=[];walk=nil;walk=lambda{|n,path|if visiting.include?(n);cycles<<(path[path.index(n)..]+[n]);return;end;return if visited.include?(n);visiting<<n;graph.fetch(n,[]).each{|d|walk.call(d,path+[d])};visiting.delete(n);visited<<n};graph.keys.each{|n|walk.call(n,[n])}
lines=["# Maven module dependency graph","","Active reactor projects: #{projects.length}","","```mermaid","graph TD"]+graph.flat_map{|a,ds|ds.empty? ? ["  #{a.tr('-','_')}[\"#{a}\"]"] : ds.map{|d|"  #{a.tr('-','_')}[\"#{a}\"] --> #{d.tr('-','_')}[\"#{d}\"]"}}+["```",""]
doc_path=File.join(root,'docs/generated/maven-module-graph.md');FileUtils.mkdir_p(File.dirname(doc_path));File.write(doc_path,lines.join("\n"))
game_artifacts=(artifacts.grep(/^game-category-/)+%w[cdxzmj njpdk scjymj xcpdk zjh zypk].select{|artifact|artifacts.include?(artifact)}).uniq
reachable=Set.new;pending=graph.fetch('game-bootstrap',[]).dup
until pending.empty?
  artifact=pending.shift
  next unless reachable.add?(artifact)
  pending.concat(graph.fetch(artifact,[]))
end
checks={all_declared_modules_resolved:projects.values.flat_map{|p|p[:modules]}.all?{|m|projects.key?(m)},unique_artifact_ids:duplicates.empty?,acyclic_dependencies:cycles.empty?,graph_documented:File.size?(doc_path),bootstrap_is_outer_assembly:(game_artifacts-reachable.to_a).empty?&&reachable.include?('game-hall')}
result={task:'REAL14',passed:checks.values.all?,checks:checks,activeProjectCount:projects.length,projects:projects,duplicates:duplicates,cycles:cycles};out=File.join(root,'work/audit/real14-maven-module-graph.json');FileUtils.mkdir_p(File.dirname(out));File.write(out,JSON.pretty_generate(result)+"\n");puts "REAL14 #{result[:passed]?'passed':'failed'}: #{projects.length} active Maven projects mapped";exit(result[:passed]?0:1)
