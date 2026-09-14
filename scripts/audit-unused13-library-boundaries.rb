require 'json'
require 'fileutils'

root = File.expand_path('..', __dir__)
java_files = Dir.glob(File.join(root, 'server/**/*.java')).reject { |path| path.include?('/build/') || path.include?('/target/') }
mixed_json = []
mixed_network = []
mixed_tools = []
java_files.each do |path|
  text = File.binread(path).force_encoding('UTF-8').scrub
  relative = path.delete_prefix(root + '/')
  json = { jackson: /(?:com\.fasterxml|tools)\.jackson/, gson: /com\.google\.gson/, fastjson: /com\.alibaba\.fastjson/ }.select { |_, re| text.match?(re) }.keys
  network = { netty: /io\.netty/, mina: /org\.apache\.mina/ }.select { |_, re| text.match?(re) }.keys
  tools = { guava: /com\.google\.common/, commons: /org\.apache\.commons/ }.select { |_, re| text.match?(re) }.keys
  mixed_json << relative if json.size > 1
  mixed_network << relative if network.size > 1
  mixed_tools << relative if tools.size > 1
end
json_adapters = ['server/LegacyAccountServer/server/src/main/java/core/network/http/client/HttpProtocolEnvelope.java']
network_adapters = [
  %r{^server/LegacyCommon/src/com/ddm/server/websocket/},
  %r{^server/gameServer/src/core/network/client2game/ClientSession\.java$}
]
unexpected_json = mixed_json - json_adapters
unexpected_network = mixed_network.reject { |path| network_adapters.any? { |pattern| path.match?(pattern) } }
tool_role_violations = mixed_tools.select do |relative|
  text = File.read(File.join(root, relative))
  guava_imports = text.lines.grep(/^import com\.google\.common/)
  commons_imports = text.lines.grep(/^import org\.apache\.commons/)
  !guava_imports.all? { |line| line.match?(/\.collect\.(?:Lists|Maps|Sets);/) } ||
    !commons_imports.all? { |line| line.match?(/(?:CollectionUtils|MapUtils|ListUtils|StringUtils|Validate);/) }
end
checks = { one_json_model_per_business_file: unexpected_json.empty?,
           network_mix_only_in_legacy_transport_adapters: unexpected_network.empty?,
           utility_mix_has_distinct_factory_and_validation_roles: tool_role_violations.empty? }
result = { task: 'UNUSED13', passed: checks.values.all?, checks: checks,
           policy: { json: 'Jackson modern; Fastjson/Gson compatibility adapters only',
                     network: 'Netty modern; MINA only inside legacy transport adapter',
                     utilities: 'Guava collection factories; Apache Commons validation/query helpers' },
           mixedJsonAdapters: mixed_json, mixedNetworkAdapters: mixed_network,
           distinctToolRoleFiles: mixed_tools, violations: unexpected_json + unexpected_network + tool_role_violations }
FileUtils.mkdir_p(File.join(root, 'work/audit'))
File.write(File.join(root, 'work/audit/unused13-library-boundaries.json'), JSON.pretty_generate(result))
puts JSON.generate(result)
abort('UNUSED13 failed') unless result[:passed]
