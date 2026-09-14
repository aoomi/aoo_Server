require 'json'
require 'fileutils'
require 'rexml/document'

root = File.expand_path('..', __dir__)
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
production_files = Dir.glob(File.join(root, 'server/**/*.java')).reject { |path| path.include?('/target/') || path.include?('/build/') }
native_loads = production_files.select do |path|
  File.binread(path).force_encoding('UTF-8').scrub.match?(/System\.(?:load|loadLibrary)\s*\(|Native\.load\s*\(/)
end.map { |path| path.delete_prefix(root + '/') }
scripts = Dir.glob(File.join(root, '{scripts,tools}/**/*.{sh,rb}')).reject { |path| path == File.expand_path(__FILE__) }
unpackers = scripts.select do |path|
  text = File.binread(path).force_encoding('UTF-8').scrub
  text.match?(/(?:unzip\s+(?!['"]?-(?:Z1|p)\b)|tar\s+[^\n]*-[^\n]*x|\bcp\b[^\n]*\.(?:so|dylib|dll)\b)/i)
end.map { |path| path.delete_prefix(root + '/') }
broad_native_access = File.read(File.join(root, 'tools/runtime-java26.sh')).include?('--enable-native-access=ALL-UNNAMED')
checks = { no_direct_native_or_classifier_dependency: native_dependencies.empty? && classifiers.empty?,
           no_manual_jni_load: native_loads.empty?, no_native_unpack_script: unpackers.empty?,
           no_broad_native_access_grant: !broad_native_access }
result = { task: 'UNUSED15', passed: checks.values.all?, checks: checks,
           directNativeDependencies: native_dependencies, classifiers: classifiers,
           manualLoads: native_loads, unpackScripts: unpackers,
           transitiveException: 'zstd-jni is owned by RocketMQ compression and ships as a platform-neutral Maven artifact; no explicit classifier/unpack path' }
FileUtils.mkdir_p(File.join(root, 'work/audit'))
File.write(File.join(root, 'work/audit/unused15-native-dependencies.json'), JSON.pretty_generate(result))
puts JSON.generate(result)
abort('UNUSED15 failed') unless result[:passed]
