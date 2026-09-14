#!/usr/bin/env ruby
# frozen_string_literal: true

require 'json'
require 'time'
require 'fileutils'

root = File.expand_path('..', __dir__)
output = File.join(root, 'work/audit/database-single-writer-audit.json')
excluded = %w[/target/ /build/ /.git/ /work/audit/].freeze
patterns = {
  embeddedWriteSql: /(?i)["']\s*(?:insert\s+into|update\s+[a-z0-9_`]+\s+set|delete\s+from|replace\s+into)/,
  directJdbcConnection: /(?:DriverManager\.getConnection|DataSource\.getConnection|\.getConnection\(\))/,
  legacyDatabaseConfig: /(?i)(?:jdbc:|mongodb\.(?:host|uri)|redis\.(?:host|port)|datasource).*(?:legacy|old|backup)/,
  scheduledWriter: /(?:@Scheduled|Quartz|Job\b|TimerTask)/
}.freeze

findings = []
Dir.glob(File.join(root, 'server/**/*'), File::FNM_DOTMATCH).each do |path|
  next unless File.file?(path) && %w[.java .kt .properties .yml .yaml .xml].include?(File.extname(path))
  next if excluded.any? { |part| path.include?(part) }
  relative = path.delete_prefix(root + '/')
  File.foreach(path, encoding: 'UTF-8', invalid: :replace, undef: :replace).with_index(1) do |raw, number|
    line = raw.encode('UTF-8', invalid: :replace, undef: :replace, replace: '')
    patterns.each do |kind, pattern|
      next unless line.match?(pattern)
      findings << {kind: kind, path: relative, line: number, text: line.strip[0, 220]}
    end
  end
end

counts = patterns.keys.to_h { |kind| [kind, findings.count { |row| row[:kind] == kind }] }
summary = counts.merge(
  affectedFiles: findings.map { |row| row[:path] }.uniq.size,
  databasePrivilegeEvidencePresent: false,
  legacyWriterDisabledEvidencePresent: false,
  passed: false
)
report = {
  schemaVersion: 1,
  generatedAt: Time.now.utc.iso8601,
  invariant: 'One authoritative application owns every mutable dataset; legacy applications have no write privilege and no scheduled writer.',
  summary: summary,
  findings: findings,
  externalEvidenceRequired: ['database grants', 'active connection inventory', 'scheduler inventory', 'write-audit log proving one writer']
}
FileUtils.mkdir_p(File.dirname(output))
File.write(output, JSON.pretty_generate(report) + "\n", mode: 'w:UTF-8')
puts JSON.generate(summary)
exit(summary[:passed] ? 0 : 2) if ENV['AOO_ENFORCE_SINGLE_WRITER_GATE'] == '1'
