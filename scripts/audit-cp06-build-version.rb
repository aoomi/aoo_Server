#!/usr/bin/env ruby
# frozen_string_literal: true

require 'json'
require 'open3'
require 'rexml/document'
require 'time'

ROOT = File.expand_path('..', __dir__)
OUTPUT = File.join(ROOT, 'docs/generated/cp06-build-version.json')
TASKS = File.join(ROOT, 'docs/Aoo-前后端全框架功能通信审计任务清单.md')
MANIFEST = File.join(ROOT, 'docs/generated/generated-artifact-manifest.json')

root_pom = REXML::Document.new(File.read(File.join(ROOT, 'pom.xml'), encoding: 'UTF-8'))
modules = root_pom.root.get_elements('modules/module').map(&:text)
versions = [['pom.xml', root_pom.root.elements['version']&.text]]
modules.each do |module_path|
  pom = REXML::Document.new(File.read(File.join(ROOT, module_path, 'pom.xml'), encoding: 'UTF-8'))
  versions << ["#{module_path}/pom.xml", pom.root.elements['parent/version']&.text]
end
first, first_error, first_status = Open3.capture3('ruby', 'tools/aoo-build-version.rb', chdir: ROOT)
second, second_error, second_status = Open3.capture3('ruby', 'tools/aoo-build-version.rb', chdir: ROOT)
revision = first.strip
jdk = File.expand_path('../.toolchains/jdk-26.0.2.1.jdk/Contents/Home', ROOT)
environment = { 'JAVA_HOME' => jdk, 'PATH' => "#{jdk}/bin:#{ENV.fetch('PATH')}" }
evaluated, evaluate_error, evaluate_status = Open3.capture3(environment, './mvnw', '-q', "-Drevision=#{revision}", '-DforceStdout', 'help:evaluate', '-Dexpression=project.version', chdir: ROOT)
checks = {
  ciFriendlyVersionInEveryReactorPom: versions.all? { |_path, version| version == '${revision}' },
  deterministicContentVersion: first_status.success? && second_status.success? && revision == second.strip && revision.match?(/\A1\.0\.0-[0-9a-f]{16}\z/),
  nonSnapshotReleaseVersion: !revision.end_with?('-SNAPSHOT'),
  mavenResolvesRevision: evaluate_status.success? && evaluated.lines.map(&:strip).include?(revision),
  releaseWrapperExists: File.executable?(File.join(ROOT, 'tools/build-release.sh')),
  fixedOutputTimestamp: File.read(File.join(ROOT, 'tools/build-release.sh'), encoding: 'UTF-8').include?('project.build.outputTimestamp')
}
report = {
  schemaVersion: 1,
  generatedAt: Time.now.utc.iso8601,
  task: 'CP06',
  revision: revision,
  checks: checks,
  reactorPomVersions: versions.to_h,
  diagnostics: [first_error, second_error, evaluate_error].reject(&:empty?),
  passed: checks.values.all?
}
File.write(OUTPUT, JSON.pretty_generate(report) + "\n", mode: 'w:UTF-8')

tasks = File.read(TASKS, encoding: 'UTF-8')
status = report[:passed] ? '已完成' : '失败跳过'
result = report[:passed] ? '通过' : '未完成'
detail = report[:passed] ? "全部 #{versions.size} 个 Reactor POM 已切换 Maven CI-friendly revision；发布入口按源码、迁移、构建脚本内容生成确定性版本 #{revision}，拒绝 SNAPSHOT 并固定输出时间戳，Maven 实际解析一致。" : "内容寻址版本门禁未通过：#{checks.reject { |_name, passed| passed }.keys.join('、')}，已记录后跳过。"
tasks.sub!(/^\| CP06 \|.*$/, "| CP06 | #{status} | SNAPSHOT 覆盖 | #{result} | #{detail} 证据：docs/generated/cp06-build-version.json、tools/build-release.sh |")
File.write(TASKS, tasks, mode: 'w:UTF-8')

manifest = JSON.parse(File.read(MANIFEST, encoding: 'UTF-8'))
path = 'docs/generated/cp06-build-version.json'
unless manifest.fetch('artifacts').any? { |artifact| artifact['path'] == path }
  manifest.fetch('artifacts') << {
    'path' => path,
    'kind' => 'generated-build-evidence',
    'authoritativeInputs' => ['pom.xml', 'server/**/pom.xml', 'tools/aoo-build-version.rb', 'tools/build-release.sh'],
    'generator' => 'scripts/audit-cp06-build-version.rb',
    'rebuild' => 'ruby scripts/audit-cp06-build-version.rb',
    'owner' => 'build-and-classpath',
    'editPolicy' => 'generated-do-not-edit'
  }
end
File.write(MANIFEST, JSON.pretty_generate(manifest) + "\n", mode: 'w:UTF-8')
puts JSON.generate(report.slice(:task, :revision, :checks, :passed))
exit 1 unless report[:passed]
