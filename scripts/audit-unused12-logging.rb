require 'json'
require 'fileutils'
require 'rexml/document'

root = File.expand_path('..', __dir__)
providers = %w[logback-classic log4j-slf4j2-impl slf4j-simple slf4j-jdk14 slf4j-nop slf4j-reload4j]
to_slf4j = %w[log4j-to-slf4j jul-to-slf4j jcl-over-slf4j]
from_slf4j = %w[log4j-slf4j2-impl slf4j-jdk14 slf4j-reload4j]
declarations = []

Dir.glob(File.join(root, '**/pom.xml')).reject { |path| path.include?('/target/') }.sort.each do |path|
  document = REXML::Document.new(File.read(path))
  REXML::XPath.each(document, '/*[local-name()="project"]/*[local-name()="dependencies"]/*[local-name()="dependency"]') do |dependency|
    artifact = REXML::XPath.first(dependency, '*[local-name()="artifactId"]')&.text
    next unless providers.include?(artifact) || to_slf4j.include?(artifact) || from_slf4j.include?(artifact) || artifact == 'commons-logging'
    declarations << { pom: path.delete_prefix(root + '/'), artifact: artifact,
                      scope: REXML::XPath.first(dependency, '*[local-name()="scope"]')&.text || 'compile',
                      optional: REXML::XPath.first(dependency, '*[local-name()="optional"]')&.text == 'true' }
  end
end
active_providers = declarations.select { |item| providers.include?(item[:artifact]) && !item[:optional] }
provider_names = active_providers.map { |item| item[:artifact] }.uniq
artifacts = declarations.map { |item| item[:artifact] }
cycles = (artifacts & to_slf4j).product(artifacts & from_slf4j).map { |a, b| [a, b] }
bad_scopes = active_providers.reject { |item| item[:scope] == 'runtime' }
checks = { only_logback_provider_declared: (provider_names - ['logback-classic']).empty?,
           providers_not_compile_api: bad_scopes.empty?, no_bridge_cycles: cycles.empty?,
           dependency_convergence_gate_present: File.read(File.join(root, 'pom.xml')).include?('<dependencyConvergence/>') }
result = { task: 'UNUSED12', passed: checks.values.all?, checks: checks,
           declarations: declarations, activeProviders: active_providers, bridgeCycles: cycles,
           policy: { facade: 'SLF4J 2.x', provider: 'Logback', bridges: 'one-way into SLF4J only' } }
FileUtils.mkdir_p(File.join(root, 'work/audit'))
File.write(File.join(root, 'work/audit/unused12-logging-stack.json'), JSON.pretty_generate(result))
puts JSON.generate(result)
abort('UNUSED12 failed') unless result[:passed]
