#!/usr/bin/env ruby
require 'json'
require 'fileutils'
root = File.expand_path('..', __dir__)
redactor = File.read(File.join(root, 'server/GameCommon/src/main/java/com/aoo/bcg/common/config/SensitiveDataRedactor.java'))
findings = []
globs = [File.join(root, 'server/**/*.{java,kt}'), File.join(root, '../Client/assets/Common/Code/**/*.{ts,js}')]
Dir.glob(globs).sort.each do |file|
  next if file.include?('/target/') || File.basename(file).start_with?('GeneratedProtocolIds.')
  File.foreach(file, encoding: 'UTF-8', invalid: :replace, undef: :replace).with_index(1) do |line, number|
    next if line.strip.start_with?('//')
    logger = line.match?(/console\.(?:log|error|warn)|System\.(?:out|err)\.print/i)
    card_value = line.match?(/selectCardList|handCardList|privateCards|\bcardList\b|\bcards\b/i) && !line.match?(/\.size\(\)|\.length|cardCount|handCount|playedCount/i)
    identity_value = line.match?(/getBirthDayStr\(|\bidc\b|idCard(?:No|Number)?/i)
    credential_value = line.match?(/(?:password|passwd|secret|token)\s*[,)+]/i)
    findings << {path: file.delete_prefix(root + '/'), line: number} if logger && (card_value || identity_value || credential_value)
  end
end
checks = {
  credential_redaction: %w[BEARER ASSIGNMENT JDBC_CREDENTIALS PEM].all? { |name| redactor.include?(name) },
  identity_redaction: redactor.include?('IDENTITY_NUMBER'),
  structured_private_card_redaction: %w[privatecards handcards cardlist cardwall].all? { |key| redactor.include?(key) },
  direct_sensitive_console_findings: findings.length
}
passed = checks.values_at(:credential_redaction, :identity_redaction, :structured_private_card_redaction).all? && findings.empty?
report = {task: 'LOGREAL10', status: passed ? 'passed' : 'failed', checks: checks, findings: findings}
out = File.join(root, 'docs/generated/logreal10-sensitive-logging.json')
FileUtils.mkdir_p(File.dirname(out)); File.write(out, JSON.pretty_generate(report) + "\n")
abort "LOGREAL10 failed: #{findings}" unless passed
puts 'LOGREAL10 PASS: sensitive logging gate closed'
