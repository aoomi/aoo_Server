#!/usr/bin/env ruby
# frozen_string_literal: true

errors = []
mahjong = File.read('server/Mahjong/src/main/java/com/aoo/bcg/mahjong/MahjongAuthoritativeSession.java')
poker = File.read('server/Poker/src/main/java/com/aoo/bcg/poker/PokerAuthoritativeSession.java')
god = File.read('server/gameServer/src/business/global/mj/set/GodInfo.java')
providers = Dir['server/{CDXZMJ,NJPDK,SCJYMJ,XCPDK,ZJH,ZYPK}/src/**/*GameProvider.java'].map { |p| File.read(p) }.join("\n")

errors << 'Mahjong authority must use auditable random source' unless mahjong.include?('SeededGameRandomSource(seed).shuffle')
errors << 'Poker authority must use auditable random source' unless poker.include?('SeededGameRandomSource(seed).shuffle')
errors << 'room/client configuration must not choose random seed' if providers.match?(/longRule\(context,\s*"randomSeed"/)
errors << 'legacy Mahjong control-card path must remain disabled' unless god.include?('private static boolean controlsEnabled() { return false; }')
errors << 'authoritative commands must not accept wall/deck/seed fields' if (mahjong + poker).match?(/r\.body\(\).*\b(wall|deck|seed|randomSeed)\b/i)

if errors.empty?
  puts 'game-random-boundaries: passed'
else
  warn errors.join("\n")
  exit 1
end
