require 'json'
require 'fileutils'
require 'rexml/document'

root = File.expand_path('..', __dir__)
parents = [File.join(root, 'pom.xml'), File.join(root, 'server/LegacyAccountServer/pom.xml')]
all_poms = Dir.glob(File.join(root, '**/pom.xml')).reject { |path| path.include?('/target/') }
pom_text = all_poms.to_h { |path| [path, File.read(path)] }
tree_text = Dir.glob(File.join(root, '{work,server/*/work}/audit/cur04-dependency-tree.txt')).map { |path| File.read(path) }.join("\n")
conventional = %w[project.build.sourceEncoding maven.compiler.release java.version]
unused_boms = []
unused_properties = []

parents.each do |path|
  xml = REXML::Document.new(pom_text.fetch(path))
  REXML::XPath.each(xml, '//*[local-name()="dependency" and *[local-name()="scope" and text()="import"]]') do |dependency|
    group = REXML::XPath.first(dependency, '*[local-name()="groupId"]')&.text
    artifact = REXML::XPath.first(dependency, '*[local-name()="artifactId"]')&.text
    next unless group && artifact
    family = artifact.sub(/-bom$/, '')
    consumers = pom_text.reject { |candidate, _| candidate == path }.values.count do |text|
      text.include?(group) || text.include?(family)
    end
    consumers += 1 if tree_text.include?("#{group}:")
    unused_boms << "#{group}:#{artifact}" if consumers.zero?
  end
  REXML::XPath.each(xml, '/*[local-name()="project"]/*[local-name()="properties"]/*') do |property|
    next if conventional.include?(property.name)
    references = pom_text.values.sum { |text| text.scan("\${#{property.name}}").size }
    unused_properties << property.name if references.zero?
  end
end

checks = {
  every_bom_referenced: unused_boms.empty?,
  every_version_property_referenced: unused_properties.empty?,
  no_unconsumed_grpc_or_otel_bom: !pom_text.fetch(File.join(root, 'pom.xml')).match?(/grpc-bom|opentelemetry-bom/) ||
    %w[io.grpc: io.opentelemetry:].all? { |family| tree_text.include?(family) }
}
result = { task: 'UNUSED06', passed: checks.values.all?, checks: checks,
           unusedBoms: unused_boms, unusedProperties: unused_properties }
FileUtils.mkdir_p(File.join(root, 'work/audit'))
File.write(File.join(root, 'work/audit/unused06-bom-properties.json'), JSON.pretty_generate(result))
puts JSON.generate(result)
abort('UNUSED06 failed') unless result[:passed]
