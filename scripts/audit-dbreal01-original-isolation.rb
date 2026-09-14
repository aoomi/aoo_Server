#!/usr/bin/env ruby
require 'fileutils'
require 'json'

root = File.expand_path('..', __dir__)
original = File.join(root, 'database/original')
runtime_files = Dir.glob(File.join(root, '{pom.xml,server/**/*,deploy/**/*,tools/*}')).select do |path|
  File.file?(path) && !path.include?('/target/') && !path.include?('/build/') &&
    %w[.java .xml .yml .yaml .properties .sh .py .rb].include?(File.extname(path))
end
allowed_audits = %w[
  tools/audit_historical_data_equivalence.rb
  tools/audit_database_runtime_mapping.rb
  tools/audit_region_reference_integrity.rb
  tools/audit_mongodb_baseline.rb
]
references = runtime_files.each_with_object([]) do |path, findings|
  relative = path.delete_prefix(root + '/')
  next if allowed_audits.include?(relative) || relative == 'scripts/audit-dbreal01-original-isolation.rb'
  content = File.read(path, mode: 'rb').force_encoding(Encoding::UTF_8).scrub
  findings << relative if content.match?(%r{database/original|(?:db_zle_qh|clark_game_qh|clark_log_qh)\.sql})
end
fresh_tool = File.read(File.join(root, 'tools/verify_fresh_migrations.sh'))
release_rules = File.read(File.join(root, '.releaseignore')).lines.map(&:strip)
checks = {
  original_directory_documented: File.file?(File.join(original, 'README.md')),
  dumps_present: Dir.glob(File.join(original, '*.sql')).length == 4,
  no_runtime_or_build_reference: references.empty?,
  fresh_initializer_uses_only_migrations: fresh_tool.include?('database/migrations') && !fresh_tool.include?('database/original'),
  release_excluded: release_rules.include?('database/original/')
}
result = { task: 'DBREAL01', passed: checks.values.all?, checks: checks, originalFiles: Dir.glob(File.join(original, '*.sql')).map { |path| path.delete_prefix(root + '/') }.sort, forbiddenReferences: references.sort }
out = File.join(root, 'work/audit/dbreal01-original-isolation.json')
FileUtils.mkdir_p(File.dirname(out))
File.write(out, JSON.pretty_generate(result) + "\n")
puts "DBREAL01 #{result[:passed] ? 'passed' : 'failed'}: #{result[:originalFiles].length} original dumps are release/runtime unreachable"
exit(result[:passed] ? 0 : 1)
