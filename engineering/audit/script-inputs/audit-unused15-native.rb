require 'json'
require 'fileutils'
require 'rexml/document'

root = File.expand_path('../../..', __dir__)

SAFE_ARCHIVE_OPERATIONS = {
  'engineering/local-dev/task-only-runtime/launcher-r20.sh' => [
    /\brequire_command\s+unzip\b/,
    /\bunzip\s+-Z1(?:\s|$)/,
    /\bunzip\s+-p(?:\s|$)/
  ],
  'engineering/tests/tool-inputs/test-room-rule-schema-evolution.rb' => [
    /system\(\s*'unzip',\s*'-qq',\s*workbook,\s*'-d',\s*directory\s*\)/
  ],
  'engineering/tests/tool-inputs/test-room-rules-watcher.rb' => [
    /system\(\s*'unzip',\s*'-tqq'/
  ],
  'engineering/release/tool-inputs/hide-xlsx-technical-sheet.rb' => [
    /system\(\s*'unzip',\s*'-q',\s*path,\s*'-d',\s*directory\s*\)/
  ],
  'engineering/release/tool-inputs/watch-room-rules.rb' => [
    /system\(\s*'unzip',\s*'-tqq',\s*workbook/
  ]
}.freeze

def native_unpack_operations(root, path, text)
  relative_path = path.delete_prefix(root + '/')
  safe_patterns = SAFE_ARCHIVE_OPERATIONS.fetch(relative_path, [])
  text.lines.each_with_object([[], []]) do |line, result|
    safe = safe_patterns.any? { |pattern| line.match?(pattern) }
    if safe
      result[1] << line.strip
      next
    end

    result[0] << line.strip if line.match?(
      /(?:\A|[;&|]\s*|\bsystem\(\s*['"])unzip(?:\s+|['"])|\btar\s+[^\n]*-[^\n]*x|\bcp\b[^\n]*\.(?:so|dylib|dll)\b/i
    )
  end
end

poms = Dir.glob(File.join(root, '**/pom.xml')).reject { |path| path.include?('/target/') }
native_dependencies = []
classifiers = []
poms.each do |path|
  document = REXML::Document.new(File.read(path))
  REXML::XPath.each(document, '//*[local-name()="dependency"]') do |dependency|
    artifact = REXML::XPath.first(dependency, '*[local-name()="artifactId"]')&.text.to_s
    classifier = REXML::XPath.first(dependency, '*[local-name()="classifier"]')&.text
    item = { pom: path.delete_prefix(root + '/'), artifact: artifact, classifier: classifier }
    classifiers << item if classifier
    native_dependencies << item if artifact.match?(/(?:jni|native|jna|jnr)/i)
  end
end
production_files = Dir.glob(File.join(root, 'modules/**/*.java')).reject { |path| path.include?('/target/') || path.include?('/build/') }
native_loads = production_files.select do |path|
  File.binread(path).force_encoding('UTF-8').scrub.match?(/System\.(?:load|loadLibrary)\s*\(|Native\.load\s*\(/)
end.map { |path| path.delete_prefix(root + '/') }
scan_inputs=JSON.parse(File.read(File.join(root, 'engineering/scan-input-roots.json'))); tool_input_roots=scan_inputs.fetch('toolInputs').map { |relative| File.join(root, relative) }; script_input_roots=scan_inputs.fetch('scriptInputs').map { |relative| File.join(root, relative) }; scripts = (script_input_roots + tool_input_roots).flat_map { |input_root| Dir.glob(File.join(input_root, '**/*.{sh,rb}')) }.uniq.reject { |path| path == File.expand_path(__FILE__) || File.basename(path).end_with?('_test.rb') }
safe_archive_operations = []
unpackers = scripts.each_with_object([]) do |path, violations|
  text = File.binread(path).force_encoding('UTF-8').scrub
  operations, safe_operations = native_unpack_operations(root, path, text)
  safe_operations.each do |operation|
    safe_archive_operations << { path: path.delete_prefix(root + '/'), operation: operation,
                                 classification: 'non-native archive inspection or rules fixture' }
  end
  next if operations.empty?

  violations << path.delete_prefix(root + '/')
end
broad_native_access = File.read(File.join(root, 'engineering/local-dev/tool-inputs/runtime-java26.sh')).include?('--enable-native-access=ALL-UNNAMED')
checks = { no_direct_native_or_classifier_dependency: native_dependencies.empty? && classifiers.empty?,
           no_manual_jni_load: native_loads.empty?, no_native_unpack_script: unpackers.empty?,
           no_broad_native_access_grant: !broad_native_access }
result = { task: 'UNUSED15', passed: checks.values.all?, checks: checks,
           directNativeDependencies: native_dependencies, classifiers: classifiers,
           manualLoads: native_loads, unpackScripts: unpackers,
           safeArchiveOperations: safe_archive_operations,
           transitiveException: 'zstd-jni is owned by RocketMQ compression and ships as a platform-neutral Maven artifact; no explicit classifier/unpack path' }
FileUtils.mkdir_p(File.join(root, 'build-only/audit'))
File.write(File.join(root, 'build-only/audit/unused15-native-dependencies.json'), JSON.pretty_generate(result))
puts JSON.generate(result)
abort('UNUSED15 failed') unless result[:passed]
