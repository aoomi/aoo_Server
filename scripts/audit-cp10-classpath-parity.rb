#!/usr/bin/env ruby
# frozen_string_literal: true

require 'json'
require 'time'

ROOT = File.expand_path('..', __dir__)
OUTPUT = File.join(ROOT, 'docs/generated/cp10-classpath-parity.json')
TASKS = File.join(ROOT, 'docs/Aoo-前后端全框架功能通信审计任务清单.md')
MANIFEST = File.join(ROOT, 'docs/generated/generated-artifact-manifest.json')

runtime = File.read(File.join(ROOT, 'tools/runtime-java26.sh'), encoding: 'UTF-8')
launchers = Dir[File.join(ROOT, 'tools/start-*-local.sh')].sort.to_h { |path| [path.delete_prefix(ROOT + '/'), File.read(path, encoding: 'UTF-8')] }
launcher_modules = launchers.values.map { |content| content[/runtime_classpath\s+([^\)\s]+)/, 1] }.compact.uniq
materialized = launcher_modules.map do |module_path|
  jars = Dir[File.join(ROOT, module_path, 'target/*.jar')].reject { |path| path.end_with?('-sources.jar', '-javadoc.jar') }
  libraries = Dir[File.join(ROOT, module_path, 'target/runtime/lib/*.jar')]
  newest = jars.max_by { |path| File.mtime(path) }
  { module: module_path, applicationJar: newest && File.basename(newest), staleJarsIgnored: jars.reject { |path| path == newest }.map { |path| File.basename(path) }, dependencyJars: libraries.map { |path| File.basename(path) }.sort }
end
root_pom = File.read(File.join(ROOT, 'pom.xml'), encoding: 'UTF-8')
checks = {
  packagedJarInsteadOfClasses: runtime.include?("-name '*.jar'") && !runtime.include?('target/classes'),
  noAbsoluteClasspathFile: !runtime.include?('runtime-classpath.txt'),
  oneMavenMaterializationRule: root_pom.include?('<id>materialize-runtime-classpath</id>') && root_pom.include?('${project.build.directory}/runtime/lib'),
  allLaunchersUseSharedClasspath: launchers.values.all? { |content| content.include?('source "$ROOT_DIR/tools/runtime-java26.sh"') && content.include?('runtime_classpath') },
  allLauncherArtifactsMaterialized: materialized.all? { |row| row[:applicationJar] && !row[:dependencyJars].empty? },
  ideMetadataNotRuntimeInput: !runtime.match?(/\.iml|\.classpath|\.idea/)
}
report = {
  schemaVersion: 1,
  generatedAt: Time.now.utc.iso8601,
  task: 'CP10',
  classpathContract: '<module>/target/<module>.jar:<module>/target/runtime/lib/*',
  launcherModules: launcher_modules,
  materializedRuntime: materialized,
  checks: checks,
  passed: checks.values.all?
}
File.write(OUTPUT, JSON.pretty_generate(report) + "\n", mode: 'w:UTF-8')

tasks = File.read(TASKS, encoding: 'UTF-8')
status = report[:passed] ? '已完成' : '失败跳过'
result = report[:passed] ? '通过' : '未完成'
detail = report[:passed] ? "开发、本地与生产统一使用 Maven package 生成的模块 JAR + target/runtime/lib/*；三个启动器均走唯一 runtime_classpath，禁止 target/classes、绝对 classpath 文件、IDE 元数据和手工注入依赖模块。已物化 #{materialized.size} 个启动模块依赖集。" : "类路径一致性门禁未通过：#{checks.reject { |_name, passed| passed }.keys.join('、')}，已记录后跳过。"
tasks.sub!(/^\| CP10 \|.*$/, "| CP10 | #{status} | 开发生产类路径一致 | #{result} | #{detail} 证据：docs/generated/cp10-classpath-parity.json |")
File.write(TASKS, tasks, mode: 'w:UTF-8')

manifest = JSON.parse(File.read(MANIFEST, encoding: 'UTF-8'))
path = 'docs/generated/cp10-classpath-parity.json'
unless manifest.fetch('artifacts').any? { |artifact| artifact['path'] == path }
  manifest.fetch('artifacts') << {
    'path' => path,
    'kind' => 'generated-classpath-evidence',
    'authoritativeInputs' => ['pom.xml', 'tools/runtime-java26.sh', 'tools/start-*-local.sh'],
    'generator' => 'scripts/audit-cp10-classpath-parity.rb',
    'rebuild' => './mvnw -Dexec.skip=true -DskipTests package && ./mvnw -f server/LegacyGameHall/pom.xml -Dexec.skip=true -DskipTests package && ruby scripts/audit-cp10-classpath-parity.rb',
    'owner' => 'build-and-classpath',
    'editPolicy' => 'generated-do-not-edit'
  }
end
File.write(MANIFEST, JSON.pretty_generate(manifest) + "\n", mode: 'w:UTF-8')
puts JSON.generate(report.slice(:task, :launcherModules, :checks, :passed))
exit 1 unless report[:passed]
