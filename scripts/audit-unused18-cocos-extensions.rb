require 'json'
require 'fileutils'

root = File.expand_path('..', __dir__)
client = File.expand_path('../Client', root)
extension_roots = Dir.glob(File.join(client, '**/extensions')).reject { |path| path.match?(%r{/(?:node_modules|temp|library|build)/}) }
extension_entries = extension_roots.flat_map { |path| Dir.children(path).map { |entry| File.join(path, entry).delete_prefix(client + '/') } }
plugin_profiles = Dir.glob(File.join(client, 'profiles/v2/packages/*plugin.json')).map do |path|
  json = JSON.parse(File.read(path))
  enabled_values = []
  walk = lambda do |value|
    if value.is_a?(Hash)
      value.each { |key, child| enabled_values << child if key.match?(/^enable/i) && (child == true || child == false); walk.call(child) }
    elsif value.is_a?(Array)
      value.each { |child| walk.call(child) }
    end
  end
  walk.call(json)
  { file: path.delete_prefix(client + '/'), enableFlags: enabled_values, enabled: enabled_values.any?(true) }
end
service_files = Dir.glob(File.join(client, '**/settings/v2/packages/cocos-service.json')).reject { |path| path.match?(%r{/(?:temp|library|build)/}) }
configured_services = service_files.flat_map do |path|
  JSON.parse(File.read(path)).fetch('configs', []).flat_map { |config| config.fetch('services', []) }
end
creator_versions = Dir.glob(File.join(client, '**/package.json')).reject { |path| path.match?(%r{/(?:node_modules|temp|library|build)/}) }.map do |path|
  [path.delete_prefix(client + '/'), JSON.parse(File.read(path)).dig('creator', 'version') || JSON.parse(File.read(path)).dig('engines', 'cocos')]
end
checks = { no_project_extension_installed: extension_entries.empty?, disabled_plugin_profiles_do_not_build: plugin_profiles.none? { |item| item[:enabled] },
           no_cocos_cloud_service_enabled: configured_services.empty?, all_creator_projects_are_3_8_8: creator_versions.all? { |_, version| version == '3.8.8' } }
result = { task: 'UNUSED18', passed: checks.values.all?, checks: checks, extensions: extension_entries,
           pluginProfiles: plugin_profiles, configuredServices: configured_services, creatorVersions: creator_versions }
FileUtils.mkdir_p(File.join(root, 'work/audit'))
File.write(File.join(root, 'work/audit/unused18-cocos-extensions.json'), JSON.pretty_generate(result))
puts JSON.generate(result)
abort('UNUSED18 failed') unless result[:passed]
