#!/usr/bin/env ruby
# frozen_string_literal: true

require 'json'
require 'rexml/document'
require 'time'

ROOT = File.expand_path('..', __dir__)
OUTPUT = File.join(ROOT, 'docs/generated/cp12-clean-isolated.json')
TASKS = File.join(ROOT, 'docs/Aoo-前后端全框架功能通信审计任务清单.md')
MANIFEST = File.join(ROOT, 'docs/generated/generated-artifact-manifest.json')
LOG = File.join(ROOT, 'work/audit/cp12-clean-isolated.log')
MARKER = File.join(ROOT, 'work/audit/cp12-clean-isolated.marker')
REPOSITORY = File.join(ROOT, 'work/audit/cp12-m2')

root_pom = REXML::Document.new(File.read(File.join(ROOT, 'pom.xml'), encoding: 'UTF-8'))
modules = root_pom.root.get_elements('modules/module').map(&:text)
jar_modules = modules.select do |module_path|
  pom = REXML::Document.new(File.read(File.join(ROOT, module_path, 'pom.xml'), encoding: 'UTF-8'))
  (pom.root.elements['packaging']&.text || 'jar') == 'jar'
end
marker_time = File.exist?(MARKER) ? File.mtime(MARKER) : Time.now
fresh_jars = jar_modules.each_with_object({}) do |module_path, rows|
  candidates = Dir[File.join(ROOT, module_path, 'target/*.jar')].reject { |path| path.end_with?('-sources.jar', '-javadoc.jar') }
  rows[module_path] = candidates.select { |path| File.mtime(path) >= marker_time }.map { |path| File.basename(path) }
end
test_reports = Dir[File.join(ROOT, 'server/**/target/surefire-reports/TEST-*.xml')]
test_failures = test_reports.select do |path|
  suite = REXML::Document.new(File.read(path, encoding: 'UTF-8')).root
  suite.attributes['failures'].to_i.positive? || suite.attributes['errors'].to_i.positive?
end
log = File.exist?(LOG) ? File.read(LOG, encoding: 'UTF-8') : ''
checks = {
  isolatedRepositoryUsed: File.directory?(REPOSITORY) && Dir[File.join(REPOSITORY, '**/*')].any? { |path| File.file?(path) },
  cleanVerifySucceeded: log.include?('BUILD SUCCESS') && !log.include?('BUILD FAILURE'),
  allReactorJarsFresh: fresh_jars.size == jar_modules.size && fresh_jars.values.all? { |jars| !jars.empty? },
  testsExecuted: !test_reports.empty? && log.include?(' T E S T S'),
  noTestFailures: test_failures.empty?
}
report = {
  schemaVersion: 1,
  generatedAt: Time.now.utc.iso8601,
  task: 'CP12',
  repository: 'work/audit/cp12-m2',
  reactorModules: modules.size,
  jarModules: jar_modules.size,
  freshJars: fresh_jars,
  testReports: test_reports.size,
  testFailures: test_failures.map { |path| path.delete_prefix(ROOT + '/') },
  checks: checks,
  passed: checks.values.all?
}
File.write(OUTPUT, JSON.pretty_generate(report) + "\n", mode: 'w:UTF-8')

tasks = File.read(TASKS, encoding: 'UTF-8')
status = report[:passed] ? '已完成' : '失败跳过'
result = report[:passed] ? '通过' : '未完成'
detail = report[:passed] ? "已在项目专属隔离 Maven 仓库执行 clean verify；#{modules.size} 个 Reactor 模块全部成功，#{jar_modules.size} 个 JAR 均为清理后新产物，#{test_reports.size} 份测试报告无失败，证明不依赖项目 target 或默认 ~/.m2 的残留内部 SNAPSHOT。" : "隔离仓库干净验证未通过：#{checks.reject { |_name, passed| passed }.keys.join('、')}，已保留日志并按规则跳过。"
tasks.sub!(/^\| CP12 \|.*$/, "| CP12 | #{status} | 干净仓库验证 | #{result} | #{detail} 证据：docs/generated/cp12-clean-isolated.json、work/audit/cp12-clean-isolated.log |")
File.write(TASKS, tasks, mode: 'w:UTF-8')

manifest = JSON.parse(File.read(MANIFEST, encoding: 'UTF-8'))
path = 'docs/generated/cp12-clean-isolated.json'
unless manifest.fetch('artifacts').any? { |artifact| artifact['path'] == path }
  manifest.fetch('artifacts') << {
    'path' => path,
    'kind' => 'generated-build-evidence',
    'authoritativeInputs' => ['pom.xml', 'server/**/pom.xml', 'tools/verify-clean-isolated-reactor.sh', 'work/audit/cp12-clean-isolated.log'],
    'generator' => 'scripts/audit-cp12-clean-isolated.rb',
    'rebuild' => 'tools/verify-clean-isolated-reactor.sh',
    'owner' => 'build-and-classpath',
    'editPolicy' => 'generated-do-not-edit'
  }
end
File.write(MANIFEST, JSON.pretty_generate(manifest) + "\n", mode: 'w:UTF-8')
puts JSON.generate(report.slice(:task, :reactorModules, :jarModules, :testReports, :checks, :passed))
exit 1 unless report[:passed]
