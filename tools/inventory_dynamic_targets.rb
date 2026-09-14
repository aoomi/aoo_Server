#!/usr/bin/env ruby
require 'json';require 'time'
b=JSON.parse(File.read('work/audit/audit-source-baseline.json'));patterns={
'reflection'=>/Class\.forName|loadClass\(|getDeclaredMethod|getMethod\(|MethodHandles|newInstance\(/,
'service-loader'=>/ServiceLoader\.load|META-INF\/services/,
'spring-name'=>/@(?:Qualifier|Resource|Bean)\b|getBean\(/,
'cocos-component'=>/getComponent\(|addComponent\(|__type__|_scriptUuid/,
'dynamic-import'=>/import\s*\(|require\s*\(/,
'config-class'=>/(?:className|implClass|providerClass|handlerClass)\s*[=:]/
}.freeze
hits=[]
b.fetch('files').each do |entry|
 path=entry['path'];next unless path.end_with?('.java','.ts','.js','.mjs','.vue','.xml','.json','.properties','.yml','.yaml','.prefab','.scene','.meta')
 begin;File.foreach(path,encoding:'UTF-8').with_index(1){|line,no|patterns.each{|kind,re|hits<<{'kind'=>kind,'path'=>path,'line'=>no,'snippet'=>line.strip[0,240]} if line.match?(re)}};rescue StandardError;next;end
end
summary=hits.group_by{|h|h['kind']}.transform_values(&:length)
File.write('work/audit/dynamic-target-inventory.json',JSON.generate({'schemaVersion'=>1,'generatedAt'=>Time.now.utc.iso8601,'summary'=>summary,'hits'=>hits})+"\n")
