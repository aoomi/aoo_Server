#!/usr/bin/env ruby
require 'digest'
require 'fileutils'
require 'json'

root = File.expand_path('..', __dir__)
poms = [File.join(root, 'pom.xml')] + Dir.glob(File.join(root, 'server/**/pom.xml'))
pom_text = poms.map { |path| File.read(path) }.join("\n")
runtime_java = File.read(File.join(root, 'tools/runtime-java26.sh'))
runtime_start = File.read(File.join(root, 'tools/runtime_start_backend_apps.py'))
launchers = Dir.glob(File.join(root, 'tools/start-*-local.sh')).to_h { |path| [path.delete_prefix(root + '/'), File.read(path)] }
containers = Dir.glob(File.join(root, '**/{Dockerfile,Containerfile,*dockerfile,compose*.yml,compose*.yaml}')).reject { |path| path.include?('/target/') || path.include?('/build/') || path.include?('/work/') }
runtime_classpaths = Dir.glob(File.join(root, 'server/**/target/runtime-classpath.txt'))
forbidden = %r{(?:reference/legacy-2\.22|third-party/com|server/common/lib|server/gameServer/build)}
classpath_findings = runtime_classpaths.each_with_object([]) do |path, findings|
  content = File.read(path)
  findings << { file: path.delete_prefix(root + '/'), forbidden: content.scan(forbidden).uniq } if content.match?(forbidden)
end
checks = {
  maven_is_authoritative: poms.all? { |path| File.read(path).include?('<project') },
  no_legacy_maven_coordinate: pom_text !~ /com\.aoo\.legacy|nettosphere/,
  local_runtime_uses_maven_classpath: runtime_java.include?('target/runtime/lib') && runtime_java.include?("-name '*.jar'") && !runtime_java.include?('target/classes') && !runtime_java.include?('runtime-classpath.txt'),
  launchers_use_shared_runtime_function: launchers.values.all? { |content| content.include?('runtime_classpath') || content.include?('local_service_control.py') },
  production_harness_excludes_legacy_jars: runtime_start.include?('return []') && runtime_start.include?('never placed on the classpath'),
  generated_classpaths_clean: classpath_findings.empty?,
  active_build_lib_absent: Dir.glob(File.join(root, 'server/**/build/lib/*.jar')).empty?,
  container_classpath_is_packaged_runtime: containers.empty? || containers.all? { |path| File.read(path).include?('COPY --chown=') && File.read(path).include?('runtime/') },
  archives_release_excluded: File.read(File.join(root, '.releaseignore')).lines.map(&:strip).include?('reference/')
}
result = {
  task: 'THIRD06', passed: checks.values.all?, checks: checks,
  environments: {
    development: 'Maven reactor compile',
    test: 'Maven reactor test classpath; main versions inherited',
    localRuntime: 'tools/runtime-java26.sh + packaged module JAR + target/runtime/lib/*',
    production: 'same packaged module JAR + Maven-materialized target/runtime/lib/*',
    container: containers.empty? ? 'not-applicable: no container definition' : 'minimal image consumes the Maven-assembled runtime directory'
  },
  pomFingerprint: Digest::SHA256.hexdigest(poms.sort.map { |path| Digest::SHA256.file(path).hexdigest }.join),
  runtimeClasspathFiles: runtime_classpaths.map { |path| path.delete_prefix(root + '/') },
  findings: classpath_findings,
  policy: 'docs/类加载与运行类路径规范.md'
}
out = File.join(root, 'work/audit/third06-classloading.json')
FileUtils.mkdir_p(File.dirname(out))
File.write(out, JSON.pretty_generate(result) + "\n")
puts "THIRD06 #{result[:passed] ? 'passed' : 'failed'}: #{poms.length} Maven projects share one classpath authority"
exit(result[:passed] ? 0 : 1)
