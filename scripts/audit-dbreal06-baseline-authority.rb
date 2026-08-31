#!/usr/bin/env ruby
# frozen_string_literal: true
require 'digest'
require 'json'
require 'pathname'

root = Pathname.new(__dir__).parent
original = root / 'database/original/db_zle.sql'
quarantined = root / 'work/quarantine/dbreal06/db_zle-brand-replacement-corrupted.sql'
fresh = root / 'tools/verify_fresh_migrations.sh'
active_duplicate = root / 'database/db_zle.sql'

checks = {
  'no_active_duplicate' => !active_duplicate.exist?,
  'legacy_original_read_only' => original.file? && (original.stat.mode & 0o222).zero?,
  'unsafe_replacement_copy_quarantined' => quarantined.file?,
  'copies_are_not_identical' => original.file? && quarantined.file? && Digest::SHA256.file(original).hexdigest != Digest::SHA256.file(quarantined).hexdigest,
  'fresh_initialization_uses_migrations_only' => fresh.read.include?('database/migrations') && !fresh.read.include?('database/original') && !fresh.read.include?('database/db_zle.sql')
}
report = {
  'task' => 'DBREAL06',
  'passed' => checks.values.all?,
  'authority' => 'database/migrations/*.sql',
  'legacyReference' => 'database/original/db_zle.sql',
  'quarantinedCopy' => 'work/quarantine/dbreal06/db_zle-brand-replacement-corrupted.sql',
  'finding' => 'the former active copy had broad substring branding replacements inside historical user tokens and URLs; it is not a valid migration source',
  'sourceHashes' => {
    'legacyOriginalSha256' => Digest::SHA256.file(original).hexdigest,
    'quarantinedCopySha256' => Digest::SHA256.file(quarantined).hexdigest
  },
  'checks' => checks
}
(root / 'work/audit').mkpath
(root / 'work/audit/dbreal06-baseline-authority.json').write(JSON.pretty_generate(report) + "\n")
abort JSON.generate(report) unless report['passed']
puts 'DBREAL06 passed: migrations are the sole initialization authority; unsafe duplicate quarantined'
