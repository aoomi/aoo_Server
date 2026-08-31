#!/usr/bin/env ruby
# frozen_string_literal: true

require 'json'
require 'time'

root = File.expand_path('..', __dir__)
baseline = JSON.parse(File.read(File.join(root, 'work/audit/audit-source-baseline.json'), encoding: 'UTF-8'))
mapping = JSON.parse(File.read(File.join(root, 'work/audit/database-runtime-mapping.json'), encoding: 'UTF-8')).fetch('mappings')
output_path = File.join(root, 'work/audit/cross-cutting-placement.json')
task_path = File.join(root, 'docs/Aoo-前后端全框架功能通信审计任务清单.md')

codes = mapping.reject { |game| game['category'] == 'INFRASTRUCTURE' }.map { |game| game.fetch('code').downcase }.uniq
concerns = {
  'billing' => /(?:Billing|Ledger|Wallet|Currency|Diamond|RoomCard|GoldService)/i,
  'permission' => /(?:Permission|Authorization|AccessControl|RoleService|Privilege)/i,
  'risk' => /(?:Risk|AntiCheat|Fraud|RateLimit|Abuse)/i,
  'logging' => /(?:LoggerFactory|LogManager|getLogger|System\.(?:out|err)\.print)/i,
  'metrics' => /(?:MeterRegistry|Counter|Histogram|OpenTelemetry|Metrics)/i,
  'persistence' => /(?:java\.sql\.|JdbcTemplate|DataSource|Jedis|Redisson|EntityManager|SqlSession|Repository|Dao\b)/i
}.freeze
facade_pattern = /(?:com\.aoo\.bcg\.(?:billing|gateway|common|gamespi|config)|BillingService|PermissionService|RiskService|PersistencePort|MetricsPort|AuditLog)/i

files = []
baseline.fetch('files').each do |item|
  path = item.fetch('path')
  next unless path.start_with?(File.join(root, 'modules') + '/', File.join(root, 'server') + '/')
  next unless File.file?(path) && %w[.java .kt].include?(File.extname(path).downcase) && item.fetch('size') <= 2_000_000
  relative = path.delete_prefix(root + '/')
  lower = relative.downcase
  game_codes = codes.select { |code| lower.match?(/(?<![a-z0-9_])#{Regexp.escape(code)}(?![a-z0-9_])/) }.first(20)
  next if game_codes.empty?
  text = File.binread(path).force_encoding('UTF-8').scrub
  kinds = concerns.each_with_object([]) { |(kind, regex), found| found << kind if text.match?(regex) }
  next if kinds.empty?
  facade = text.match?(facade_pattern)
  direct_infrastructure = kinds.any? { |kind| %w[persistence logging metrics].include?(kind) } && !facade
  files << {path: relative, gameCodes: game_codes, concerns: kinds, facadePresent: facade,
            directInfrastructureCandidate: direct_infrastructure}
end

summary = {
  gameSpecificCrossCuttingFiles: files.size,
  facadeConsumerFiles: files.count { |file| file[:facadePresent] },
  directInfrastructureCandidates: files.count { |file| file[:directInfrastructureCandidate] },
  affectedGames: files.select { |file| file[:directInfrastructureCandidate] }.flat_map { |file| file[:gameCodes] }.uniq.size,
  concernCounts: concerns.keys.to_h { |kind| [kind, files.count { |file| file[:concerns].include?(kind) }] },
  directConcernCounts: concerns.keys.to_h do |kind|
    [kind, files.count { |file| file[:directInfrastructureCandidate] && file[:concerns].include?(kind) }]
  end
}
report = {
  schemaVersion: 1, generatedAt: Time.now.utc.iso8601, summary: summary,
  invariant: 'Billing, permission, risk, logging, metrics and persistence are accessed through framework ports/services; game modules do not import concrete infrastructure.',
  files: files.sort_by { |file| file[:path] },
  limitations: ['Static facade token detection is candidate evidence; architecture bytecode rules and dependency injection graph are required for closure.']
}
File.write(output_path, JSON.pretty_generate(report) + "\n", mode: 'w:UTF-8')

task = File.read(task_path, encoding: 'UTF-8')
row = "| CLASS09 | 未完成 | 已建横切门禁·待整改 | 已盘点账务/权限/风控/日志/指标/持久化在具体玩法的接入；横切文件=#{summary[:gameSpecificCrossCuttingFiles]}、经框架门面=#{summary[:facadeConsumerFiles]}、直接基础设施候选=#{summary[:directInfrastructureCandidates]}、涉及玩法=#{summary[:affectedGames]}；需达零具体基础设施依赖且字节码架构门禁通过才能闭合。 证据：work/audit/cross-cutting-placement.json；工具：tools/audit_cross_cutting_placement.rb |"
task.sub!(/^\| CLASS09 \|.*$/, row) or abort 'CLASS09 row not found'
task.sub!(/^下一项：.*$/, '下一项：CLASS10 分类冲突清单') or abort 'next pointer not found'
File.write(task_path, task, mode: 'w:UTF-8')
puts JSON.generate(summary)
