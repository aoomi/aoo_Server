#!/usr/bin/env ruby
# frozen_string_literal: true
require 'json'; require 'fileutils'; require 'time'
root=File.expand_path('..',__dir__)
files=Dir.glob(File.join(root,'server','**','*.java')).reject{|path|path.include?('/test/')||path.include?('/target/')||path.include?('/build/')}
candidates=files.map do |path|
  text=File.read(path,encoding:'UTF-8').encode('UTF-8',invalid: :replace,undef: :replace,replace:'')
  next unless text.match?(/RoomEventJournal|JdbcRoomEventJournal/) && text.match?(/\.append\s*\(|appendAndSnapshot\s*\(/)
  sequential=text.match?(/events\.append\s*\(/)&&text.match?(/snapshots\.save\s*\(/)
  jdbcAtomic=text.match?(/instanceof\s+JdbcRoomEventJournal/)&&text.match?(/appendAndSnapshot\s*\(/)
  {path:path.delete_prefix(root+'/'),sequentialFallback:sequential,jdbcAtomic:jdbcAtomic,
   productionSafe:!sequential||jdbcAtomic}
end.compact
journal=File.read(File.join(root,'server/GameCommon/src/main/java/com/aoo/bcg/common/event/JdbcRoomEventJournal.java'),encoding:'UTF-8')
transactionChecks={autoCommitDisabled:journal.include?('setAutoCommit(false)'),eventInsert:journal.include?('event.executeUpdate()'),snapshotWrite:journal.include?('state.executeUpdate()'),commit:journal.include?('connection.commit()'),rollback:journal.include?('connection.rollback()'),fencingLock:journal.include?('FOR UPDATE')}
violations=candidates.reject{|item|item[:productionSafe]}.map{|item|item[:path]}
passed=violations.empty?&&transactionChecks.values.all?&&!candidates.empty?
report={generatedAt:Time.now.utc.iso8601,invariant:'every authoritative JDBC state mutation commits event and snapshot in one fenced transaction',transactionChecks:transactionChecks,candidates:candidates,violations:violations,passed:passed}
output=File.join(root,'work/audit/atomic-event-snapshot-commits.json');FileUtils.mkdir_p(File.dirname(output));File.write(output,JSON.pretty_generate(report)+"\n")
puts JSON.generate(candidates:candidates.length,violations:violations.length,transactionChecks:transactionChecks.values.count(true),passed:passed);exit(passed ? 0 : 1)
