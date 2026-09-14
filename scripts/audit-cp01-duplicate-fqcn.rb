#!/usr/bin/env ruby
# frozen_string_literal: true

require 'find'
require 'json'
require 'time'

ROOT = File.expand_path('..', __dir__)
OUTPUT = File.join(ROOT, 'docs/generated/cp01-duplicate-fqcn.json')
TASKS = File.join(ROOT, 'docs/Aoo-前后端全框架功能通信审计任务清单.md')
MANIFEST = File.join(ROOT, 'docs/generated/generated-artifact-manifest.json')

roots = Dir[
  File.join(ROOT, 'server/**/src/main/java'),
  File.join(ROOT, 'server/**/src/core/java'),
  File.join(ROOT, 'server/**/src/generated/java'),
  File.join(ROOT, 'server/**/target/generated-sources/**'),
  File.join(ROOT, 'server/**/build/generated/sources/**'),
  File.join(ROOT, 'third-party/**/src/**/java')
].select { |path| File.directory?(path) }.uniq.sort

def top_level_types(source)
  scrubbed = source.gsub(%r{(?:/\*.*?\*/)|(?://[^\r\n]*)|(?:"(?:\\.|[^"\\])*")|(?:'(?:\\.|[^'\\])*')}m) { |match| ' ' * match.length }
  depth = 0
  declarations = []
  token = /[{}]|(?:(?:public|protected|private|abstract|final|sealed|non-sealed|static|strictfp)\s+)*(?:class|record|interface|enum|@interface)\s+([A-Za-z_$][\w$]*)/
  scrubbed.scan(token) do |match|
    lexeme = Regexp.last_match(0)
    if lexeme == '{'
      depth += 1
    elsif lexeme == '}'
      depth -= 1
      depth = 0 if depth.negative?
    elsif depth.zero?
      declarations << match.first
    end
  end
  declarations
end

owners = Hash.new { |hash, key| hash[key] = [] }
files = []
roots.each do |source_root|
  Find.find(source_root) do |path|
    next unless File.file?(path) && path.end_with?('.java')
    source = File.read(path, encoding: 'UTF-8')
    package_name = source[/\bpackage\s+([\w.]+)\s*;/, 1]
    top_level_types(source).each do |type_name|
      fqcn = [package_name, type_name].compact.reject(&:empty?).join('.')
      owners[fqcn] << path.delete_prefix(ROOT + '/')
    end
    files << path
  rescue Encoding::InvalidByteSequenceError, Encoding::UndefinedConversionError
    owners['<encoding-error>'] << path.delete_prefix(ROOT + '/')
  end
end

duplicates = owners.each_with_object([]) do |(fqcn, paths), rows|
  unique_paths = paths.uniq
  rows << { fqcn: fqcn, paths: unique_paths } if unique_paths.size > 1 || fqcn == '<encoding-error>'
end
report = {
  schemaVersion: 1,
  generatedAt: Time.now.utc.iso8601,
  task: 'CP01',
  sourceRoots: roots.map { |path| path.delete_prefix(ROOT + '/') },
  scannedFiles: files.size,
  discoveredTypes: owners.size,
  duplicateCount: duplicates.size,
  duplicates: duplicates,
  passed: duplicates.empty?
}
File.write(OUTPUT, JSON.pretty_generate(report) + "\n", mode: 'w:UTF-8')

tasks = File.read(TASKS, encoding: 'UTF-8')
status = report[:passed] ? '已完成' : '失败跳过'
result = report[:passed] ? '通过' : '未完成'
detail = report[:passed] ? "已建立覆盖 Reactor 主源码、core、生成源码、build/target 生成目录和 third-party 源码的顶层类型 FQCN 门禁；扫描 #{report[:scannedFiles]} 个 Java 文件、#{report[:discoveredTypes]} 个类型，重复 0。" : "全范围 FQCN 扫描发现 #{report[:duplicateCount]} 组冲突，已生成冲突路径清单并按规则跳过。"
row = "| CP01 | #{status} | 全模块重复 FQCN | #{result} | #{detail} 证据：docs/generated/cp01-duplicate-fqcn.json |"
File.write(TASKS, tasks.sub(/^\| CP01 \|.*$/, row), mode: 'w:UTF-8') if tasks.match?(/^\| CP01 \|/)

manifest = JSON.parse(File.read(MANIFEST, encoding: 'UTF-8'))
path = 'docs/generated/cp01-duplicate-fqcn.json'
unless manifest.fetch('artifacts').any? { |artifact| artifact['path'] == path }
  manifest.fetch('artifacts') << {
    'path' => path,
    'kind' => 'generated-classpath-evidence',
    'authoritativeInputs' => ['server', 'third-party'],
    'generator' => 'scripts/audit-cp01-duplicate-fqcn.rb',
    'rebuild' => 'ruby scripts/audit-cp01-duplicate-fqcn.rb',
    'owner' => 'build-and-classpath',
    'editPolicy' => 'generated-do-not-edit'
  }
end
File.write(MANIFEST, JSON.pretty_generate(manifest) + "\n", mode: 'w:UTF-8')

puts JSON.generate(report.slice(:task, :scannedFiles, :discoveredTypes, :duplicateCount, :passed))
exit 1 unless report[:passed]
