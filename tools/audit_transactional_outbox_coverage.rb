#!/usr/bin/env ruby
# frozen_string_literal: true
require 'json'; require 'fileutils'; require 'time'
root=File.expand_path('..',__dir__)
files=Dir.glob(File.join(root,'server','**','*.java')).reject{|path|path.include?('/test/')||path.include?('/target/')||path.include?('/build/')}
candidates=files.map do |path|
  text=File.read(path,encoding:'UTF-8').encode('UTF-8',invalid: :replace,undef: :replace,replace:'')
  next unless text.include?('setAutoCommit(false)') && text.match?(/(?:INSERT|UPDATE|DELETE)\s+/i)
  domainWrites=text.scan(/(?:INSERT\s+INTO|UPDATE|DELETE\s+FROM)\s+([a-zA-Z0-9_]+)/i).flatten.uniq.reject{|table|table=='aoo_outbox'||table=='aoo_consumed_event'}
  next if domainWrites.empty?
  outbox=text.match?(/(?:INSERT\s+INTO\s+aoo_outbox|TransactionalOutbox)/i)
  rollback=text.include?('rollback()'); commit=text.include?('commit()')
  {path:path.delete_prefix(root+'/'),domainTables:domainWrites,transactionalOutbox:outbox,
   commit:commit,rollback:rollback,covered:outbox&&commit&&rollback}
end.compact
uncovered=candidates.reject{|item|item[:covered]}
report={generatedAt:Time.now.utc.iso8601,invariant:'every transaction creating a cross-service database fact writes its outbox row on the same connection before commit',candidates:candidates,covered:candidates.length-uncovered.length,uncovered:uncovered,passed:!candidates.empty?&&uncovered.empty?}
output=File.join(root,'work/audit/transactional-outbox-coverage.json');FileUtils.mkdir_p(File.dirname(output));File.write(output,JSON.pretty_generate(report)+"\n")
puts JSON.generate(candidates:candidates.length,covered:report[:covered],uncovered:uncovered.length,passed:report[:passed]);exit(report[:passed] ? 0 : 1)
