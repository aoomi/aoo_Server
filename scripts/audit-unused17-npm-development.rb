require 'json'
require 'fileutils'

root = File.expand_path('..', __dir__)
client = File.expand_path('../Client', root)
binary_names = { 'typescript' => 'tsc' }
inventory = []
unused = []
Dir.glob(File.join(client, '**/package.json')).reject { |path| path.match?(%r{/(?:node_modules|temp|library|build)/}) }.each do |path|
  package = JSON.parse(File.read(path))
  scripts = package.fetch('scripts', {})
  package.fetch('devDependencies', {}).each do |name, version|
    token = binary_names.fetch(name, name)
    consumers = scripts.select { |_, command| command.match?(/(?:^|\s)#{Regexp.escape(token)}(?:\s|$)/) }.keys
    item = { manifest: path.delete_prefix(client + '/'), name: name, version: version, scripts: consumers }
    inventory << item
    unused << item if consumers.empty?
  end
end
checks = { every_dev_dependency_has_executed_script_entry: unused.empty?,
           no_invalid_standalone_typecheck_toolchain: inventory.empty?,
           no_duplicate_package_manager_lock: Dir.glob(File.join(client, '{package-lock.json,yarn.lock}')).empty? }
result = { task: 'UNUSED17', passed: checks.values.all?, checks: checks, inventory: inventory, unused: unused,
           removed: [{ name: 'typescript', reason: 'source-project has no assets and extends Creator-generated temp config; actual Creator project owns compilation' }] }
FileUtils.mkdir_p(File.join(root, 'work/audit'))
File.write(File.join(root, 'work/audit/unused17-npm-development.json'), JSON.pretty_generate(result))
puts JSON.generate(result)
abort('UNUSED17 failed') unless result[:passed]
