#!/usr/bin/env ruby
# frozen_string_literal: true
require 'json';require 'fileutils';require 'time'
root=File.expand_path('..',__dir__)
consumer_files=Dir.glob(File.join(root,'server','**','*.java')).reject{|p|p.include?('/test/')||p.include?('/target/')||p.include?('/build/')}.select do |path|
  text=File.read(path,encoding:'UTF-8').encode('UTF-8',invalid: :replace,undef: :replace,replace:'')
  text.match?(/MessageListener|registerMessageListener|@KafkaListener|@RabbitListener/)
end
rows=consumer_files.map do |path|
  text=File.read(path,encoding:'UTF-8').encode('UTF-8',invalid: :replace,undef: :replace,replace:'')
  checks={claim:text.include?('.claim('),complete:text.include?('.complete('),abandon:text.include?('.abandon('),eventId:text.include?('eventId()')}
  {path:path.delete_prefix(root+'/'),checks:checks,covered:checks.values.all?}
end
store=File.read(File.join(root,'server/GameCommon/src/main/java/com/aoo/bcg/common/event/JdbcConsumedEventStore.java'),encoding:'UTF-8')
resultChecks={code:store.include?('result_code'),schemaVersion:store.include?('result_schema_version'),payload:store.include?('result_payload'),query:store.include?('previousResult')}
uncovered=rows.reject{|row|row[:covered]};passed=!rows.empty?&&uncovered.empty?&&resultChecks.values.all?
report={generatedAt:Time.now.utc.iso8601,consumers:rows,resultChecks:resultChecks,uncovered:uncovered.map{|r|r[:path]},passed:passed};output=File.join(root,'work/audit/consumer-idempotent-results.json');FileUtils.mkdir_p(File.dirname(output));File.write(output,JSON.pretty_generate(report)+"\n");puts JSON.generate(consumers:rows.length,uncovered:uncovered.length,resultChecks:resultChecks.values.count(true),passed:passed);exit(passed ? 0 : 1)
