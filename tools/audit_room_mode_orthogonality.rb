#!/usr/bin/env ruby
# frozen_string_literal: true

require 'json'
require 'time'

root = File.expand_path('..', __dir__)
baseline = JSON.parse(File.read(File.join(root, 'work/audit/audit-source-baseline.json'), encoding: 'UTF-8'))
mapping = JSON.parse(File.read(File.join(root, 'work/audit/database-runtime-mapping.json'), encoding: 'UTF-8')).fetch('mappings')
output_path = File.join(root, 'work/audit/room-mode-orthogonality.json')
task_path = File.join(root, 'docs/Aoo-前后端全框架功能通信审计任务清单.md')

codes = mapping.reject { |game| game['category'] == 'INFRASTRUCTURE' }.map { |game| game.fetch('code').downcase }.uniq
mode_patterns = {
  'normalRoom' => /(?:normalRoom|privateRoom|friendRoom|roomType\s*==\s*0)/i,
  'clubRoom' => /(?:clubRoom|clubId|unionId|familyRoom|guildRoom|clubGame)/i,
  'goldRoom' => /(?:goldRoom|goldField|goldMatch|currencyRoom|coinRoom)/i,
  'tournament' => /(?:tournament|competition|matchRoom|arena|qualifier)/i
}.freeze

files = []
baseline.fetch('files').each do |item|
  path = item.fetch('path')
  next unless path.start_with?(File.join(root, 'modules') + '/', File.join(root, 'server') + '/')
  next unless File.file?(path) && %w[.java .kt].include?(File.extname(path).downcase) && item.fetch('size') <= 2_000_000
  text = File.binread(path).force_encoding('UTF-8').scrub
  modes = mode_patterns.each_with_object([]) { |(mode, regex), found| found << mode if text.match?(regex) }
  next if modes.empty?
  relative = path.delete_prefix(root + '/')
  lower = relative.downcase
  game_codes = codes.select { |code| lower.match?(/(?<![a-z0-9_])#{Regexp.escape(code)}(?![a-z0-9_])/) }.first(20)
  shared_layer = relative.match?(%r{server/(?:GameCommon|GameSPI|Families|Gateway|ConfigCenter|Billing)/|modules/(?:room|club|match|tournament|common)/}i)
  lines = []
  text.lines.each_with_index do |line, index|
    line_modes = mode_patterns.each_with_object([]) { |(mode, regex), found| found << mode if line.match?(regex) }
    next if line_modes.empty?
    lines << {line: index + 1, modes: line_modes, excerpt: line.strip[0, 300]}
    break if lines.size >= 80
  end
  files << {path: relative, modes: modes, gameCodes: game_codes, sharedLayer: shared_layer,
            gameSpecificModeCandidate: !shared_layer && !game_codes.empty?, matches: lines}
end

summary = {
  filesWithRoomModes: files.size,
  sharedLayerFiles: files.count { |file| file[:sharedLayer] },
  gameSpecificModeFiles: files.count { |file| file[:gameSpecificModeCandidate] },
  affectedGames: files.select { |file| file[:gameSpecificModeCandidate] }.flat_map { |file| file[:gameCodes] }.uniq.size,
  modeFileCounts: mode_patterns.keys.to_h { |mode| [mode, files.count { |file| file[:modes].include?(mode) }] }
}
report = {
  schemaVersion: 1, generatedAt: Time.now.utc.iso8601, summary: summary,
  invariant: 'Normal, club, gold and tournament rooms are orthogonal mode capabilities; game modules may consume mode context but may not duplicate mode lifecycle or infrastructure.',
  files: files.sort_by { |file| file[:path] },
  limitations: ['Static token presence is candidate evidence; dependency and runtime lifecycle traces must prove mode services are injected rather than reimplemented.']
}
File.write(output_path, JSON.pretty_generate(report) + "\n", mode: 'w:UTF-8')

task = File.read(task_path, encoding: 'UTF-8')
row = "| CLASS04 | 未完成 | 已建正交性门禁·待整改 | 已对普通房/亲友圈/金币场/赛事模式建立静态正交性清册；模式文件=#{summary[:filesWithRoomModes]}、公共层=#{summary[:sharedLayerFiles]}、具体玩法内模式候选=#{summary[:gameSpecificModeFiles]}、涉及玩法=#{summary[:affectedGames]}；需达具体玩法不复制模式生命周期且运行依赖注入可证明才能闭合。 证据：work/audit/room-mode-orthogonality.json；工具：tools/audit_room_mode_orthogonality.rb |"
task.sub!(/^\| CLASS04 \|.*$/, row) or abort 'CLASS04 row not found'
task.sub!(/^下一项：.*$/, '下一项：CLASS05 通用房间能力归位') or abort 'next pointer not found'
File.write(task_path, task, mode: 'w:UTF-8')
puts JSON.generate(summary)
