#!/usr/bin/env ruby
# frozen_string_literal: true

errors = []
mahjong = File.read('server/Games/Mahjong/src/main/java/com/aoo/bcg/mahjong/MahjongAuthoritativeSession.java')
poker = File.read('server/Games/Poker/src/main/java/com/aoo/bcg/poker/PokerAuthoritativeSession.java')
god = File.read('server/gameServer/src/business/global/mj/set/GodInfo.java')
provider_paths = %w[Games/Mahjong/CDXZMJ Games/Mahjong/SCJYMJ Games/Poker/PDK Games/Poker/ZJH Games/Poker/ZYPK]
providers = provider_paths.flat_map { |path| Dir["server/#{path}/src/**/*GameProvider.java"] }.map { |p| File.read(p) }.join("\n")
poker_catalog = File.read('server/Games/Poker/src/main/java/com/aoo/bcg/poker/PokerCatalogRuntimeRegistry.java')

errors << 'Mahjong authority must use auditable random source' unless mahjong.include?('SeededGameRandomSource(seed).shuffle')
errors << 'Poker authority must use a persisted per-deal auditable random source' unless
  poker.include?('SeededGameRandomSource(currentRoundSeed).shuffle') &&
    poker.include?('o.put("currentRoundSeed", currentRoundSeed)') &&
    poker.include?('o.put("shuffleSequence", shuffleSequence)')
errors << 'room/client configuration must not choose random seed' if providers.match?(/longRule\(context,\s*"randomSeed"/)
errors << 'Poker room/client configuration must not choose shuffle seed' if
  poker_catalog.match?(/publishedRules\.get\("shuffleSeed"\)/)
errors << 'Poker production provider must create a server-owned secure seed' unless
  poker_catalog.include?('SeededGameRandomSource.create().seed()')
errors << 'legacy Mahjong control-card path must remain disabled' unless god.include?('private static boolean controlsEnabled() { return false; }')
errors << 'authoritative commands must not accept wall/deck/seed fields' if (mahjong + poker).match?(/r\.body\(\).*\b(wall|deck|seed|randomSeed)\b/i)

if errors.empty?
  puts 'game-random-boundaries: passed'
else
  warn errors.join("\n")
  exit 1
end
