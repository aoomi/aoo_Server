#!/usr/bin/env ruby
# frozen_string_literal: true

require 'json'
require 'time'

root = File.expand_path('..', __dir__)
baseline = JSON.parse(File.read(File.join(root, 'work/audit/audit-source-baseline.json'), encoding: 'UTF-8'))
mapping = JSON.parse(File.read(File.join(root, 'work/audit/database-runtime-mapping.json'), encoding: 'UTF-8')).fetch('mappings')
output_path = File.join(root, 'work/audit/common-game-lifecycle-placement.json')
task_path = File.join(root, 'docs/Aoo-前后端全框架功能通信审计任务清单.md')

codes = mapping.reject { |game| game['category'] == 'INFRASTRUCTURE' }.map { |game| game.fetch('code').downcase }.uniq
capabilities = {
  'operationWindow' => /(?:OperationWindow|ActionWindow|CandidateWindow|AllowedOperations|PendingAction)/i,
  'countdown' => /(?:Countdown|OperationTimer|TurnTimer|Deadline|TimeoutScheduler|scheduleTimeout)/i,
  'event' => /(?:GameEvent|DomainEvent|EventStore|EventPublisher|appendEvent)/i,
  'snapshot' => /(?:GameSnapshot|RoomSnapshot|StateSnapshot|SnapshotStore|authoritativeState)/i,
  'settlement' => /(?:Settlement|Settle|RoundResult|FinalResult|ScoreResult)/i,
  'lifecycle' => /(?:GameLifecycle|RoundLifecycle|RoomLifecycle|startRound|endRound|finishRound)/i
}.freeze

files = []
baseline.fetch('files').each do |item|
  path = item.fetch('path')
  next unless path.start_with?(File.join(root, 'modules') + '/', File.join(root, 'server') + '/')
  next unless File.file?(path) && %w[.java .kt].include?(File.extname(path).downcase) && item.fetch('size') <= 2_000_000
  text = File.binread(path).force_encoding('UTF-8').scrub
  kinds = capabilities.each_with_object([]) { |(kind, regex), found| found << kind if text.match?(regex) }
  next if kinds.empty?
  relative = path.delete_prefix(root + '/')
  lower = relative.downcase
  game_codes = codes.select { |code| lower.match?(/(?<![a-z0-9_])#{Regexp.escape(code)}(?![a-z0-9_])/) }.first(20)
  common_layer = relative.match?(%r{server/(?:GameCommon|GameSPI|Families|Gateway|Bootstrap|Billing|ConfigCenter)/|modules/(?:game|room|event|snapshot|settlement|common)/}i)
  files << {path: relative, capabilities: kinds, gameCodes: game_codes, commonLayer: common_layer,
            gameSpecificLifecycleCandidate: !common_layer && !game_codes.empty?}
end

summary = {
  files: files.size,
  commonLayerFiles: files.count { |file| file[:commonLayer] },
  gameSpecificFiles: files.count { |file| file[:gameSpecificLifecycleCandidate] },
  affectedGames: files.select { |file| file[:gameSpecificLifecycleCandidate] }.flat_map { |file| file[:gameCodes] }.uniq.size,
  capabilityCounts: capabilities.keys.to_h { |kind| [kind, files.count { |file| file[:capabilities].include?(kind) }] },
  gameSpecificCapabilityCounts: capabilities.keys.to_h do |kind|
    [kind, files.count { |file| file[:gameSpecificLifecycleCandidate] && file[:capabilities].include?(kind) }]
  end
}
report = {
  schemaVersion: 1, generatedAt: Time.now.utc.iso8601, summary: summary,
  invariant: 'Operation windows, countdown scheduling, event envelope/store, snapshot lifecycle and settlement orchestration belong to common game infrastructure; game modules supply rules and payloads.',
  files: files.sort_by { |file| file[:path] },
  limitations: ['Static placement does not prove lifecycle ownership; architecture dependency tests and runtime orchestration traces remain required.']
}
File.write(output_path, JSON.pretty_generate(report) + "\n", mode: 'w:UTF-8')

task = File.read(task_path, encoding: 'UTF-8')
row = "| CLASS06 | 未完成 | 已建归位门禁·待整改 | 已盘点操作窗口/倒计时/事件/快照/结算/生命周期通用牌局能力；候选文件=#{summary[:files]}、公共层=#{summary[:commonLayerFiles]}、具体玩法内候选=#{summary[:gameSpecificFiles]}、涉及玩法=#{summary[:affectedGames]}；需达公共层统一编排且玩法只提供规则/载荷才能闭合。 证据：work/audit/common-game-lifecycle-placement.json；工具：tools/audit_common_game_lifecycle_placement.rb |"
task.sub!(/^\| CLASS06 \|.*$/, row) or abort 'CLASS06 row not found'
task.sub!(/^下一项：.*$/, '下一项：CLASS07 牌类能力归位') or abort 'next pointer not found'
File.write(task_path, task, mode: 'w:UTF-8')
puts JSON.generate(summary)
