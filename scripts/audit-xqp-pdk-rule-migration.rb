#!/usr/bin/env ruby
require 'json'
require 'fileutils'

server_root = File.expand_path('..', __dir__)
ledger_path = File.join(server_root, 'work/audit/room-rules/xqp-pdk-migration-ledger.json')
abort('缺少XQP跑得快逐条迁移台账') unless File.file?(ledger_path)
ledger = JSON.parse(File.read(ledger_path, encoding: 'UTF-8'))
rules = ledger.fetch('rules')
ids = rules.map { |rule| rule.fetch('sourceRuleId') }
abort('XQP源规则必须逐条覆盖500001..500135') unless ids == (500001..500135).to_a
workbook = ledger.fetch('sourceWorkbookAudit')
abort('XQP源Excel未完成全工作表解析') unless workbook.fetch('sheets') == ['chessRule_50']
abort('XQP源Excel与导出表不一致') unless workbook.fetch('exportMatchesExcel')
server_binary = ledger.fetch('sourceServerBinaryAudit')
required_server_classes = %w[
  ww/chess/room/Room.class ww/chess/room/RoomRule.class
  ww/chess/room/RoomHandler.class ww/chess/room/RoomRole.class
  ww/chess/pdk/PDKRoom.class ww/chess/pdk/PDKPlayRule.class ww/chess/JinHua.class
]
abort('XQP源服务端二进制未完成不可变取证') unless
  server_binary.fetch('gitCommit').match?(/\A[0-9a-f]{40}\z/) &&
  server_binary.fetch('sha256').match?(/\A[0-9a-f]{64}\z/) &&
  required_server_classes.all? { |entry| server_binary.fetch('classesVerified').include?(entry) }

migrated = rules.select { |rule| rule.fetch('status').start_with?('已迁移') }
evidenced = rules.select do |rule|
  rule.fetch('status').start_with?('已迁移') || rule.fetch('status').start_with?('已取证') ||
    rule.fetch('status').start_with?('阻塞')
end
blocked = rules.select { |rule| rule.fetch('status').start_with?('阻塞') }
abort('迁移计数与台账不一致') unless ledger.fetch('migratedRules') == migrated.length &&
        ledger.fetch('rulesWithCodeEvidence') == evidenced.length &&
        ledger.fetch('blockedRules') == blocked.length
abort('已迁移规则缺少源代码证据') unless migrated.all? do |rule|
  !rule.fetch('sourceCodeEvidence').empty? ||
    (rule.fetch('sourceType') == 60 && rule.fetch('businessSemantics').include?('未消费该样本'))
end

implementation_files = %w[
  PaoDeKuaiRuleSet.java PdkAdvancedRules.java PdkPublishedRuleOptions.java
  PdkInitialHandEvaluator.java PdkScoringPolicy.java PokerAuthoritativeSession.java
]
implementation = implementation_files.map { |name| File.read(File.join(server_root,
        'server/Poker/src/main/java/com/aoo/bcg/poker', name), encoding: 'UTF-8') }.join("\n")
test_files = %w[PdkPublishedRuleOptionsTest.java PdkXqpEquivalentRulesTest.java]
tests = test_files.map { |name| File.read(File.join(server_root,
        'server/Poker/src/test/java/com/aoo/bcg/poker', name), encoding: 'UTF-8') }.join("\n")
required_tokens = %w[tripleWithoutAttachmentTiming tripleAttachmentMode airplaneAttachmentMode
        fourAttachmentMode compareTripleAttachments forceHighestPairAgainstReportedPair
        minimumStraightLength minimumPairRunLength allowSingle allowPair cardsPerPlayer
        deckCards allowConsecutiveBomb specialTripleBombRanks playedCardVisibility
        baseScore requiredFirstCardRounds bankerSelectionCard
        handScoreTable bombScoreMode initialHandPatterns directWinPatterns
        operationTimeoutSeconds hostingMissThreshold competeDealer autoReady
        declaredRoundTimeoutSeconds textChatEnabled settlementPresentation
        distanceWarningEnabled interactionEnabled offlineDissolveSeconds
        gpsAdmissionRequired gpsMinimumDistanceMeters uniqueIpRequired entryMode]
missing = required_tokens.reject { |token| implementation.include?(token) && tests.include?(token) }
abort("Aoo已取证策略缺少实现或测试: #{missing.join('、')}") unless missing.empty?
%w[spring reverseSpring].each do |prefix|
  abort("Aoo计分策略缺少#{prefix}解析或测试") unless
    implementation.include?("scoreRule(rules, \"#{prefix}\"") &&
    tests.include?("\"#{prefix}Mode\"")
end
billing = File.read(File.join(server_root,
        'server/Bootstrap/src/main/java/com/aoo/bcg/bootstrap/JdbcRoomSagaBillingPort.java'),
        encoding: 'UTF-8')
billing_test = File.read(File.join(server_root,
        'server/Bootstrap/src/test/java/com/aoo/bcg/bootstrap/JdbcRoomSagaBillingPortTest.java'),
        encoding: 'UTF-8')
abort('XQP房主支付缺少持久化实现或幂等测试') unless
  billing.include?('ROOM_CREATE_RESERVE') && billing.include?('JdbcBillingService') &&
  billing_test.include?('ownerPaymentUsesDurablePolicyLedgerAndReservation')

report = {
  'sourceRules'=>rules.length, 'sourceTypes'=>ledger.fetch('ruleTypes').length,
  'sourceWorkbook'=>workbook, 'sourceServerBinary'=>server_binary,
  'codeEvidencedRules'=>evidenced.length,
  'configurationEvidencedRules'=>ledger.fetch('rulesWithConfigurationEvidence'),
  'migratedRules'=>migrated.length, 'blockedRules'=>blocked.length,
  'provenButNotMigratedRules'=>evidenced.length - migrated.length,
  'complete'=>migrated.length == rules.length,
  'coverageComplete'=>migrated.length + blocked.length == rules.length,
  'failClosedRuleIds'=>rules.reject { |rule| rule.fetch('status').start_with?('已迁移') }
          .map { |rule| rule.fetch('sourceRuleId') }
}
output = File.join(server_root, 'work/audit/room-rules/xqp-pdk-migration-audit.json')
FileUtils.mkdir_p(File.dirname(output))
File.write(output, JSON.pretty_generate(report) + "\n", encoding: 'UTF-8')
puts "XQP跑得快迁移稽查通过: 覆盖#{rules.length}条，已迁移#{migrated.length}条，明确阻塞#{blocked.length}条"
