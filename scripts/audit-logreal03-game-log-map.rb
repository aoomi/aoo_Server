#!/usr/bin/env ruby
require 'json'
require 'time'
require 'fileutils'

ROOT = File.expand_path('..', __dir__)
OUTPUT = File.join(ROOT, 'docs/generated/runtime-game-log-ledger.json')

PLAY = {
  'cdxzmj' => {
    catalogGameId: 517, runtimeGameIds: [516], displayName: '成都血战麻将',
    category: 'MAHJONG', family: 'mahjong-xue-zhan', region: '四川省成都市',
    sourceModule: 'server/CDXZMJ'
  },
  'njpdk' => {
    catalogGameId: 629, runtimeGameIds: [629], displayName: '内江跑得快',
    category: 'POKER', family: 'poker-paodekuai', region: '四川省内江市',
    sourceModule: 'server/NJPDK'
  }
}.freeze

PREFERRED = {
  'game516' => 'cdxzmj',
  'game7103' => 'njpdk',
  'game990051' => 'njpdk',
  'game990516' => 'cdxzmj',
  'game990629' => 'njpdk'
}.freeze

directories = Dir.glob(File.join(ROOT, 'logs/game*')).select { |path| File.directory?(path) }.sort
entries = directories.map do |directory|
  name = File.basename(directory)
  namespaces = Hash.new(0)
  bindings = []
  startup_configs = []
  files = Dir.glob(File.join(directory, '**/*')).select { |path| File.file?(path) }.sort
  files.each do |file|
    File.foreach(file, encoding: 'UTF-8', invalid: :replace, undef: :replace) do |line|
      PLAY.each_key { |code| namespaces[code] += 1 if line.match?(/(?:event:|Event：).*\b#{code}\./i) }
      if (match = line.match(/Unified authority bound roomId:(\d+), gameId:(\d+), provider:(\w+)/))
        bindings << { roomId: match[1].to_i, gameId: match[2].to_i, provider: match[3].downcase }
      end
      if (match = line.match(/启动参数:\[(.+?)\]/))
        startup_configs << match[1].delete_prefix(ROOT + '/')
      end
    end
  end
  bindings.uniq!
  startup_configs.uniq!
  code = bindings.map { |row| row[:provider] }.find { |provider| PLAY.key?(provider) } || PREFERRED.fetch(name)
  play = PLAY.fetch(code)
  confidence = if bindings.any? { |row| row[:provider] == code }
                 'authoritative-runtime-binding'
               elsif startup_configs.any? { |path| path.include?(code) }
                 'startup-config-binding'
               elsif name.end_with?(play[:catalogGameId].to_s) && namespaces[code].positive?
                 'directory-id-and-namespace-binding'
               else
                 'namespace-binding'
               end
  {
    logDirectory: "logs/#{name}", runtimeServerId: name.delete_prefix('game').to_i,
    catalogGameId: play[:catalogGameId], runtimeGameIds: play[:runtimeGameIds], code: code,
    displayName: play[:displayName], category: play[:category], family: play[:family],
    region: play[:region], sourceModule: play[:sourceModule], confidence: confidence,
    evidence: { authoritativeBindings: bindings.first(20), startupConfigs: startup_configs,
                protocolNamespaceCounts: namespaces.sort.to_h }
  }
end

missing = PREFERRED.keys - entries.map { |row| File.basename(row[:logDirectory]) }
abort "missing expected game log directories: #{missing.join(', ')}" unless missing.empty?
abort 'unmapped game log directory' unless entries.all? { |row| row[:catalogGameId] && row[:sourceModule] && row[:region] }

report = {
  schemaVersion: 1, generatedAt: Time.now.utc.iso8601,
  policy: 'Runtime server IDs, catalog game IDs and legacy runtime game IDs are distinct fields. Protocol registration alone is not treated as proof when an authoritative room binding or startup config exists.',
  summary: { directories: entries.length, mapped: entries.length, unresolved: 0 }, entries: entries
}
FileUtils.mkdir_p(File.dirname(OUTPUT))
File.write(OUTPUT, JSON.pretty_generate(report) + "\n")
puts "LOGREAL03/04 PASS: #{entries.length} game log directories mapped"
