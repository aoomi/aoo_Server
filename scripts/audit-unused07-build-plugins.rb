require 'json'
require 'fileutils'
require 'rexml/document'

root = File.expand_path('..', __dir__)
default_lifecycle = %w[maven-compiler-plugin maven-resources-plugin maven-surefire-plugin maven-jar-plugin]
release_cli_plugins = {
  'flyway-maven-plugin' => File.join(root, 'tools/apply_migrations.sh')
}
inventory = []
ineffective = []

Dir.glob(File.join(root, '**/pom.xml')).reject { |path| path.include?('/target/') }.sort.each do |path|
  document = REXML::Document.new(File.read(path))
  REXML::XPath.each(document, '/*[local-name()="project"]/*[local-name()="build"]/*[local-name()="plugins"]/*[local-name()="plugin"]') do |plugin|
    artifact = REXML::XPath.first(plugin, '*[local-name()="artifactId"]')&.text
    executions = REXML::XPath.match(plugin, '*[local-name()="executions"]/*[local-name()="execution"]')
    external_cli = release_cli_plugins.key?(artifact) && File.file?(release_cli_plugins[artifact]) &&
                   File.read(release_cli_plugins[artifact]).include?(artifact)
    effective = default_lifecycle.include?(artifact) || external_cli || executions.any? do |execution|
      REXML::XPath.first(execution, '*[local-name()="goals"]/*[local-name()="goal"]')
    end
    item = { pom: path.delete_prefix(root + '/'), plugin: artifact,
             mechanism: default_lifecycle.include?(artifact) ? 'default-lifecycle' : (external_cli ? 'audited-release-cli' : 'bound-execution'),
             executions: executions.size, effective: effective }
    inventory << item
    ineffective << item unless effective
  end
end

checks = { all_active_plugins_effective: ineffective.empty?, no_gradle_build: Dir.glob(File.join(root, '**/{build.gradle,build.gradle.kts}')).empty? }
result = { task: 'UNUSED07', passed: checks.values.all?, checks: checks,
           pluginCount: inventory.size, ineffective: ineffective, inventory: inventory }
FileUtils.mkdir_p(File.join(root, 'work/audit'))
File.write(File.join(root, 'work/audit/unused07-build-plugins.json'), JSON.pretty_generate(result))
puts JSON.generate(result.reject { |key, _| key == :inventory })
abort('UNUSED07 failed') unless result[:passed]
