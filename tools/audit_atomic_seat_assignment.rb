#!/usr/bin/env ruby
# frozen_string_literal: true
require 'json'; require 'fileutils'
root = File.expand_path('..', __dir__)
common_path = File.join(root, 'server/GameCommon/src/main/java/com/aoo/bcg/common/room/AuthoritativeRoom.java')
common = File.read(common_path)
representatives = %w[
  server/Mahjong/src/main/java/com/aoo/bcg/mahjong/MahjongAuthoritativeSession.java
  server/ZJH/src/business/global/pk/zjh/ZJHTable.java
  server/ZYPK/src/business/global/pk/zypk/ZYPKTable.java
]
checks = {
  designated_assignment_atomic: common.include?('synchronized SeatAssignment assign('),
  automatic_assignment_atomic: common.include?('synchronized SeatAssignment assignAutomatically('),
  player_uniqueness_enforced: common.include?('requirePlayerAvailable(playerId)'),
  ownership_safe_rollback: common.include?('rollbackAssignment(SeatAssignment assignment)') && common.include?('current.playerId() != assignment.playerId()'),
  representative_join_critical_sections: representatives.all? { |path| File.read(File.join(root, path)).match?(/synchronized (?:void|GameCommandResult) (?:join|execute)\(/) }
}
result = { task: 'SEAT03', passed: checks.values.all?, checks: checks, representativeFiles: representatives }
out = File.join(root, 'work/audit/atomic-seat-assignment.json'); FileUtils.mkdir_p(File.dirname(out)); File.write(out, JSON.pretty_generate(result) + "\n")
puts JSON.generate(result); exit(result[:passed] ? 0 : 1)
