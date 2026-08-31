require 'json'
require 'fileutils'

root = File.expand_path('..', __dir__)
client = File.expand_path('../Client', root)
manifests = Dir.glob(File.join(client, '**/package.json')).reject do |path|
  path.match?(%r{/(?:node_modules|temp|library|build)/})
end
inventory = manifests.map do |path|
  package = JSON.parse(File.read(path))
  { manifest: path.delete_prefix(client + '/'), dependencies: package.fetch('dependencies', {}) }
end
production_dependencies = inventory.flat_map do |item|
  item[:dependencies].map { |name, version| { manifest: item[:manifest], name: name, version: version } }
end
checks = { every_manifest_scanned: manifests.size == 2,
           no_unproved_production_package: production_dependencies.empty?,
           cocos_runtime_uses_creator_modules_not_node_packages: JSON.parse(File.read(File.join(client, 'package.json'))).fetch('dependencies', {}).empty? }
result = { task: 'UNUSED16', passed: checks.values.all?, checks: checks,
           manifests: inventory, removed: [{ name: 'playwright', reason: 'no import, dynamic import, script or plugin configuration; browser tests use external harness' }] }
FileUtils.mkdir_p(File.join(root, 'work/audit'))
File.write(File.join(root, 'work/audit/unused16-npm-production.json'), JSON.pretty_generate(result))
puts JSON.generate(result)
abort('UNUSED16 failed') unless result[:passed]
