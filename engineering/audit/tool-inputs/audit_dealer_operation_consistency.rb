#!/usr/bin/env ruby
# frozen_string_literal: true
require 'json'; require 'fileutils'; require 'open3'
root = File.expand_path('../../..', __dir__); jdk = File.expand_path('../.toolchains/jdk-26.0.2.1.jdk/Contents/Home', root)
stdout, stderr, status = Open3.capture3({ 'JAVA_HOME' => jdk, 'PATH' => "#{jdk}/bin:#{ENV['PATH']}" }, './mvnw', '-q',
  '-pl', 'server/CommonRoom,server/Games/Mahjong,server/Games/Poker,server/Games/LongCard,server/Games/LongCard/Compatibility/WordCard,server/Games/Poker/ZYPK', '-am',
  '-Dsurefire.failIfNoSpecifiedTests=false', 'test', chdir: root)
files = {
  common: 'server/CommonRoom/src/main/java/com/aoo/bcg/common/turn/RoundSeatAuthority.java',
  mahjong: 'server/Games/Mahjong/src/main/java/com/aoo/bcg/mahjong/MahjongState.java',
  poker: 'server/Games/Poker/src/main/java/com/aoo/bcg/poker/PokerTurnState.java',
  long_card: 'server/Games/LongCard/src/main/java/com/aoo/bcg/longcard/LongCardState.java',
  word_card: 'server/Games/LongCard/Compatibility/WordCard/src/main/java/com/aoo/bcg/wordcard/WordCardState.java',
  zypk: 'server/Games/Poker/ZYPK/src/business/global/pk/zypk/ZYPKTable.java'
}
checks = { categories_present: files.values.all? { |path| File.file?(File.join(root, path)) }, tests_passed: status.success? }
common = File.read(File.join(root, files[:common])); checks[:deal_from_dealer] = common.include?('operationSeat = dealerSeat')
checks[:turn_actor_guarded] = common.include?('actorSeat != operationSeat'); checks[:settlement_rotates_dealer] = common.include?('nextRound(int nextDealerSeat)')
checks[:recovery_supported] = common.include?('RoundSeatAuthority restore(')
result = { task: 'SEAT06', passed: checks.values.all?, checks: checks, categoryEvidence: files, testStdout: stdout.strip, testStderr: stderr.strip }
out = File.join(root, 'build-only/audit/dealer-operation-consistency.json'); FileUtils.mkdir_p(File.dirname(out)); File.write(out, JSON.pretty_generate(result) + "\n")
puts JSON.generate(result); exit(result[:passed] ? 0 : 1)
