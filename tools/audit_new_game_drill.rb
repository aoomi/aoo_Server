#!/usr/bin/env ruby
# frozen_string_literal: true

require 'json'
require 'time'

root = File.expand_path('..', __dir__)
code = ENV.fetch('AOO_DRILL_GAME', 'xcpdk').downcase
module_name = code.upcase
module_root = File.join(root, 'server', module_name)
provider_path = Dir.glob(File.join(module_root, '**', '*GameProvider.java')).reject { |path| path.include?('/test/') }.first
test_paths = Dir.glob(File.join(module_root, '**', '*Test.java')).reject { |path| path.include?('/target/') }
service_path = File.join(module_root, 'src/main/resources/META-INF/services/com.aoo.bcg.gamespi.GameProvider')
classification_path = File.join(root, 'work/generated/classification/game-classification.json')
output_path = File.join(root, 'work/audit/new-game-drill.json')
task_path = File.join(root, 'docs/Aoo-前后端全框架功能通信审计任务清单.md')

provider_text = provider_path && File.file?(provider_path) ? File.read(provider_path, encoding: 'UTF-8') : ''
test_text = test_paths.map { |path| File.read(path, encoding: 'UTF-8') }.join("\n")
classification = File.file?(classification_path) ? JSON.parse(File.read(classification_path, encoding: 'UTF-8')) : {'rows' => []}
catalog_row = classification.fetch('rows').find { |row| row['code'] == code }
surefire_reports = Dir.glob(File.join(module_root, 'target/surefire-reports/TEST-*.xml'))
test_exit = ENV['AOO_DRILL_TEST_EXIT']&.to_i

stages = {
  moduleBuildEntry: File.file?(File.join(module_root, 'pom.xml')),
  providerMetadata: provider_text.match?(/new\s+GameDescriptor\s*\(/),
  categoryContract: provider_text.match?(/implements\s+(?:Mahjong|Poker|LongCard|WordCard)GameProvider/),
  serviceRegistration: File.file?(service_path) && File.read(service_path, encoding: 'UTF-8').include?('GameProvider'),
  generatedDatabaseSeed: !catalog_row.nil?,
  authoritativeSessionFactory: provider_text.match?(/createAuthoritativeSession\s*\(/),
  roomFactory: provider_text.match?(/roomFactory\s*\(/),
  roomCreationSuccessTest: test_text.match?(/assertDoesNotThrow[^;]*roomFactory\(\)\.create|GameRoomHandle\s+\w+\s*=\s*[^;]*roomFactory\(\)\.create/m),
  registrationTest: test_text.match?(/GameRegistry|ServiceLoader/),
  buildAndTestsPassed: test_exit == 0 && !surefire_reports.empty?,
  coreUntouchedProof: false
}
summary = {
  game: code, passedStages: stages.count { |_name, passed| passed }, totalStages: stages.size,
  failedStages: stages.select { |_name, passed| !passed }.keys,
  testExit: test_exit, surefireReports: surefire_reports.map { |path| path.delete_prefix(root + '/') }
}
report = {
  schemaVersion: 1, generatedAt: Time.now.utc.iso8601, representativeGame: code,
  stages: stages, summary: summary,
  evidence: {moduleRoot: module_root, provider: provider_path, tests: test_paths, serviceRegistration: service_path, catalogRow: catalog_row},
  invariant: 'A representative game must build, register, seed schema/config, create a real room and pass tests without modifying core modules.',
  limitations: ['No immutable before/after core baseline proves core modules were untouched; a successful room creation test is currently absent.']
}
File.write(output_path, JSON.pretty_generate(report) + "\n", mode: 'w:UTF-8')

task = File.read(task_path, encoding: 'UTF-8')
row = "| CLASS12 | 未完成 | 演练未通过 | 已以 #{code} 作为代表玩法执行模块/Provider/分类/注册/建房/测试演练；通过=#{summary[:passedStages]}/#{summary[:totalStages]}、失败阶段=#{summary[:failedStages].join(',')}、测试退出=#{test_exit.inspect}；缺真实成功建房测试及核心层未修改的不可变基线，不得闭合。 证据：work/audit/new-game-drill.json |"
task.sub!(/^\| CLASS12 \|.*$/, row) or abort 'CLASS12 row not found'
task.sub!(/^下一项：.*$/, '下一项：REF01 玩法外键与运行注册完整性') or abort 'next pointer not found'
File.write(task_path, task, mode: 'w:UTF-8')
puts JSON.generate(summary)
