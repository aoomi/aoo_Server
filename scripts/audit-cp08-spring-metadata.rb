#!/usr/bin/env ruby
# frozen_string_literal: true

require 'json'
require 'rexml/document'
require 'time'

ROOT = File.expand_path('..', __dir__)
OUTPUT = File.join(ROOT, 'docs/generated/cp08-spring-metadata.json')
TASKS = File.join(ROOT, 'docs/Aoo-前后端全框架功能通信审计任务清单.md')
MANIFEST = File.join(ROOT, 'docs/generated/generated-artifact-manifest.json')

root_pom = REXML::Document.new(File.read(File.join(ROOT, 'pom.xml'), encoding: 'UTF-8'))
modules = root_pom.root.get_elements('modules/module').map(&:text)
spring_dependencies = []
metadata = []
modules.each do |module_path|
  pom_path = File.join(ROOT, module_path, 'pom.xml')
  pom = File.read(pom_path, encoding: 'UTF-8')
  spring_dependencies << pom_path.delete_prefix(ROOT + '/') if pom.match?(%r{<groupId>org\.springframework(?:\.boot)?</groupId>})
  metadata.concat(Dir[
    File.join(ROOT, module_path, 'src/main/resources/META-INF/spring.factories'),
    File.join(ROOT, module_path, 'src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports'),
    File.join(ROOT, module_path, 'src/main/resources/META-INF/*spring-configuration-metadata.json')
  ].map { |path| path.delete_prefix(ROOT + '/') })
end
report = {
  schemaVersion: 1,
  generatedAt: Time.now.utc.iso8601,
  task: 'CP08',
  reactorModules: modules.size,
  springDependencies: spring_dependencies,
  springMetadata: metadata,
  legacyBoundary: 'server/LegacyAccountServer is outside the production reactor and retained only for comparison.',
  passed: spring_dependencies.empty? && metadata.empty?
}
File.write(OUTPUT, JSON.pretty_generate(report) + "\n", mode: 'w:UTF-8')

tasks = File.read(TASKS, encoding: 'UTF-8')
status = report[:passed] ? '已完成' : '失败跳过'
result = report[:passed] ? '通过·不适用' : '未完成'
detail = report[:passed] ? "生产 Reactor 的 #{modules.size} 个模块不依赖 Spring/Spring Boot，也不存在 spring.factories、AutoConfiguration imports 或配置元数据，故无合并覆盖面；遗留 Spring 账号服务已在 Reactor 外隔离。门禁会阻止生产模块静默引入未治理的 Spring 元数据。" : "生产 Reactor 发现 Spring 依赖 #{spring_dependencies.size} 项、元数据 #{metadata.size} 项但尚无合并策略，已记录后跳过。"
tasks.sub!(/^\| CP08 \|.*$/, "| CP08 | #{status} | Spring 元数据合并 | #{result} | #{detail} 证据：docs/generated/cp08-spring-metadata.json |")
File.write(TASKS, tasks, mode: 'w:UTF-8')

manifest = JSON.parse(File.read(MANIFEST, encoding: 'UTF-8'))
path = 'docs/generated/cp08-spring-metadata.json'
unless manifest.fetch('artifacts').any? { |artifact| artifact['path'] == path }
  manifest.fetch('artifacts') << {
    'path' => path,
    'kind' => 'generated-classpath-evidence',
    'authoritativeInputs' => ['pom.xml', 'server/**/pom.xml', 'server/**/src/main/resources/META-INF'],
    'generator' => 'scripts/audit-cp08-spring-metadata.rb',
    'rebuild' => 'ruby scripts/audit-cp08-spring-metadata.rb',
    'owner' => 'build-and-classpath',
    'editPolicy' => 'generated-do-not-edit'
  }
end
File.write(MANIFEST, JSON.pretty_generate(manifest) + "\n", mode: 'w:UTF-8')
puts JSON.generate(report.slice(:task, :reactorModules, :springDependencies, :springMetadata, :passed))
exit 1 unless report[:passed]
