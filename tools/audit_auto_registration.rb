#!/usr/bin/env ruby
require 'json';require 'time';require 'set'
b=JSON.parse(File.read('work/audit/audit-source-baseline.json'))
patterns={
'provider'=>/\b(?:GameProvider|Provider)\b|ServiceLoader\.load/,
'router'=>/\b(?:Router|Route)\b|@RequestMapping|@MessageMapping/,
'handler'=>/\bHandler\b|registerHandler|@EventListener/,
'controller'=>/@(?:RestController|Controller)\b|\bController\b/,
'consumer-listener'=>/@(?:KafkaListener|RocketMQMessageListener|RabbitListener)\b|\b(?:Consumer|Listener)\b/,
'scheduler'=>/@Scheduled\b|\bScheduler\b|scheduleAtFixedRate/,
'cocos-component'=>/@ccclass\b|_decorator\.ccclass/
}.freeze
hits=[]
b.fetch('files').each do |e|
 p=e['path'];next unless p.end_with?('.java','.ts','.vue','.xml','.json')
 begin;File.foreach(p,encoding:'UTF-8').with_index(1){|line,no|patterns.each{|kind,re|hits<<{'kind'=>kind,'path'=>p,'line'=>no,'snippet'=>line.strip[0,240]} if line.match?(re)}};rescue StandardError;next;end
end
summary=hits.group_by{|h|h['kind']}.transform_values{|v|{'occurrences'=>v.length,'files'=>v.map{|h|h['path']}.uniq.length}}
File.write('work/audit/auto-registration-inventory.json',JSON.generate({'schemaVersion'=>1,'generatedAt'=>Time.now.utc.iso8601,'summary'=>summary,'hits'=>hits,'limitations'=>['candidate inventory only; runtime registry membership and generated registrations need execution evidence']})+"\n")
