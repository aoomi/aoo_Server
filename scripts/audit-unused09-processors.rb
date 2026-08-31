require 'json'
require 'fileutils'
require 'rexml/document'

root = File.expand_path('..', __dir__)
poms = Dir.glob(File.join(root, '**/pom.xml')).reject { |path| path.include?('/target/') }
active_lombok = []
processor_paths = []
violations = []

poms.each do |path|
  document = REXML::Document.new(File.read(path))
  REXML::XPath.each(document, '/*[local-name()="project"]/*[local-name()="dependencies"]/*[local-name()="dependency"]') do |dependency|
    artifact = REXML::XPath.first(dependency, '*[local-name()="artifactId"]')&.text
    next unless artifact == 'lombok'
    scope = REXML::XPath.first(dependency, '*[local-name()="scope"]')&.text
    item = { pom: path.delete_prefix(root + '/'), scope: scope }
    active_lombok << item
    violations << item.merge(reason: 'Lombok must be provided') unless scope == 'provided'
  end
  REXML::XPath.each(document, '//*[local-name()="annotationProcessorPaths"]/*[local-name()="path"]') do |processor|
    group = REXML::XPath.first(processor, '*[local-name()="groupId"]')&.text
    artifact = REXML::XPath.first(processor, '*[local-name()="artifactId"]')&.text
    item = { pom: path.delete_prefix(root + '/'), coordinate: "#{group}:#{artifact}" }
    processor_paths << item
    violations << item.merge(reason: 'unapproved annotation processor') unless [group, artifact] == ['org.projectlombok', 'lombok']
  end
end

lombok_sources = Dir.glob(File.join(root, 'server/**/*.java')).reject { |path| path.include?('/target/') }.count do |path|
  File.binread(path).include?('lombok.')
end
checks = { lombok_is_compile_only: active_lombok.any? && violations.none? { |v| v[:reason] == 'Lombok must be provided' },
           processor_allowlist_enforced: processor_paths.size == 2 && violations.none? { |v| v[:reason] == 'unapproved annotation processor' },
           processor_config_covers_both_reactors: processor_paths.map { |i| i[:pom] }.sort == ['pom.xml', 'server/LegacyAccountServer/pom.xml'],
           lombok_usage_proven: lombok_sources.positive? }
result = { task: 'UNUSED09', passed: checks.values.all?, checks: checks, lombokSourceCount: lombok_sources,
           lombokDependencies: active_lombok, processorPaths: processor_paths, violations: violations }
FileUtils.mkdir_p(File.join(root, 'work/audit'))
File.write(File.join(root, 'work/audit/unused09-annotation-processors.json'), JSON.pretty_generate(result))
puts JSON.generate(result)
abort('UNUSED09 failed') unless result[:passed]
