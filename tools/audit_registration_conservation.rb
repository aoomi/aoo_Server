#!/usr/bin/env ruby
# frozen_string_literal: true

require 'json'
require 'time'

root = File.expand_path('..', __dir__)
mapping_path = File.join(root, 'work/audit/database-runtime-mapping.json')
registration_path = File.join(root, 'work/audit/auto-registration-inventory.json')
output_path = File.join(root, 'work/audit/registration-conservation-audit.json')
task_path = File.join(root, 'docs/Aoo-前后端全框架功能通信审计任务清单.md')

mapping = JSON.parse(File.read(mapping_path, encoding: 'UTF-8'))
registration = JSON.parse(File.read(registration_path, encoding: 'UTF-8'))
hits = registration.fetch('hits')
baseline = JSON.parse(File.read(File.join(root, 'work/audit/audit-source-baseline.json'), encoding: 'UTF-8'))

relevant_hits = hits.select { |hit| %w[provider router handler cocos-component controller].include?(hit.fetch('kind')) }
hit_text = relevant_hits.map do |hit|
  path = hit.fetch('path')
  text = File.file?(path) && File.size(path) <= 2_000_000 ? File.binread(path).force_encoding('UTF-8').scrub.downcase : ''
  hit.merge('text' => text, 'relativePath' => path.delete_prefix(root + '/'))
end
concrete_provider_paths = []
baseline.fetch('files').each do |item|
  path = item.fetch('path')
  next unless path.start_with?(File.join(root, 'server') + '/') && path.end_with?('.java')
  next unless File.file?(path) && item.fetch('size') <= 2_000_000
  text = File.binread(path).force_encoding('UTF-8').scrub
  next unless text.match?(/(?:implements|extends)\s+(?:Mahjong|Poker|LongCard|WordCard)?GameProvider\b/)
  concrete_provider_paths << path
  hit_text << {'kind' => 'provider', 'path' => path, 'line' => 1, 'snippet' => 'concrete GameProvider declaration',
               'text' => text.downcase, 'relativePath' => path.delete_prefix(root + '/')}
end
generic_router_present = hit_text.any? { |hit| %w[router handler controller].include?(hit.fetch('kind')) && hit.fetch('relativePath').include?('GameWebSocketRouter') }

rows = mapping.fetch('mappings').map do |game|
  code = game.fetch('code').downcase
  token = /(?<![a-z0-9_])#{Regexp.escape(code)}(?![a-z0-9_])/i
  matched = hit_text.select { |hit| hit.fetch('relativePath').downcase.match?(token) || hit.fetch('text').match?(token) }
  providers = matched.select { |hit| hit.fetch('kind') == 'provider' }
  routes = matched.select { |hit| %w[router handler controller].include?(hit.fetch('kind')) }
  components = matched.select { |hit| hit.fetch('kind') == 'cocos-component' }
  direct_provider_paths = concrete_provider_paths.select { |path| path.downcase.match?(token) }
  native_provider = providers.any? { |hit| !hit.fetch('relativePath').include?('CatalogGameProvider') } || !direct_provider_paths.empty?
  database = game.fetch('databaseMapped')
  route_registered = !routes.empty? || (native_provider && generic_router_present)
  closed = database && native_provider && route_registered && !components.empty?
  {
    gameId: game['gameId'], code: code, category: game['category'], family: game['family'],
    databaseRegistered: database, nativeProviderRegistered: native_provider,
    routeRegistered: route_registered, clientComponentRegistered: !components.empty?, closed: closed,
    evidence: (matched.map { |hit| {kind: hit['kind'], path: hit['relativePath'], line: hit['line']} } +
      direct_provider_paths.map { |path| {kind: 'provider', path: path.delete_prefix(root + '/'), line: 1} }).uniq.first(40)
  }
end

summary = {
  catalogCount: rows.size,
  databaseRegistered: rows.count { |row| row[:databaseRegistered] },
  nativeProviderRegistered: rows.count { |row| row[:nativeProviderRegistered] },
  routeRegistered: rows.count { |row| row[:routeRegistered] },
  clientComponentRegistered: rows.count { |row| row[:clientComponentRegistered] },
  closed: rows.count { |row| row[:closed] },
  violations: rows.count { |row| !row[:closed] }
}
report = {
  schemaVersion: 1,
  generatedAt: Time.now.utc.iso8601,
  invariant: 'Each catalog game must have database, native provider, route and client-component registration.',
  summary: summary,
  rows: rows
}
File.write(output_path, JSON.pretty_generate(report) + "\n", mode: 'w:UTF-8')

task = File.read(task_path, encoding: 'UTF-8')
row = "| CALL12 | 未完成 | 未通过·已建门禁 | 已建立迁移代码、Provider、路由、Cocos 组件与数据库登记的注册守恒门禁；目录=#{summary[:catalogCount]}、DB=#{summary[:databaseRegistered]}、原生 Provider=#{summary[:nativeProviderRegistered]}、路由=#{summary[:routeRegistered]}、前端组件=#{summary[:clientComponentRegistered]}、全闭合=#{summary[:closed]}、违规=#{summary[:violations]}，未达零违规不得闭合。 证据：work/audit/registration-conservation-audit.json；工具：tools/audit_registration_conservation.rb |"
task.sub!(/^\| CALL12 \|.*$/, row) or abort 'CALL12 row not found'
task.sub!(/^下一项：.*$/, '下一项：ISO02 旧 WebSocket Handler、消息号与兼容别名隔离') or abort 'next pointer not found'
File.write(task_path, task, mode: 'w:UTF-8')
puts JSON.generate(summary)
exit(summary[:violations].zero? ? 0 : 2) if ENV['AOO_ENFORCE_REGISTRATION_GATE'] == '1'
