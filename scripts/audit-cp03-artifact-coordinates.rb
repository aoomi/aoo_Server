#!/usr/bin/env ruby
# frozen_string_literal: true

require 'json'
require 'rexml/document'
require 'time'

ROOT = File.expand_path('..', __dir__)
OUTPUT = File.join(ROOT, 'docs/generated/cp03-artifact-coordinates.json')
TASKS = File.join(ROOT, 'docs/Aoo-前后端全框架功能通信审计任务清单.md')
MANIFEST = File.join(ROOT, 'docs/generated/generated-artifact-manifest.json')

rows = Dir[File.join(ROOT, '**/pom.xml')].reject { |path| path.include?('/target/') || path.include?('/build/') }.sort.map do |path|
  document = REXML::Document.new(File.read(path, encoding: 'UTF-8'))
  project = document.root
  group_id = project.elements['groupId']&.text || project.elements['parent/groupId']&.text
  artifact_id = project.elements['artifactId']&.text
  version = project.elements['version']&.text || project.elements['parent/version']&.text
  { path: path.delete_prefix(ROOT + '/'), groupId: group_id, artifactId: artifact_id, version: version }
end
coordinate_groups = rows.group_by { |row| [row[:groupId], row[:artifactId], row[:version]] }
artifact_groups = rows.group_by { |row| row[:artifactId] }
coordinate_conflicts = coordinate_groups.each_with_object([]) { |(coordinate, entries), conflicts| conflicts << { coordinate: coordinate.join(':'), paths: entries.map { |row| row[:path] } } if entries.size > 1 }
artifact_conflicts = artifact_groups.each_with_object([]) { |(artifact_id, entries), conflicts| conflicts << { artifactId: artifact_id, paths: entries.map { |row| row[:path] } } if entries.size > 1 }
report = {
  schemaVersion: 1,
  generatedAt: Time.now.utc.iso8601,
  task: 'CP03',
  pomCount: rows.size,
  coordinateCount: coordinate_groups.size,
  coordinateConflicts: coordinate_conflicts,
  artifactIdConflicts: artifact_conflicts,
  passed: coordinate_conflicts.empty? && artifact_conflicts.empty?,
  artifacts: rows
}
File.write(OUTPUT, JSON.pretty_generate(report) + "\n", mode: 'w:UTF-8')

tasks = File.read(TASKS, encoding: 'UTF-8')
status = report[:passed] ? '已完成' : '失败跳过'
result = report[:passed] ? '通过' : '未完成'
detail = report[:passed] ? "已对 #{report[:pomCount]} 个有效及隔离 Maven POM 建立 groupId/artifactId/version 与 artifactId 双重唯一门禁；旧大厅已改为 legacy-game-hall-quarantine，坐标冲突为 0。" : "发现坐标冲突 #{coordinate_conflicts.size} 组、artifactId 冲突 #{artifact_conflicts.size} 组，已记录后跳过。"
tasks.sub!(/^\| CP03 \|.*$/, "| CP03 | #{status} | artifactId 冲突 | #{result} | #{detail} 证据：docs/generated/cp03-artifact-coordinates.json |")
File.write(TASKS, tasks, mode: 'w:UTF-8')

manifest = JSON.parse(File.read(MANIFEST, encoding: 'UTF-8'))
path = 'docs/generated/cp03-artifact-coordinates.json'
unless manifest.fetch('artifacts').any? { |artifact| artifact['path'] == path }
  manifest.fetch('artifacts') << {
    'path' => path,
    'kind' => 'generated-build-evidence',
    'authoritativeInputs' => ['pom.xml', 'server/**/pom.xml'],
    'generator' => 'scripts/audit-cp03-artifact-coordinates.rb',
    'rebuild' => 'ruby scripts/audit-cp03-artifact-coordinates.rb',
    'owner' => 'build-and-classpath',
    'editPolicy' => 'generated-do-not-edit'
  }
end
File.write(MANIFEST, JSON.pretty_generate(manifest) + "\n", mode: 'w:UTF-8')
puts JSON.generate(report.slice(:task, :pomCount, :coordinateCount, :coordinateConflicts, :artifactIdConflicts, :passed))
exit 1 unless report[:passed]
