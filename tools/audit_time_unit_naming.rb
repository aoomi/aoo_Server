#!/usr/bin/env ruby
# frozen_string_literal: true
require 'json';require 'fileutils'
root=File.expand_path('..',__dir__);modules=%w[GameSPI GameCommon Gateway Bootstrap Mahjong Poker LongCard WordCard]
files=modules.flat_map{|name|Dir[File.join(root,'server',name,'src/main/java/**/*.java')]}
primitive=/\b(?:long|int|Integer|Long|double)\s+([A-Za-z][A-Za-z0-9]*(?:timeout|deadline|duration|interval|ttl|delay|expiresAt|timestamp|serverTime))\b/i
explicit=/(?:Millis|Seconds|Nanos|EpochMillis|EpochSeconds)$/
transport_exemptions={'server/Gateway/src/main/java/com/aoo/bcg/gateway/WebSocketFrame.java'=>%w[timestamp],'server/Gateway/src/main/java/com/aoo/bcg/gateway/HttpResult.java'=>%w[timestamp]}
platform_exemptions={'server/GameCommon/src/main/java/com/aoo/bcg/common/persistence/DriverManagerDataSource.java'=>%w[getLoginTimeout]}
violations=[]
files.each do |path|
 relative=path.delete_prefix(root+'/');File.readlines(path).each_with_index do |line,index|
  line.scan(primitive).flatten.each do |name|
   next if name.match?(explicit)||transport_exemptions.fetch(relative,[]).include?(name)||platform_exemptions.fetch(relative,[]).any?{|method|line.include?(method)}
   violations<<"#{relative}:#{index+1}:#{name}"
  end
 end
end
checks={no_ambiguous_internal_primitive_time:violations.empty?,instant_deadline_model:File.read(File.join(root,'server/GameSPI/src/main/java/com/aoo/bcg/gamespi/time/OperationDeadline.java')).include?('Instant deadline'),duration_timeout_model:File.read(File.join(root,'server/Mahjong/src/main/java/com/aoo/bcg/mahjong/MahjongAuthoritativeSession.java')).include?('Duration operationTimeout')}
result={task:'TIME12',passed:checks.values.all?,checks:checks,documentedBoundaryExemptions:transport_exemptions.merge(platform_exemptions),violations:violations};out=File.join(root,'work/audit/time-unit-naming.json');FileUtils.mkdir_p(File.dirname(out));File.write(out,JSON.pretty_generate(result)+"\n");puts JSON.generate(result);exit(result[:passed] ? 0 : 1)
