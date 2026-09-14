#!/usr/bin/env ruby
# frozen_string_literal: true

errors = []

def source(path)
  File.read(path, encoding: 'UTF-8', invalid: :replace, undef: :replace)
end

nj = source('server/NJPDK/src/business/global/pk/njpdk/NJPDKRoomSet.java')
xc = source('server/XCPDK/src/business/global/pk/xcpdk/XCPDKRoomSet.java')
zypk = source('server/ZYPK/src/business/global/pk/zypk/ZYPKRoomPos.java')
replay = source('server/gameServer/src/business/global/room/base/RoomPlayBackImplAbstract.java')

errors << 'NJPDK SetEnd must not use public replay' if nj.match?(/playBack2All\s*\(\s*SNJPDK_SetEnd/)
errors << 'XCPDK SetEnd must not use public replay' if xc.match?(/playBack2All\s*\(\s*SXCPDK_SetEnd/)
errors << 'NJPDK settlement must create viewer card views' unless nj.include?('cardViewFor(viewerPos)')
errors << 'XCPDK settlement must create viewer card views' unless xc.include?('cardViewFor(viewerPos)')
errors << 'NJPDK reveal must bind authenticated player' unless nj.include?('roomPos.getPid() != authenticatedPid')
errors << 'XCPDK must not auto-reveal every hand after settlement' if xc.match?(/GAME_STATUS_RESULT[\s\S]{0,1200}for \(int i = 0; i < this\.room\.getPlayerNum\(\); i\+\+\)[\s\S]{0,300}onOpenCard/)
errors << 'ZYPK hidden cards must use zero placeholders' unless zypk.include?('isSelf ? cardByte : 0x00')
errors << 'targeted replay must remain private' unless replay.include?('recordPrivate(') &&
                                                    replay.include?('Targeted messages may contain private cards')

if errors.empty?
  puts 'hidden-card-boundaries: passed'
else
  warn errors.join("\n")
  exit 1
end
