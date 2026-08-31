#!/usr/bin/env ruby
# frozen_string_literal: true

require 'json'
require 'time'
require 'fileutils'

root = File.expand_path('..', __dir__)
output = File.join(root, 'work/audit/credential-isolation-audit.json')
extensions = %w[.properties .yml .yaml .xml .json .java .kt .sh].freeze
excluded = %w[/target/ /build/ /.git/ /work/audit/].freeze
assignment = /(?i)\b(password|passwd|secret|access[_-]?token|api[_-]?key|private[_-]?key|client[_-]?secret|jdbc(?:url)?|mq[_-]?(?:user|password))\b\s*[=:]\s*([^\s,;#<]+)/
placeholder = /^(?:\$\{|\{[a-z0-9_.-]+\}|<redacted>|changeme|example|none|null|empty)$/i

findings = []
Dir.glob(File.join(root, '**/*'), File::FNM_DOTMATCH).each do |path|
  next unless File.file?(path) && extensions.include?(File.extname(path))
  next if excluded.any? { |part| path.include?(part) }
  relative = path.delete_prefix(root + '/')
  File.foreach(path, encoding: 'UTF-8', invalid: :replace, undef: :replace).with_index(1) do |raw, number|
    line = raw.encode('UTF-8', invalid: :replace, undef: :replace, replace: '')
    line.scan(assignment).each do |name, value|
      normalized = value.to_s.gsub(/["']/, '')
      next if normalized.empty? || placeholder.match?(normalized)
      findings << {path: relative, line: number, credentialType: name.downcase, valueRedacted: true}
    end
  end
end

summary = {
  embeddedCredentialCandidates: findings.size,
  affectedFiles: findings.map { |row| row[:path] }.uniq.size,
  revocationEvidencePresent: false,
  passed: findings.empty? && false
}
report = {
  schemaVersion: 1,
  generatedAt: Time.now.utc.iso8601,
  invariant: 'No credential is embedded in the repository and every legacy credential has authoritative revocation evidence.',
  summary: summary,
  findings: findings,
  externalEvidenceRequired: %w[token-revocation api-key-rotation certificate-revocation database-user-disable mq-user-disable]
}
FileUtils.mkdir_p(File.dirname(output))
File.write(output, JSON.pretty_generate(report) + "\n", mode: 'w:UTF-8')
puts JSON.generate(summary)
exit(summary[:passed] ? 0 : 2) if ENV['AOO_ENFORCE_CREDENTIAL_GATE'] == '1'
