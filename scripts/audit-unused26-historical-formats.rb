#!/usr/bin/env ruby
require 'json'
require 'pathname'

root = Pathname(__dir__).join('..').expand_path
required = %w[
  server/GameCommon/src/main/java/com/aoo/bcg/common/event/EventSchemaRegistry.java
  server/GameCommon/src/main/java/com/aoo/bcg/common/event/OutboxEvent.java
  server/GameCommon/src/main/java/com/aoo/bcg/common/recovery/RoomSnapshot.java
  server/GameCommon/src/main/java/com/aoo/bcg/common/recovery/RoomDeadlineSnapshot.java
  server/GameCommon/src/main/java/com/aoo/bcg/common/replay/ReplayFrame.java
  server/GameCommon/src/main/java/core/replay/PerspectiveReplayEvent.java
  server/gameServer/src/business/global/replay/LegacyPerspectiveReplayReader.java
  server/gameServer/src/business/global/replay/LegacyPerspectiveReplayRecorder.java
]
test = 'server/GameCommon/src/test/java/com/aoo/bcg/common/serialization/HistoricalFormatCompatibilityTest.java'
missing = (required + [test]).reject { |path| root.join(path).exist? }
schema = root.join(required.first).exist? ? root.join(required.first).read : ''
test_text = root.join(test).exist? ? root.join(test).read : ''
checks = {
  adjacentUpcasters: schema.include?('missing adjacent event upcaster'),
  futureSchemaFailsClosed: schema.include?('unsupported future event schema'),
  historicalJsonFixture: test_text.include?('legacyMarker') && test_text.include?('schemaVersion'),
  snapshotFixture: test_text.include?('legacy-2.22'),
  replayFixture: test_text.include?('ReplayFrame'),
  deadlineFixture: test_text.include?('RoomDeadlineSnapshot')
}
errors = []
errors << "required historical format types missing: #{missing.join(', ')}" unless missing.empty?
errors << "historical compatibility checks incomplete: #{checks.select { |_k, v| !v }.keys.join(', ')}" unless checks.values.all?
report = {
  task: 'UNUSED26', status: errors.empty? ? 'passed' : 'failed', retainedTypes: required,
  compatibilityTest: test, checks: checks,
  deletionPolicy: 'Historical JSON/event/snapshot/replay readers, DTOs, codecs and adjacent upcasters are data contracts and cannot be deleted solely because direct production calls are absent.',
  errors: errors
}
out = root.join('work/audit/unused26-historical-formats.json')
out.dirname.mkpath
out.write(JSON.pretty_generate(report) + "\n")
abort(errors.join("\n")) unless errors.empty?
puts 'UNUSED26 passed: historical JSON, event, snapshot, deadline and replay compatibility types are protected'
