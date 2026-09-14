#!/usr/bin/env ruby
# frozen_string_literal: true

require 'json'
require 'rexml/document'
require 'set'
require 'time'

ROOT = File.expand_path('..', __dir__)
OUTPUT = File.join(ROOT, 'docs/generated/cp05-reactor-order.json')
TASKS = File.join(ROOT, 'docs/Aoo-前后端全框架功能通信审计任务清单.md')
MANIFEST = File.join(ROOT, 'docs/generated/generated-artifact-manifest.json')
LOG = File.join(ROOT, 'work/audit/cp05-reactor-package.log')
MARKER = File.join(ROOT, 'work/audit/cp05-reactor-start.marker')

root_pom = REXML::Document.new(File.read(File.join(ROOT, 'pom.xml'), encoding: 'UTF-8'))
modules = root_pom.root.get_elements('modules/module').map(&:text)
rows = modules.map do |module_path|
  document = REXML::Document.new(File.read(File.join(ROOT, module_path, 'pom.xml'), encoding: 'UTF-8'))
  artifact_id = document.root.elements['artifactId'].text
  packaging = document.root.elements['packaging']&.text || 'jar'
  dependencies = document.root.get_elements('dependencies/dependency').map do |dependency|
    group_id = dependency.elements['groupId']&.text
    dependency.elements['artifactId']&.text if group_id == 'com.aoo.bcg'
  end.compact
  { modulePath: module_path, artifactId: artifact_id, packaging: packaging, dependencies: dependencies }
end
active_artifacts = rows.map { |row| row[:artifactId] }.to_set
unresolved = rows.flat_map do |row|
  row[:dependencies].reject { |dependency| active_artifacts.include?(dependency) }.map { |dependency| { module: row[:modulePath], dependency: dependency } }
end
log = File.exist?(LOG) ? File.read(LOG, encoding: 'UTF-8') : ''
marker_time = File.exist?(MARKER) ? File.mtime(MARKER) : Time.now
jar_modules = rows.select { |row| row[:packaging] == 'jar' }
present_jars = jar_modules.map do |row|
  jars = Dir[File.join(ROOT, row[:modulePath], 'target/*.jar')].reject { |path| path.end_with?('-sources.jar', '-javadoc.jar') }
  jars.first
end.compact
report = {
  schemaVersion: 1,
  generatedAt: Time.now.utc.iso8601,
  task: 'CP05',
  reactorModules: rows.size,
  internalDependencies: rows.sum { |row| row[:dependencies].size },
  unresolvedInternalDependencies: unresolved,
  buildSuccess: log.include?('BUILD SUCCESS'),
  buildFailure: log.include?('BUILD FAILURE'),
  packagedJarModules: present_jars.size,
  expectedJarModules: jar_modules.size,
  passed: unresolved.empty? && log.include?('BUILD SUCCESS') && !log.include?('BUILD FAILURE') && present_jars.size == jar_modules.size,
  modules: rows
}
File.write(OUTPUT, JSON.pretty_generate(report) + "\n", mode: 'w:UTF-8')

tasks = File.read(TASKS, encoding: 'UTF-8')
status = report[:passed] ? '已完成' : '失败跳过'
result = report[:passed] ? '通过' : '未完成'
detail = report[:passed] ? "已从父 Reactor 一次性 package #{report[:reactorModules]} 个子模块，#{report[:expectedJarModules]} 个 jar 模块均产生产物，内部依赖均由同一 Reactor 解析且 BUILD SUCCESS；不再以单模块残留 SNAPSHOT 作为顺序证据。" : "父 Reactor 构建或产物完整性未通过：未解析内部依赖 #{unresolved.size}、产物模块 #{present_jars.size}/#{jar_modules.size}，已记录后跳过。"
tasks.sub!(/^\| CP05 \|.*$/, "| CP05 | #{status} | Maven reactor 顺序 | #{result} | #{detail} 证据：docs/generated/cp05-reactor-order.json、work/audit/cp05-reactor-package.log |")
File.write(TASKS, tasks, mode: 'w:UTF-8')

manifest = JSON.parse(File.read(MANIFEST, encoding: 'UTF-8'))
path = 'docs/generated/cp05-reactor-order.json'
unless manifest.fetch('artifacts').any? { |artifact| artifact['path'] == path }
  manifest.fetch('artifacts') << {
    'path' => path,
    'kind' => 'generated-build-evidence',
    'authoritativeInputs' => ['pom.xml', 'server/**/pom.xml', 'work/audit/cp05-reactor-package.log'],
    'generator' => 'scripts/audit-cp05-reactor-order.rb',
    'rebuild' => 'date > work/audit/cp05-reactor-start.marker && ./mvnw -Dexec.skip=true -DskipTests package > work/audit/cp05-reactor-package.log 2>&1 && ruby scripts/audit-cp05-reactor-order.rb',
    'owner' => 'build-and-classpath',
    'editPolicy' => 'generated-do-not-edit'
  }
end
File.write(MANIFEST, JSON.pretty_generate(manifest) + "\n", mode: 'w:UTF-8')
puts JSON.generate(report.slice(:task, :reactorModules, :unresolvedInternalDependencies, :buildSuccess, :packagedJarModules, :expectedJarModules, :passed))
exit 1 unless report[:passed]
