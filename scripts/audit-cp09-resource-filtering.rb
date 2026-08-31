#!/usr/bin/env ruby
# frozen_string_literal: true

require 'digest'
require 'json'
require 'rexml/document'
require 'time'

ROOT = File.expand_path('..', __dir__)
OUTPUT = File.join(ROOT, 'docs/generated/cp09-resource-filtering.json')
TASKS = File.join(ROOT, 'docs/Aoo-前后端全框架功能通信审计任务清单.md')
MANIFEST = File.join(ROOT, 'docs/generated/generated-artifact-manifest.json')

root_pom = REXML::Document.new(File.read(File.join(ROOT, 'pom.xml'), encoding: 'UTF-8'))
modules = root_pom.root.get_elements('modules/module').map(&:text)
declared_resources = []
filtering_enabled = []
modules.each do |module_path|
  pom = REXML::Document.new(File.read(File.join(ROOT, module_path, 'pom.xml'), encoding: 'UTF-8'))
  pom.root.get_elements('build/resources/resource').each do |resource|
    filtering = resource.elements['filtering']&.text || 'false(default)'
    row = { module: module_path, directory: resource.elements['directory']&.text, filtering: filtering }
    declared_resources << row
    filtering_enabled << row if filtering == 'true'
  end
end

copy_checks = []
Dir[File.join(ROOT, 'server/*/src/main/resources/**/*')].select { |path| File.file?(path) }.each do |source|
  module_path = source[%r{\A#{Regexp.escape(ROOT)}/(server/[^/]+)/}, 1]
  relative = source.split('/src/main/resources/', 2).last
  target = File.join(ROOT, module_path, 'target/classes', relative)
  next unless File.file?(target)
  copy_checks << {
    source: source.delete_prefix(ROOT + '/'),
    target: target.delete_prefix(ROOT + '/'),
    byteIdentical: Digest::SHA256.file(source).hexdigest == Digest::SHA256.file(target).hexdigest
  }
end
Dir[File.join(ROOT, 'database/migrations/*.sql')].each do |source|
  target = File.join(ROOT, 'server/gameServer/target/classes/db/migration', File.basename(source))
  next unless File.file?(target)
  copy_checks << {
    source: source.delete_prefix(ROOT + '/'),
    target: target.delete_prefix(ROOT + '/'),
    byteIdentical: Digest::SHA256.file(source).hexdigest == Digest::SHA256.file(target).hexdigest
  }
end
root_text = File.read(File.join(ROOT, 'pom.xml'), encoding: 'UTF-8')
checks = {
  noFilteringEnabled: filtering_enabled.empty?,
  allObservedCopiesByteIdentical: !copy_checks.empty? && copy_checks.all? { |row| row[:byteIdentical] },
  binaryExtensionsProtected: %w[png jpg jpeg gif webp atlas skel proto sql].all? { |extension| root_text.include?("<nonFilteredFileExtension>#{extension}</nonFilteredFileExtension>") }
}
report = {
  schemaVersion: 1,
  generatedAt: Time.now.utc.iso8601,
  task: 'CP09',
  declaredResources: declared_resources,
  filteringEnabled: filtering_enabled,
  copyChecks: copy_checks,
  checks: checks,
  passed: checks.values.all?
}
File.write(OUTPUT, JSON.pretty_generate(report) + "\n", mode: 'w:UTF-8')

tasks = File.read(TASKS, encoding: 'UTF-8')
status = report[:passed] ? '已完成' : '失败跳过'
result = report[:passed] ? '通过' : '未完成'
detail = report[:passed] ? "生产资源过滤全部关闭，SQL、协议及图片/骨骼二进制扩展列入不可过滤白名单；#{copy_checks.size} 个已构建资源逐字节哈希与源文件一致，中文、占位符和密钥格式不会被 Maven 插值污染。" : "资源过滤门禁未通过：#{checks.reject { |_name, passed| passed }.keys.join('、')}，已记录后跳过。"
tasks.sub!(/^\| CP09 \|.*$/, "| CP09 | #{status} | 资源过滤污染 | #{result} | #{detail} 证据：docs/generated/cp09-resource-filtering.json |")
File.write(TASKS, tasks, mode: 'w:UTF-8')

manifest = JSON.parse(File.read(MANIFEST, encoding: 'UTF-8'))
path = 'docs/generated/cp09-resource-filtering.json'
unless manifest.fetch('artifacts').any? { |artifact| artifact['path'] == path }
  manifest.fetch('artifacts') << {
    'path' => path,
    'kind' => 'generated-build-evidence',
    'authoritativeInputs' => ['pom.xml', 'server/**/pom.xml', 'server/**/src/main/resources', 'database/migrations'],
    'generator' => 'scripts/audit-cp09-resource-filtering.rb',
    'rebuild' => './mvnw -Dexec.skip=true -DskipTests process-resources && ruby scripts/audit-cp09-resource-filtering.rb',
    'owner' => 'build-and-classpath',
    'editPolicy' => 'generated-do-not-edit'
  }
end
File.write(MANIFEST, JSON.pretty_generate(manifest) + "\n", mode: 'w:UTF-8')
puts JSON.generate(report.slice(:task, :checks, :passed))
exit 1 unless report[:passed]
