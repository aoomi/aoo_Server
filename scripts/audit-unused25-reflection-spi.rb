#!/usr/bin/env ruby
require 'json'
require 'pathname'

root = Pathname(__dir__).join('..').expand_path
ignored = %r{/(?:target|work|build)/}
java_files = Dir[root.join('server/**/*.java').to_s].reject { |p| p.match?(ignored) }
types = {}
annotated = []
reflection_sites = []
literal_reflections = []
java_files.each do |path|
  text = File.binread(path).force_encoding(Encoding::UTF_8).scrub
  package_name = text[/^\s*package\s+([\w.]+)\s*;/, 1]
  type_name = text[/\b(?:class|interface|enum|record)\s+([A-Za-z_$][\w$]*)/, 1]
  types["#{package_name}.#{type_name}"] = path if package_name && type_name
  relative = path.delete_prefix(root.to_s + '/')
  annotated << relative if text.match?(/^\s*@(Component|Service|Repository|Configuration|Controller|RestController|Entity|Table|RequestMapping)\b/m)
  if text.match?(/\b(?:Class\.forName|ServiceLoader|loadClass\s*\(|getDeclared(?:Method|Field|Constructor)|getMethod\s*\(|MethodHandles|Proxy\.newProxyInstance)/)
    reflection_sites << relative
  end
  text.scan(/Class\.forName\s*\(\s*"([A-Za-z_$][\w$]*(?:\.[A-Za-z_$][\w$]*)+)"/) do |match|
    literal_reflections << { source: relative, className: match.first }
  end
end

service_files = Dir[root.join('server/**/src/main/resources/META-INF/services/*').to_s].reject { |p| p.match?(ignored) }
services = service_files.map do |path|
  providers = File.readlines(path).map(&:strip).reject { |line| line.empty? || line.start_with?('#') }
  { file: path.delete_prefix(root.to_s + '/'), service: File.basename(path), providers: providers }
end
spring_files = Dir[root.join('server/**/{spring.factories,org.springframework.boot.autoconfigure.AutoConfiguration.imports}').to_s].reject { |p| p.match?(ignored) }
spring_entries = spring_files.map do |path|
  entries = File.read(path).scan(/[A-Za-z_$][\w$]*(?:\.[A-Za-z_$][\w$]*)+/).uniq
  { file: path.delete_prefix(root.to_s + '/'), classes: entries }
end

missing = []
services.each do |entry|
  missing << { kind: 'service-interface', className: entry[:service], source: entry[:file] } unless types.key?(entry[:service])
  entry[:providers].each { |name| missing << { kind: 'service-provider', className: name, source: entry[:file] } unless types.key?(name) }
end
spring_entries.each { |entry| entry[:classes].each { |name| missing << { kind: 'spring-entry', className: name, source: entry[:file] } unless types.key?(name) || name.start_with?('org.springframework.') } }
literal_reflections.each { |entry| missing << entry.merge(kind: 'literal-reflection') unless types.key?(entry[:className]) || entry[:className].start_with?('java.') }

errors = []
errors << "reflective/SPI classes missing: #{missing.map { |x| x[:className] }.join(', ')}" unless missing.empty?
report = {
  task: 'UNUSED25', status: errors.empty? ? 'passed' : 'failed', sourceTypeCount: types.length,
  serviceDescriptors: services, springRegistrations: spring_entries, annotationManagedClasses: annotated.sort,
  reflectionSites: reflection_sites.sort, literalReflectionClasses: literal_reflections, unresolvedRetentionEntries: missing,
  deletionPolicy: 'A class is not removable until direct references, META-INF/services, Spring factories/imports, annotation discovery, configuration class names and reflection sites are all checked.',
  errors: errors
}
out = root.join('work/audit/unused25-reflection-spi.json')
out.dirname.mkpath
out.write(JSON.pretty_generate(report) + "\n")
abort(errors.join("\n")) unless errors.empty?
puts "UNUSED25 passed: #{services.length} service descriptors and #{reflection_sites.length} reflection sites are retained and resolvable"
