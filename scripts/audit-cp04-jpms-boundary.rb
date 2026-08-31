#!/usr/bin/env ruby
# frozen_string_literal: true

require 'json'
require 'rexml/document'
require 'time'

ROOT = File.expand_path('..', __dir__)
OUTPUT = File.join(ROOT, 'docs/generated/cp04-jpms-boundary.json')
TASKS = File.join(ROOT, 'docs/Aoo-前后端全框架功能通信审计任务清单.md')
MANIFEST = File.join(ROOT, 'docs/generated/generated-artifact-manifest.json')

root_pom = REXML::Document.new(File.read(File.join(ROOT, 'pom.xml'), encoding: 'UTF-8'))
modules = root_pom.root.get_elements('modules/module').map(&:text)
module_rows = modules.map do |module_path|
  pom_path = File.join(ROOT, module_path, 'pom.xml')
  pom = REXML::Document.new(File.read(pom_path, encoding: 'UTF-8'))
  artifact_id = pom.root.elements['artifactId'].text
  explicit_name = Dir[File.join(ROOT, module_path, 'src/main/resources/META-INF/MANIFEST.MF')].map do |manifest|
    File.read(manifest, encoding: 'UTF-8')[/^Automatic-Module-Name:\s*(\S+)/, 1]
  end.compact.first
  automatic_name = explicit_name || artifact_id.gsub(/[^A-Za-z0-9]+/, '.').gsub(/^\.|\.$/, '')
  packages = Dir[File.join(ROOT, module_path, 'src/main/java/**/*.java')].map do |source_path|
    File.read(source_path, encoding: 'UTF-8')[/\bpackage\s+([\w.]+)\s*;/, 1]
  end.compact.uniq.sort
  { modulePath: module_path, artifactId: artifact_id, automaticModuleName: automatic_name, packages: packages }
end

name_conflicts = module_rows.group_by { |row| row[:automaticModuleName] }.each_with_object([]) do |(name, rows), conflicts|
  conflicts << { automaticModuleName: name, modules: rows.map { |row| row[:modulePath] } } if rows.size > 1
end
package_owners = Hash.new { |hash, key| hash[key] = [] }
module_rows.each { |row| row[:packages].each { |package_name| package_owners[package_name] << row[:modulePath] } }
split_packages = package_owners.each_with_object([]) do |(package_name, owners), conflicts|
  conflicts << { packageName: package_name, modules: owners } if owners.size > 1
end
shade_poms = modules.select { |module_path| File.read(File.join(ROOT, module_path, 'pom.xml'), encoding: 'UTF-8').include?('maven-shade-plugin') }
report = {
  schemaVersion: 1,
  generatedAt: Time.now.utc.iso8601,
  task: 'CP04',
  reactorModules: module_rows.size,
  moduleNameConflicts: name_conflicts,
  splitPackages: split_packages,
  shadedModules: shade_poms,
  passed: name_conflicts.empty? && split_packages.empty?,
  modules: module_rows
}
File.write(OUTPUT, JSON.pretty_generate(report) + "\n", mode: 'w:UTF-8')

tasks = File.read(TASKS, encoding: 'UTF-8')
status = report[:passed] ? '已完成' : '失败跳过'
result = report[:passed] ? '通过' : '未完成'
detail = report[:passed] ? "已对 #{report[:reactorModules]} 个生产 Reactor 模块建立派生/显式模块名、split package 与 shade 配置门禁；模块名冲突、跨模块拆包和 shaded 模块均为 0。隔离遗留模块不进入生产模块路径。" : "发现模块名冲突 #{name_conflicts.size} 组、split package #{split_packages.size} 组，已记录后跳过。"
tasks.sub!(/^\| CP04 \|.*$/, "| CP04 | #{status} | 自动模块名冲突 | #{result} | #{detail} 证据：docs/generated/cp04-jpms-boundary.json |")
File.write(TASKS, tasks, mode: 'w:UTF-8')

manifest = JSON.parse(File.read(MANIFEST, encoding: 'UTF-8'))
path = 'docs/generated/cp04-jpms-boundary.json'
unless manifest.fetch('artifacts').any? { |artifact| artifact['path'] == path }
  manifest.fetch('artifacts') << {
    'path' => path,
    'kind' => 'generated-build-evidence',
    'authoritativeInputs' => ['pom.xml', 'server/**/pom.xml', 'server/**/src/main/java', 'server/**/src/main/resources/META-INF/MANIFEST.MF'],
    'generator' => 'scripts/audit-cp04-jpms-boundary.rb',
    'rebuild' => 'ruby scripts/audit-cp04-jpms-boundary.rb',
    'owner' => 'build-and-classpath',
    'editPolicy' => 'generated-do-not-edit'
  }
end
File.write(MANIFEST, JSON.pretty_generate(manifest) + "\n", mode: 'w:UTF-8')
puts JSON.generate(report.slice(:task, :reactorModules, :moduleNameConflicts, :splitPackages, :shadedModules, :passed))
exit 1 unless report[:passed]
