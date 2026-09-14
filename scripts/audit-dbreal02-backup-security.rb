#!/usr/bin/env ruby
require 'digest'
require 'fileutils'
require 'json'
require 'time'

root = File.expand_path('..', __dir__)
backup_dir = File.join(root, 'database/backups')
ledger_path = File.join(root, 'database/backup-retention.json')
ledger = JSON.parse(File.read(ledger_path))
entries = ledger.fetch('entries')
files = Dir.glob(File.join(backup_dir, '*')).select { |path| File.file?(path) }
plain = files.reject { |path| path.end_with?('.enc') }
key = File.expand_path('../.secrets/database-backup-aes.key', root)
mode = lambda { |path| File.stat(path).mode & 0o777 }
now = Time.now.utc
registered = entries.map { |entry| entry.fetch('file') }
checks = {
  backup_directory_owner_only: mode.call(backup_dir) == 0o700,
  no_plaintext_backup: plain.empty?,
  every_ciphertext_registered: files.all? { |path| registered.include?(path.delete_prefix(root + '/')) },
  every_registered_ciphertext_exists: registered.all? { |path| File.file?(File.join(root, path)) },
  every_ciphertext_owner_only: files.all? { |path| mode.call(path) == 0o600 },
  openssl_salted_format: files.all? { |path| File.binread(path, 8) == 'Salted__' },
  external_key_exists_owner_only: File.file?(key) && mode.call(key) == 0o600 && !key.start_with?(root + '/'),
  complete_retention_metadata: entries.all? { |entry| %w[owner classification createdAt expiresAt encryption keyId restoreApproval destruction].all? { |field| !entry[field].to_s.empty? } },
  retention_not_expired: entries.all? { |entry| Time.parse(entry.fetch('expiresAt')).utc > now },
  backups_git_ignored: File.read(File.join(root, '.gitignore')).include?('/database/backups/'),
  backups_release_excluded: File.read(File.join(root, '.releaseignore')).include?('database/backups/'),
  no_legacy_brand_filename: files.none? { |path| File.basename(path).match?(/qh/i) }
}
result = {
  task: 'DBREAL02', passed: checks.values.all?, checks: checks,
  ciphertexts: files.map { |path| { file: path.delete_prefix(root + '/'), bytes: File.size(path), sha256: Digest::SHA256.file(path).hexdigest } },
  plaintextFindings: plain.map { |path| path.delete_prefix(root + '/') },
  retentionLedger: 'database/backup-retention.json'
}
out = File.join(root, 'work/audit/dbreal02-backup-security.json')
FileUtils.mkdir_p(File.dirname(out))
File.write(out, JSON.pretty_generate(result) + "\n")
puts "DBREAL02 #{result[:passed] ? 'passed' : 'failed'}: #{files.length} encrypted backups, #{plain.length} plaintext findings"
exit(result[:passed] ? 0 : 1)
