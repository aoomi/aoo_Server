#!/usr/bin/env ruby
# frozen_string_literal: true

require 'json'
require 'time'

root = File.expand_path('..', __dir__)
baseline = JSON.parse(File.read(File.join(root, 'work/audit/audit-source-baseline.json'), encoding: 'UTF-8'))
mapping = JSON.parse(File.read(File.join(root, 'work/audit/database-runtime-mapping.json'), encoding: 'UTF-8')).fetch('mappings')
output_path = File.join(root, 'work/audit/common-room-capability-placement.json')
task_path = File.join(root, 'docs/Aoo-前后端全框架功能通信审计任务清单.md')

codes = mapping.reject { |game| game['category'] == 'INFRASTRUCTURE' }.map { |game| game.fetch('code').downcase }.uniq
capabilities = {
  'joinSeat' => /(?:joinRoom|enterRoom|joinSeat|sitDown|seatPlayer|addPlayer)/i,
  'ready' => /(?:playerReady|setReady|readyState|roomReady)/i,
  'chat' => /(?:quickChat|roomChat|chatMessage|voiceMessage|magicEmoji|magicExpression)/i,
  'dissolve' => /(?:dissolveRoom|disbandRoom|roomDissolve|voteDissolve|applyDissolve)/i,
  'reconnect' => /(?:reconnect|resumeRoom|restoreRoom|reconnectToken|lastSeq)/i,
  'record' => /(?:gameRecord|battleRecord|roomRecord|historyRecord)/i,
  'replay' => /(?:gameReplay|playBack|replayEvent|replayRecord)/i
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
  shared_layer = relative.match?(%r{server/(?:GameCommon|GameSPI|Families|Gateway|Bootstrap|gameServer|gameHall)/|modules/(?:room|club|record|replay|common)/}i)
  files << {path: relative, capabilities: kinds, gameCodes: game_codes, sharedLayer: shared_layer,
            gameSpecificCapabilityCandidate: !shared_layer && !game_codes.empty?}
end

summary = {
  files: files.size,
  sharedLayerFiles: files.count { |file| file[:sharedLayer] },
  gameSpecificFiles: files.count { |file| file[:gameSpecificCapabilityCandidate] },
  affectedGames: files.select { |file| file[:gameSpecificCapabilityCandidate] }.flat_map { |file| file[:gameCodes] }.uniq.size,
  capabilityCounts: capabilities.keys.to_h { |kind| [kind, files.count { |file| file[:capabilities].include?(kind) }] },
  gameSpecificCapabilityCounts: capabilities.keys.to_h do |kind|
    [kind, files.count { |file| file[:gameSpecificCapabilityCandidate] && file[:capabilities].include?(kind) }]
  end
}
report = {
  schemaVersion: 1, generatedAt: Time.now.utc.iso8601, summary: summary,
  invariant: 'Join/seat, ready, chat, dissolve, reconnect, record and replay lifecycle belong to shared room services; game modules expose hooks only.',
  files: files.sort_by { |file| file[:path] },
  limitations: ['Static placement cannot prove runtime delegation; dependency traces and contract tests must show game modules invoke shared services without duplicate state.']
}
File.write(output_path, JSON.pretty_generate(report) + "\n", mode: 'w:UTF-8')

task = File.read(task_path, encoding: 'UTF-8')
row = "| CLASS05 | 未完成 | 已建归位门禁·待整改 | 已盘点入座/准备/聊天/解散/重连/战绩/回放通用房间能力；候选文件=#{summary[:files]}、公共层=#{summary[:sharedLayerFiles]}、具体玩法内候选=#{summary[:gameSpecificFiles]}、涉及玩法=#{summary[:affectedGames]}；具体玩法仅保留 hook、无重复状态且运行委托合同通过后方可闭合。 证据：work/audit/common-room-capability-placement.json；工具：tools/audit_common_room_capability_placement.rb |"
task.sub!(/^\| CLASS05 \|.*$/, row) or abort 'CLASS05 row not found'
task.sub!(/^下一项：.*$/, '下一项：CLASS06 通用牌局能力归位') or abort 'next pointer not found'
File.write(task_path, task, mode: 'w:UTF-8')
puts JSON.generate(summary)
