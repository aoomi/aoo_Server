#!/usr/bin/env ruby
require 'json'
require 'open3'
require 'fileutils'

root = File.expand_path('..', __dir__)
inputs = %w[database/original database/migrations]
pattern = 'CREATE[[:space:]]+TABLE|ALTER[[:space:]]+TABLE|CREATE[[:space:]]+(UNIQUE[[:space:]]+)?INDEX|CONSTRAINT'
out, err, status = Open3.capture3('rg', '-n', '--no-heading', '-i', pattern, *inputs, chdir: root)
abort "schema extraction failed: #{err}" unless status.success? || status.exitstatus == 1
entries = out.lines.map do |line|
  file, line_no, ddl = line.split(':', 3)
  next unless ddl
  kind = case ddl
         when /CREATE\s+TABLE/i then 'table'
         when /ALTER\s+TABLE/i then 'alter'
         when /CREATE\s+(?:UNIQUE\s+)?INDEX/i then 'index'
         else 'constraint'
         end
  name = ddl[/`([^`]+)`/, 1] || ddl[/\b(?:TABLE|INDEX|CONSTRAINT)\s+(?:IF\s+NOT\s+EXISTS\s+)?([A-Za-z0-9_.]+)/i, 1]
  { file: file, line: line_no.to_i, kind: kind, object: name, ddl: ddl.strip[0, 800] }
end.compact.sort_by { |entry| [entry[:file], entry[:line], entry[:kind], entry[:object].to_s] }
files = Dir.glob(inputs.map { |path| File.join(root, path, '**/*.sql') }).map { |path| path.delete_prefix(root + '/') }.sort
by_file = files.to_h { |file| [file, entries.count { |entry| entry[:file] == file }] }
report = {
  schemaVersion: 1, authoritativeInputs: inputs, sqlFiles: files,
  summary: { statements: entries.length, tables: entries.count { |e| e[:kind] == 'table' }, alters: entries.count { |e| e[:kind] == 'alter' }, indexes: entries.count { |e| e[:kind] == 'index' }, constraints: entries.count { |e| e[:kind] == 'constraint' } },
  sourceCoverage: by_file, entries: entries,
  checks: { allSqlSourcesCovered: by_file.keys == files, hasOriginalBaseline: files.any? { |f| f.start_with?('database/original/') }, hasIncrementalMigrations: files.any? { |f| f.start_with?('database/migrations/') }, hasTables: entries.any? { |e| e[:kind] == 'table' }, hasIndexesOrConstraints: entries.any? { |e| %w[index constraint].include?(e[:kind]) } }
}
json_path = File.join(root, 'docs/generated/drift04-schema-dictionary.json')
md_path = File.join(root, 'docs/generated/drift04-schema-dictionary.md')
FileUtils.mkdir_p(File.dirname(json_path))
File.write(json_path, JSON.pretty_generate(report) + "\n")
File.write(md_path, <<~MD)
  # Aoo 数据库对象字典

  > 自动生成，禁止手工编辑。覆盖 `database/original` 原始基线与 `database/migrations` 增量迁移。

  | SQL 文件 | DDL/约束条目数 |
  |---|---:|
  #{by_file.map { |file, count| "| `#{file}` | #{count} |" }.join("\n")}

  汇总：表 #{report[:summary][:tables]}、变更 #{report[:summary][:alters]}、索引 #{report[:summary][:indexes]}、约束 #{report[:summary][:constraints]}。逐对象、来源行和 DDL 摘要见同名 JSON。
MD
abort "DRIFT04 failed: #{report[:checks]}" unless report[:checks].values.all?
puts "DRIFT04 PASS: #{files.length} SQL files and #{entries.length} schema entries reconciled"
