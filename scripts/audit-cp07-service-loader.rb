#!/usr/bin/env ruby
# frozen_string_literal: true

require 'json'
require 'rexml/document'
require 'time'

ROOT = File.expand_path('..', __dir__)
OUTPUT = File.join(ROOT, 'docs/generated/cp07-service-loader.json')
TASKS = File.join(ROOT, 'docs/Aoo-前后端全框架功能通信审计任务清单.md')
MANIFEST = File.join(ROOT, 'docs/generated/generated-artifact-manifest.json')
SERVICE = 'META-INF/services/com.aoo.bcg.gamespi.GameProvider'

declarations = Dir[File.join(ROOT, 'server/*/src/main/resources', SERVICE)].sort.flat_map do |path|
  File.readlines(path, chomp: true, encoding: 'UTF-8').map(&:strip).reject { |line| line.empty? || line.start_with?('#') }.map do |provider|
    { module: path.split('/')[-6], provider: provider, resource: path.delete_prefix(ROOT + '/') }
  end
end
report_path = File.join(ROOT, 'server/Bootstrap/target/surefire-reports/TEST-com.aoo.bcg.bootstrap.GameProviderServiceLoaderTest.xml')
test_passed = false
test_count = 0
if File.file?(report_path)
  document = REXML::Document.new(File.read(report_path, encoding: 'UTF-8'))
  suite = document.root
  test_count = suite.attributes['tests'].to_i
  test_passed = test_count == 1 && suite.attributes['failures'].to_i.zero? && suite.attributes['errors'].to_i.zero?
end
duplicates = declarations.group_by { |row| row[:provider] }.select { |_provider, rows| rows.size > 1 }.keys
checks = {
  sixNativeDeclarations: declarations.size == 6,
  uniqueProviderDeclarations: duplicates.empty?,
  runtimeServiceLoaderTestPassed: test_passed,
  thinClasspathAssembly: !File.read(File.join(ROOT, 'server/Bootstrap/pom.xml'), encoding: 'UTF-8').include?('maven-shade-plugin')
}
report = {
  schemaVersion: 1,
  generatedAt: Time.now.utc.iso8601,
  task: 'CP07',
  assemblyPolicy: 'Thin application classpath: provider resources remain in dependency jars; no shade-time overwrite is permitted.',
  declarations: declarations,
  duplicateProviders: duplicates,
  testReport: report_path.delete_prefix(ROOT + '/'),
  checks: checks,
  passed: checks.values.all?
}
File.write(OUTPUT, JSON.pretty_generate(report) + "\n", mode: 'w:UTF-8')

tasks = File.read(TASKS, encoding: 'UTF-8')
status = report[:passed] ? '已完成' : '失败跳过'
result = report[:passed] ? '通过' : '未完成'
detail = report[:passed] ? '生产装配明确采用 thin classpath 而非 shade；六个原生玩法 ServiceLoader 声明唯一，Bootstrap 实际测试从依赖 JAR 发现六个 Provider 且无重复，资源不会被单 JAR 覆盖。' : "ServiceLoader 装配未通过：#{checks.reject { |_name, passed| passed }.keys.join('、')}，已记录后跳过。"
tasks.sub!(/^\| CP07 \|.*$/, "| CP07 | #{status} | ServiceLoader 合并 | #{result} | #{detail} 证据：docs/generated/cp07-service-loader.json、#{report[:testReport]} |")
File.write(TASKS, tasks, mode: 'w:UTF-8')

manifest = JSON.parse(File.read(MANIFEST, encoding: 'UTF-8'))
path = 'docs/generated/cp07-service-loader.json'
unless manifest.fetch('artifacts').any? { |artifact| artifact['path'] == path }
  manifest.fetch('artifacts') << {
    'path' => path,
    'kind' => 'generated-classpath-evidence',
    'authoritativeInputs' => ['server/*/src/main/resources/META-INF/services/com.aoo.bcg.gamespi.GameProvider', 'server/Bootstrap/src/test/java/com/aoo/bcg/bootstrap/GameProviderServiceLoaderTest.java'],
    'generator' => 'scripts/audit-cp07-service-loader.rb',
    'rebuild' => './mvnw -Dexec.skip=true -pl server/Bootstrap -am -Dtest=GameProviderServiceLoaderTest -Dsurefire.failIfNoSpecifiedTests=false test && ruby scripts/audit-cp07-service-loader.rb',
    'owner' => 'build-and-classpath',
    'editPolicy' => 'generated-do-not-edit'
  }
end
File.write(MANIFEST, JSON.pretty_generate(manifest) + "\n", mode: 'w:UTF-8')
puts JSON.generate(report.slice(:task, :checks, :passed))
exit 1 unless report[:passed]
