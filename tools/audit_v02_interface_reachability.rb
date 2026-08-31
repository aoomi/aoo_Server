#!/usr/bin/env ruby
# frozen_string_literal: true

require 'json'
require 'pathname'

ROOT = Pathname.new(__dir__).join('..', '..').cleanpath
PRODUCTION_CLIENT_ROOTS = [ROOT.join('Client/assets'), ROOT.join('Admin/src')].freeze
SERVER_ROOT = ROOT.join('Server/server')

Finding = Struct.new(:id, :kind, :boundary, :summary, :evidence, keyword_init: true)

def source_files(root, extensions)
  return [] unless root.directory?
  root.glob('**/*').select do |path|
    path.file? && extensions.include?(path.extname) &&
      !path.to_s.match?(%r{/(?:target|node_modules|src/test|test)/})
  end
end

def locations(files, pattern)
  files.flat_map do |path|
    path.readlines.each_with_index.map do |line, index|
      "#{path.relative_path_from(ROOT)}:#{index + 1}" if line.match?(pattern)
    end.compact
  end
end

def endpoint_literals(files)
  rows = []
  expression = %r{(?<quote>['"`])(?<path>/(?:api/)?v\d+/[A-Za-z0-9_./${}?=&:-]+)\k<quote>}
  files.each do |file|
    file.readlines.each_with_index do |line, index|
      line.scan(expression) do
        match = Regexp.last_match
        path = match[:path].sub(/\?.*\z/, '')
        path = path.gsub(/\$\{[^}]+\}/, '{id}')
        rows << [path, "#{file.relative_path_from(ROOT)}:#{index + 1}"]
      end
    end
  end
  rows.uniq
end

def server_contexts(files)
  contexts = []
  expression = /["'](?<path>\/(?:api\/)?v\d+(?:\/[^"']*)?)["']/
  files.each do |file|
    file.readlines.each_with_index do |line, index|
      line.scan(expression) do
        contexts << [Regexp.last_match[:path], "#{file.relative_path_from(ROOT)}:#{index + 1}"]
      end
    end
  end
  contexts.uniq
end

def covered?(endpoint, contexts)
  concrete = endpoint.sub(%r{/\{id\}.*\z}, '')
  contexts.any? do |context, _location|
    endpoint == context || endpoint.start_with?("#{context}/") || context.start_with?(concrete)
  end
end

client_files = PRODUCTION_CLIENT_ROOTS.flat_map { |root| source_files(root, %w[.ts .tsx .js .vue]) }
server_files = source_files(SERVER_ROOT, ['.java'])
client_endpoints = endpoint_literals(client_files)
contexts = server_contexts(server_files)
findings = []

legacy_locations = locations(client_files, %r{['"`]\/v1\/|['"`]\/api\/v1\/})
unless legacy_locations.empty?
  findings << Finding.new(id: 'V02-LEGACY-001', kind: 'legacy_entry', boundary: 'Client/Admin network owner → API governance owner',
                         summary: '生产客户端仍直接调用 v1 HTTP 入口，唯一 v2 入口门禁不成立。', evidence: legacy_locations.first(40))
end

send_pack_locations = locations(source_files(ROOT.join('Client/assets'), %w[.ts .js]), /\bSendPack\s*\(/)
unless send_pack_locations.empty?
  findings << Finding.new(id: 'V02-LEGACY-002', kind: 'legacy_entry', boundary: 'Client realtime owner → Gateway owner',
                         summary: '生产 assets 仍可达旧 SendPack 调用，WSS 唯一协议入口未关闭。', evidence: send_pack_locations.first(40))
end

unmatched = client_endpoints.reject { |endpoint, _| covered?(endpoint, contexts) }
unless unmatched.empty?
  findings << Finding.new(id: 'V02-HTTP-001', kind: 'transport_break', boundary: 'Client/Admin API owner → Server route owner',
                         summary: '客户端声明的 HTTP 路径没有可静态匹配的服务端 createContext。',
                         evidence: unmatched.first(80).map { |endpoint, location| "#{location} #{endpoint}" })
end

external_fetch = locations(client_files, /fetch\(\s*["'`]https?:\/\//)
unless external_fetch.empty?
  findings << Finding.new(id: 'V02-BYPASS-001', kind: 'authority_bypass', boundary: 'Admin/Client owner → Server integration owner',
                         summary: '生产 UI 代码直接访问外部 HTTP 服务，绕过服务端鉴权、审计与权威边界。', evidence: external_fetch.first(40))
end

adapter_definitions = client_files.flat_map do |file|
  text = file.read
  text.scan(/export\s+class\s+(\w+(?:Gateway|Client|Controller))\b/).flatten.map { |name| [name, file] }
end
all_client_text = client_files.map(&:read).join("\n")
orphans = adapter_definitions.map do |name, file|
  "#{file.relative_path_from(ROOT)} #{name}" if all_client_text.scan(/\b#{Regexp.escape(name)}\b/).length == 1
end.compact
unless orphans.empty?
  findings << Finding.new(id: 'V02-UI-001', kind: 'ui_break', boundary: 'UI scene/controller owner → Client API owner',
                         summary: '网络适配器只有定义、没有生产实例化/消费证据，UI 起点或回流断开。', evidence: orphans.first(80))
end

ids_unique = findings.map(&:id).uniq.length == findings.length
report = {
  schemaVersion: 1,
  gate: 'V02-interface-reachability',
  result: findings.empty? && ids_unique ? 'PASS' : 'FAIL',
  policy: 'Only zero findings passes; manual/real-device acceptance is excluded.',
  inventory: {
    clientSourceFiles: client_files.length,
    serverSourceFiles: server_files.length,
    clientHttpEndpointLiterals: client_endpoints.length,
    serverHttpContexts: contexts.length
  },
  checks: {
    uniqueFindingIds: ids_unique,
    noLegacyHttpEntry: legacy_locations.empty?,
    noLegacySendPackEntry: send_pack_locations.empty?,
    allClientHttpPathsHaveServerContext: unmatched.empty?,
    noDirectExternalHttpBypass: external_fetch.empty?,
    noUninstantiatedNetworkAdapters: orphans.empty?
  },
  findings: findings.map(&:to_h)
}

puts JSON.pretty_generate(report)
exit(report[:result] == 'PASS' ? 0 : 1)
