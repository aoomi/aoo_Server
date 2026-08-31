#!/usr/bin/env ruby
# frozen_string_literal: true

require 'find'
require 'json'
require 'pathname'

ROOT = Pathname.new(__dir__).parent
OUTPUT = ROOT / 'docs/generated/sensitive-data-repository-scan.json'
DATA_EXTENSIONS = %w[.sql .json .csv .log .txt .properties .yml .yaml .xml .conf].freeze
PATTERNS = {
  'phone' => /\b1[3-9]\d{9}\b/,
  'jwt' => /\beyJ[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}\b/,
  'wechatIdentifier' => /\bwx_[A-Za-z0-9_-]{20,}\b/,
  'gpsCoordinate' => /\b(?:lat(?:itude)?|lng|lon(?:gitude)?)\s*[:=]\s*-?\d{1,3}\.\d{4,}/i,
  'privateKey' => /BEGIN (?:RSA |EC )?PRIVATE KEY/
}.freeze
EXCLUDED = %r{\A(?:target|build|work|reference|database/original|\.git)/}

findings = []
Find.find(ROOT.to_s) do |absolute|
  next unless File.file?(absolute)
  relative = Pathname.new(absolute).relative_path_from(ROOT).to_s
  next if relative.match?(EXCLUDED) || relative.start_with?('docs/')
  next unless DATA_EXTENSIONS.include?(File.extname(relative).downcase)
  next if File.size(absolute) > 20_000_000
  content = File.binread(absolute).force_encoding('UTF-8').scrub
  PATTERNS.each do |category, pattern|
    count = content.scan(pattern).length
    findings << { 'path' => relative, 'category' => category, 'count' => count } if count.positive?
  end
  content.lines.each_with_index do |line, index|
    next if File.extname(relative).downcase == '.sql'
    next if line.match?(/\b(?:secretName|urlSecret|secretKeyRef)\s*:/)
    next if line.lstrip.start_with?('#')
    match = line.match(/^\s*([^:=]+?)\s*[:=]\s*(.*?)\s*$/)
    next unless match && match[1].match?(/password|passwd|secret|private.?key|server.?key|notifykey|charge.?key|partnerkey|paternerkey|^key$/i)
    next if match[1].strip.match?(/(?:ref|name|verifier|hash)\z/i)
    value = match[2].sub(/\s+#.*$/, '').strip
    next if value.empty? || value.match?(/\A\$\{[A-Z0-9_]+(?::[^}]*)?\}\z/)
    findings << { 'path' => relative, 'category' => 'plaintextCredential', 'line' => index + 1, 'count' => 1 }
  end
end

backup_files = Dir[(ROOT / 'database/backups/*').to_s].select { |path| File.file?(path) }
plaintext_backups = backup_files.reject { |path| path.end_with?('.enc') }
original_counts = {}
Dir[(ROOT / 'database/original/*.sql').to_s].sort.each do |path|
  original_counts[File.basename(path)] = {
    'bytes' => File.size(path),
    'classification' => 'historical sensitive data; immutable and runtime/release isolated',
    'coveredCategories' => %w[userIdentifier token location asset chat]
  }
end
checks = {
  'no_sensitive_records_in_runtime_repository_data' => findings.empty?,
  'all_backups_encrypted' => backup_files.any? && plaintext_backups.empty?,
  'encrypted_backup_permissions' => backup_files.all? { |path| (File.stat(path).mode & 0o077).zero? },
  'legacy_sensitive_data_is_read_only_and_release_isolated' => Dir[(ROOT / 'database/original/*.sql').to_s].all? { |path| (File.stat(path).mode & 0o222).zero? } && (ROOT / '.releaseignore').read.include?('database/original/')
}
report = {
  'schemaVersion' => 1, 'task' => 'DBREAL10', 'passed' => checks.values.all?,
  'scannedCategories' => PATTERNS.keys + ['plaintextCredential', 'asset records and chat are covered by original SQL isolation'],
  'runtimeFindings' => findings,
  'backupFiles' => backup_files.map { |path| Pathname.new(path).relative_path_from(ROOT).to_s },
  'legacyOriginalAggregateCounts' => original_counts,
  'privacyRule' => 'reports contain aggregate counts and paths only; never copy sensitive values into evidence',
  'checks' => checks
}
OUTPUT.dirname.mkpath
OUTPUT.write(JSON.pretty_generate(report) + "\n")
abort JSON.generate(report) unless report['passed']
puts 'DBREAL10 passed: repository data and encrypted backups contain no unapproved plaintext sensitive material'
