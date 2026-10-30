#!/usr/bin/env ruby
# frozen_string_literal: true

errors = []
read = ->(path) { File.read(path, encoding: 'UTF-8', invalid: :replace, undef: :replace) }

player = read.call('server/gameServer/src/core/network/client2game/handler/PlayerHandler.java')
dispatcher = read.call('server/gameServer/src/core/network/client2game/ClientHandlerDispatcher.java')
session = read.call('server/LegacyCommon/src/com/ddm/server/websocket/BaseSession.java')
mina = read.call('server/LegacyCommon/src/com/ddm/server/websocket/BaseIoHandler.java')
gateway = read.call('server/Gateway/src/main/java/com/aoo/bcg/gateway/GameWebSocketRouter.java')
guard = read.call('server/Gateway/src/main/java/com/aoo/bcg/gateway/WebSocketRequestGuard.java')

errors << 'legacy writes must validate replay envelope' unless player.include?('LegacyReplayGuard.validate(')
errors << 'legacy writes must validate room and actor identity' unless player.include?('LegacyGameRequestGuard.validate(')
errors << 'all three legacy transports must hard-reject invalid sequence' unless dispatcher.scan(/checkSequenceException/).size == 3 && dispatcher.scan(/return -3;/).size == 3
errors << 'legacy sequence must reject rollback' unless session.include?('return this.curSequence == unsignedSequence;')
errors << 'gateway must persist requestId idempotency' unless gateway.include?('idempotency.acquire(') && gateway.include?('idempotency.save(')
errors << 'gateway must validate timestamp' unless guard.include?('request timestamp outside accepted window')
errors << 'legacy MINA websocket must enforce exact Origin allow-list' unless mina.include?('isAllowedWebSocketOrigin') &&
                                                                       mina.include?('HTTP/1.1 403 Forbidden')

if errors.empty?
  puts 'gameplay-security-boundaries: passed'
else
  warn errors.join("\n")
  exit 1
end
