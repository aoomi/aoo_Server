#!/usr/bin/env ruby
require 'json'
require 'open3'
root = File.expand_path('..', __dir__)
targets = {
  'round' => 'server/GameCommon/src/main/java/com/aoo/bcg/common/room/AuthoritativeRoom.java',
  'seat_revision' => 'server/GameCommon/src/main/java/com/aoo/bcg/common/turn/RoundSeatAuthority.java',
  'ownership_revision' => 'server/GameCommon/src/main/java/com/aoo/bcg/common/room/RoomOwnership.java',
  'outbox_attempt' => 'server/GameCommon/src/main/java/com/aoo/bcg/common/event/InMemoryOutboxRepository.java',
  'ledger_id' => 'server/Billing/src/main/java/com/aoo/bcg/billing/JdbcLedgerRepository.java',
  'settlement_id' => 'server/GameCommon/src/main/java/com/aoo/bcg/common/settlement/JdbcSettlementRepository.java'
}
coverage = targets.to_h { |name, path| [name, File.read(File.join(root, path)).include?('ExactDomainMath')] }
settlement = File.read(File.join(root, 'server/GameCommon/src/main/java/com/aoo/bcg/common/settlement/SettlementValidator.java'))
zypk = File.read(File.join(root, 'server/ZYPK/src/business/global/pk/zypk/ZYPKTable.java'))
java_home = File.expand_path('../.toolchains/jdk-26.0.2.1.jdk/Contents/Home', root)
env = {'JAVA_HOME' => java_home, 'PATH' => "#{java_home}/bin:#{ENV['PATH']}"}
stdout, stderr, status = Open3.capture3(env, './mvnw', '-pl', 'server/ZJH,server/ZYPK,server/Billing', '-am', 'test', '-q', chdir: root)
checks = {'counter_and_id_coverage' => coverage.values.all?,
          'settlement_uses_exact_sum' => settlement.include?('Math.addExact'),
          'wager_and_chip_math_exact' => zypk.include?('Math.multiplyExact') && zypk.include?('Math.subtractExact'),
          'overflow_tests_passed' => status.success?}
evidence = {'task' => 'TYPE08', 'passed' => checks.values.all?, 'checks' => checks,
            'coverage' => coverage, 'buildStdout' => stdout, 'buildStderr' => stderr}
audit = File.join(root, 'work/audit'); Dir.mkdir(File.join(root, 'work')) unless Dir.exist?(File.join(root, 'work')); Dir.mkdir(audit) unless Dir.exist?(audit)
File.write(File.join(audit, 'exact-domain-arithmetic.json'), JSON.pretty_generate(evidence))
puts JSON.generate(evidence); exit(evidence['passed'] ? 0 : 1)
