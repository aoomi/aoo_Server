#!/usr/bin/env ruby
# frozen_string_literal: true

require 'digest'
require 'find'
require 'json'
require 'time'

ROOT = File.expand_path('..', __dir__)
OUTPUT = File.join(ROOT, 'docs/generated/cp02-resource-collisions.json')
TASKS = File.join(ROOT, 'docs/Aoo-前后端全框架功能通信审计任务清单.md')
MANIFEST = File.join(ROOT, 'docs/generated/generated-artifact-manifest.json')
MERGEABLE_PREFIXES = ['META-INF/services/'].freeze

roots = Dir[
  File.join(ROOT, 'server/**/src/main/resources'),
  File.join(ROOT, 'server/**/target/generated-resources/**'),
  File.join(ROOT, 'server/**/build/generated/resources/**')
].select { |path| File.directory?(path) }.uniq.sort
owners = Hash.new { |hash, key| hash[key] = [] }
roots.each do |resource_root|
  Find.find(resource_root) do |path|
    next unless File.file?(path)
    relative = path.delete_prefix(resource_root + '/')
    owners[relative] << {
      path: path.delete_prefix(ROOT + '/'),
      sha256: Digest::SHA256.file(path).hexdigest
    }
  end
end

duplicates = owners.filter { |_path, entries| entries.size > 1 }.map do |path, entries|
  mergeable = MERGEABLE_PREFIXES.any? { |prefix| path.start_with?(prefix) }
  { resourcePath: path, mergeable: mergeable, entries: entries }
end
blocking = duplicates.reject { |row| row[:mergeable] }
report = {
  schemaVersion: 1,
  generatedAt: Time.now.utc.iso8601,
  task: 'CP02',
  resourceRoots: roots.map { |path| path.delete_prefix(ROOT + '/') },
  uniqueResourcePaths: owners.size,
  duplicatePaths: duplicates.size,
  mergeablePaths: duplicates.count { |row| row[:mergeable] },
  blockingCollisions: blocking,
  duplicates: duplicates,
  passed: blocking.empty?,
  policy: 'Only META-INF/services entries may share a path; their packaged merge completeness is enforced separately by CP07.'
}
File.write(OUTPUT, JSON.pretty_generate(report) + "\n", mode: 'w:UTF-8')

tasks = File.read(TASKS, encoding: 'UTF-8')
status = report[:passed] ? '已完成' : '失败跳过'
result = report[:passed] ? '通过' : '未完成'
detail = report[:passed] ? "已建立主资源与生成资源同路径覆盖门禁；#{report[:uniqueResourcePaths]} 个资源路径中无不可合并覆盖，唯一重复路径为 ServiceLoader 声明并转交 CP07 验证聚合完整性。" : "发现 #{blocking.size} 个不可合并资源同路径覆盖，已记录路径和内容哈希并按规则跳过。"
tasks.sub!(/^\| CP02 \|.*$/, "| CP02 | #{status} | 资源同路径覆盖 | #{result} | #{detail} 证据：docs/generated/cp02-resource-collisions.json |")
File.write(TASKS, tasks, mode: 'w:UTF-8')

manifest = JSON.parse(File.read(MANIFEST, encoding: 'UTF-8'))
path = 'docs/generated/cp02-resource-collisions.json'
unless manifest.fetch('artifacts').any? { |artifact| artifact['path'] == path }
  manifest.fetch('artifacts') << {
    'path' => path,
    'kind' => 'generated-classpath-evidence',
    'authoritativeInputs' => ['server/**/src/main/resources', 'server/**/target/generated-resources', 'server/**/build/generated/resources'],
    'generator' => 'scripts/audit-cp02-resource-collisions.rb',
    'rebuild' => 'ruby scripts/audit-cp02-resource-collisions.rb',
    'owner' => 'build-and-classpath',
    'editPolicy' => 'generated-do-not-edit'
  }
end
File.write(MANIFEST, JSON.pretty_generate(manifest) + "\n", mode: 'w:UTF-8')
puts JSON.generate(report.slice(:task, :uniqueResourcePaths, :duplicatePaths, :mergeablePaths, :passed))
exit 1 unless report[:passed]
