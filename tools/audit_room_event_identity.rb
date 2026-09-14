#!/usr/bin/env ruby
# frozen_string_literal: true
require 'json'; require 'fileutils'; require 'time'
root=File.expand_path('..',__dir__)
files={
  migration:'database/migrations/V20260823_01__room_event_business_identity.sql',
  identity:'server/GameCommon/src/main/java/com/aoo/bcg/common/event/RoomEventIdentity.java',
  journal:'server/GameCommon/src/main/java/com/aoo/bcg/common/event/RoomEventJournal.java',
  jdbc:'server/GameCommon/src/main/java/com/aoo/bcg/common/event/JdbcRoomEventJournal.java'
}
checks={
  roundNo: /round_no|roundNo/,
  sequence: /event_sequence|sequence/,
  businessEventId: /business_event_id|businessEventId/,
  eventType: /event_type|eventType/,
  schemaVersion: /schema_version|schemaVersion/,
  identityType: /RoomEventIdentity/,
  uniqueBusinessKey: /UNIQUE KEY uq_room_round_business_view/
}
matrix=files.map do |name,path|
  text=File.file?(File.join(root,path)) ? File.read(File.join(root,path),encoding:'UTF-8') : ''
  {artifact:name,path:path,present:!text.empty?,checks:checks.to_h{|key,pattern|[key,text.match?(pattern)]}}
end
required={migration:%i[roundNo sequence businessEventId eventType schemaVersion uniqueBusinessKey],identity:%i[roundNo sequence businessEventId eventType schemaVersion],journal:%i[identityType],jdbc:%i[roundNo sequence businessEventId eventType schemaVersion]}
violations=matrix.flat_map do |entry|
  required.fetch(entry[:artifact]).reject{|key|entry[:checks][key]}.map{|key|"#{entry[:artifact]}.#{key}"}
end
report={generatedAt:Time.now.utc.iso8601,invariant:'room+round+sequence+businessEventId+eventType+schemaVersion are durable and business identity is unique',artifacts:matrix,violations:violations,passed:violations.empty?}
output=File.join(root,'work/audit/room-event-identity.json');FileUtils.mkdir_p(File.dirname(output));File.write(output,JSON.pretty_generate(report)+"\n")
puts JSON.generate(artifacts:matrix.length,violations:violations.length,passed:report[:passed]);exit(report[:passed]?0:1)
