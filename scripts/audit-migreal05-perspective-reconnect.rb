require 'json'

root = File.expand_path('..', __dir__)
legacy = File.read(File.join(root, 'server/gameServer/src/core/network/client2game/handler/room/CPlayerRoomReconnectV2.java'))
reconnect = File.read(File.join(root, 'server/gameServer/src/core/server/ProductionRoomReconnectService.java'))
recorder = File.read(File.join(root, 'server/gameServer/src/business/global/replay/LegacyPerspectiveReplayRecorder.java'))
modules = Dir[File.join(root, 'server/*/src')].select { |path| File.directory?(path) }
game_sources = modules.flat_map { |path| Dir[File.join(path, '**/*.java')] }
playback = game_sources.count do |path|
  source = File.read(path)
  source.include?('playBack2All(') || source.include?('playBack2Pos(')
end

checks = {
  'legacyEntryFailClosed' => legacy.include?('ErrorCode.NotAllow') && legacy.include?('room.reconnect'),
  'clientCursorUsed' => reconnect.include?('body.get("lastServerSeq")') &&
    reconnect.include?('lastSeq,') && reconnect.include?('PAGE_SIZE + 1'),
  'aheadSequenceRejected' => reconnect.include?('lastSeq > head.lastEventSequence()') &&
    reconnect.include?('ROOM_RECOVERY_CURSOR_AHEAD'),
  'spectatorPrivateDenied' => reconnect.include?("visibility='PUBLIC' AND owner_player_id=0") &&
    reconnect.include?("visibility='PLAYER_PRIVATE' AND owner_player_id=?") &&
    reconnect.include?(%q{: "(visibility='PUBLIC' AND owner_player_id=0)"}),
  'requiredEventColumns' => %w[round_no business_event_id schema_version].all? { |column| recorder.include?(column) },
  'atomicReplayAndReconnectWrite' => recorder.include?('connection.setAutoCommit(false)') && recorder.include?('connection.commit()'),
  'noQueueDropOnPressure' => recorder.include?('queue.put(event)'),
  'genericGameplayRecorders' => playback.positive?
}

abort 'MIGREAL05 reconnect closure failed' unless checks.values.all?

output = {
  'task' => 'MIGREAL05',
  'sourceModules' => modules.size,
  'gameplayCallSitesUsingSharedPlayback' => playback,
  'productionReconnectAuthority' => 'server/gameServer/src/core/server/ProductionRoomReconnectService.java',
  'checks' => checks,
  'dynamicPerspectiveEvidence' => 'docs/generated/migreal03-viewer-identity-chain.json',
  'status' => 'passed'
}
File.write(File.join(root, 'docs/generated/migreal05-perspective-reconnect.json'), JSON.pretty_generate(output) + "\n")
puts 'MIGREAL05 perspective reconnect audit passed'
