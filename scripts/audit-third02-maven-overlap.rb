#!/usr/bin/env ruby
require 'fileutils'
require 'json'
require 'open3'
require 'set'

root = File.expand_path('..', __dir__)
archive_root = File.join(root, 'reference/legacy-2.22/third-party-maven-repository/com')
embedded_jars = Dir.glob(File.join(archive_root, '**/*.jar')).sort
classes = lambda do |jar|
  stdout, _stderr, status = Open3.capture3('unzip', '-Z1', jar)
  status.success? ? stdout.b.lines.map { |line| line.strip.force_encoding(Encoding::BINARY) }.select { |name| name.end_with?('.class'.b) }.reject { |name| name == 'module-info.class'.b }.to_set : Set.new
end
embedded_classes = embedded_jars.to_h { |jar| [jar.delete_prefix(root + '/'), classes.call(jar)] }
pom_files = [File.join(root, 'pom.xml')] + Dir.glob(File.join(root, 'server/**/pom.xml'))
pom_text = pom_files.map { |path| File.read(path) }.join("\n")
embedded_coordinates = embedded_jars.map do |jar|
  relative = jar.delete_prefix(archive_root + '/').split('/')
  [relative[0..-4].join('.'), relative[-3], relative[-2]].join(':')
end
runtime_jars = Dir.glob(File.join(root, 'server/**/{target,build}/lib/*.jar')).sort
runtime_classes = runtime_jars.to_h { |jar| [jar.delete_prefix(root + '/'), classes.call(jar)] }
runtime_overlaps = embedded_classes.flat_map do |embedded, names|
  runtime_classes.each_with_object([]) do |(runtime, runtime_names), findings|
    overlap = names & runtime_names
    findings << { embedded: embedded, runtime: runtime, classes: overlap.to_a.sort } unless overlap.empty?
  end
end
source_types = {}
Dir.glob(File.join(root, 'server/**/*.java')).each do |path|
  next if path.include?('/target/') || path.include?('/build/') || path.include?('/reference/')
  content = File.read(path)
  package_name = content[/^\s*package\s+([\w.]+)\s*;/, 1]
  type_name = content[/^\s*(?:public\s+)?(?:final\s+|abstract\s+)?(?:class|interface|enum|record)\s+(\w+)/, 1]
  next unless type_name
  binary_name = [package_name&.tr('.', '/'), "#{type_name}.class"].compact.reject(&:empty?).join('/')
  source_types[binary_name] = path.delete_prefix(root + '/')
end
source_overlaps = embedded_classes.flat_map do |embedded, names|
  (names & source_types.keys.to_set).map { |name| { embedded: embedded, class: name, source: source_types[name] } }
end
checks = {
  no_embedded_coordinate_dependency: embedded_coordinates.none? { |coordinate| pom_text.include?(coordinate.split(':')[0]) && pom_text.include?(coordinate.split(':')[1]) },
  no_file_repository: pom_text !~ %r{<url>\s*file:}i,
  no_system_path: pom_text !~ /<systemPath>/,
  archived_repository_not_active: !Dir.exist?(File.join(root, 'third-party')),
  no_active_vendored_lib: Dir.glob(File.join(root, 'server/**/{lib,libs}/*.jar')).reject { |path| path.include?('/target/') || path.include?('/build/') }.empty?,
  archived_artifacts_release_excluded: File.read(File.join(root, '.releaseignore')).lines.map(&:strip).include?('reference/')
}
result = {
  task: 'THIRD02', passed: checks.values.all?, checks: checks,
  embeddedCoordinates: embedded_coordinates, runtimeJarCount: runtime_jars.length,
  archivedRuntimeOverlaps: runtime_overlaps, archivedSourceOverlaps: source_overlaps,
  policy: 'docs/内置第三方依赖隔离规范.md'
}
out = File.join(root, 'work/audit/third02-maven-overlap.json')
FileUtils.mkdir_p(File.dirname(out))
File.write(out, JSON.pretty_generate(result) + "\n")
puts "THIRD02 #{result[:passed] ? 'passed' : 'failed'}: #{embedded_jars.length} embedded jars isolated from #{runtime_jars.length} resolved runtime jars"
exit(result[:passed] ? 0 : 1)
