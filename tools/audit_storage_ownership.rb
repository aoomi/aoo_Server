#!/usr/bin/env ruby
# frozen_string_literal: true

require 'json'
require 'time'

root = File.expand_path('..', __dir__)
baseline = JSON.parse(File.read(File.join(root, 'work/audit/audit-source-baseline.json'), encoding: 'UTF-8'))
output_path = File.join(root, 'work/audit/storage-ownership-inventory.json')
task_path = File.join(root, 'docs/Aoo-前后端全框架功能通信审计任务清单.md')

patterns = {
  'jdbc' => /(?:java\.sql\.|JdbcTemplate|DataSource|MyBatis|SqlSession|EntityManager|Jooq)/i,
  'nativeSql' => /(?:\bSELECT\b.+\bFROM\b|\bINSERT\s+INTO\b|\bUPDATE\b.+\bSET\b|\bDELETE\s+FROM\b|createNativeQuery)/i,
  'redis' => /(?:Redis|Jedis|Redisson|Lettuce|RBucket|RMap|opsForValue|opsForHash)/i,
  'lua' => /(?:\.lua\b|EVALSHA|\bEVAL\b|RedisScript|DefaultRedisScript)/i,
  'objectStorage' => /(?:S3Client|AmazonS3|Minio|OSSClient|CosClient|ObjectStorage|BlobClient)/i,
  'storedProcedure' => /(?:CallableStatement|\bCALL\s+[a-z_]|createStoredProcedureQuery)/i
}.freeze

def ownership(path)
  case path
  when %r{/(?:account|login|auth)/}i then 'account'
  when %r{/(?:club|union|family|guild)/}i then 'club'
  when %r{/(?:room|game|play|match)/}i then 'game-room'
  when %r{/(?:pay|wallet|currency|billing|accounting)/}i then 'accounting'
  when %r{/(?:record|replay|history)/}i then 'record-replay'
  when %r{/config}i then 'configuration'
  when %r{/protocol|/network|/gateway}i then 'transport'
  else 'unclassified'
  end
end

entries = []
baseline.fetch('files').each do |item|
  path = item.fetch('path')
  next unless path.start_with?(File.join(root, 'modules') + '/', File.join(root, 'server') + '/')
  next unless File.file?(path) && item.fetch('size') <= 3_000_000
  next unless %w[.java .kt .xml .yml .yaml .properties .sql .lua].include?(File.extname(path).downcase)
  text = File.binread(path).force_encoding('UTF-8').scrub
  kinds = patterns.each_with_object([]) { |(kind, regex), found| found << kind if text.match?(regex) }
  next if kinds.empty?
  matches = []
  text.lines.each_with_index do |line, index|
    line_kinds = patterns.each_with_object([]) { |(kind, regex), found| found << kind if line.match?(regex) }
    next if line_kinds.empty?
    matches << {line: index + 1, kinds: line_kinds, excerpt: line.strip[0, 300]}
    break if matches.size >= 50
  end
  relative = path.delete_prefix(root + '/')
  entries << {path: relative, owner: ownership(relative), kinds: kinds, matches: matches}
end

summary = patterns.keys.to_h { |kind| [kind, entries.count { |entry| entry[:kinds].include?(kind) }] }
owners = entries.group_by { |entry| entry[:owner] }.transform_values(&:size).sort.to_h
unclassified = entries.select { |entry| entry[:owner] == 'unclassified' }
report = {
  schemaVersion: 1,
  generatedAt: Time.now.utc.iso8601,
  summary: summary,
  ownershipSummary: owners,
  unclassifiedCount: unclassified.size,
  entries: entries.sort_by { |entry| entry[:path] },
  limitations: ['Static ownership is path-derived and must be reconciled with runtime datasource/Redis/object-store telemetry.']
}
File.write(output_path, JSON.pretty_generate(report) + "\n", mode: 'w:UTF-8')

task = File.read(task_path, encoding: 'UTF-8')
detail = summary.map { |kind, count| "#{kind}=#{count}" }.join('、')
row = "| CALL09 | 未完成 | 已审计·待归属收敛 | 已归档 JDBC/原生 SQL/Redis/Lua/存储过程/对象存储调用并按业务路径归属（#{detail}，未分类=#{unclassified.size}）；未分类项及运行数据源归属尚待收敛。 证据：work/audit/storage-ownership-inventory.json |"
task.sub!(/^\| CALL09 \|.*$/, row) or abort 'CALL09 row not found'
task.sub!(/^下一项：.*$/, '下一项：CALL10 静态调用图与运行覆盖差异') or abort 'next pointer not found'
File.write(task_path, task, mode: 'w:UTF-8')
puts JSON.generate(summary.merge('unclassified' => unclassified.size))
