#!/usr/bin/env ruby
require 'fileutils'
require 'json'

root = File.expand_path('../../..', __dir__)
original = File.join(root, 'database/original')
scan_inputs=JSON.parse(File.read(File.join(root, 'engineering/scan-input-roots.json'))); tool_input_roots=scan_inputs.fetch('toolInputs').map { |relative| File.join(root, relative) }; script_input_roots=scan_inputs.fetch('scriptInputs').map { |relative| File.join(root, relative) }; runtime_files = (Dir.glob(File.join(root, '{pom.xml,modules/**/*,deploy/**/*}')) + tool_input_roots.flat_map { |input_root| Dir.glob(File.join(input_root, '*')) }).uniq.select do |path|
  input_root = tool_input_roots.find { |base| path.start_with?(base + '/') } || root
  relative = path.delete_prefix(input_root + '/')
  File.file?(path) && !('/' + relative).include?('/target/') && !('/' + relative).include?('/build/') && %w[.java .xml .yml .yaml .properties .sh .py .rb].include?(File.extname(path))
end
allowed_audits = %w[
  engineering/audit/tool-inputs/audit_historical_data_equivalence.rb
  engineering/audit/tool-inputs/audit_database_runtime_mapping.rb
  engineering/audit/tool-inputs/audit_region_reference_integrity.rb
  engineering/audit/tool-inputs/audit_mongodb_baseline.rb
]
references = runtime_files.each_with_object([]) do |path, findings|
  relative = path.delete_prefix(root + '/')
  next if allowed_audits.include?(relative) || relative == 'engineering/audit/script-inputs/audit-dbreal01-original-isolation.rb'
  content = File.read(path, mode: 'rb').force_encoding(Encoding::UTF_8).scrub
  findings << relative if content.match?(%r{database/original|(?:db_zle_qh|clark_game_qh|clark_log_qh)\.sql})
end
fresh_tool = File.read(File.join(root, 'engineering/tests/tool-inputs/verify_fresh_migrations.sh'))
release_rules = File.read(File.join(root, '.releaseignore')).lines.map(&:strip)
checks = {
  original_directory_documented: File.file?(File.join(original, 'README.md')),
  dumps_present: Dir.glob(File.join(original, '*.sql')).length == 4,
  no_runtime_or_build_reference: references.empty?,
  fresh_initializer_uses_only_migrations: fresh_tool.include?('database/migrations') && !fresh_tool.include?('database/original'),
  release_excluded: release_rules.include?('database/original/')
}
result = { task: 'DBREAL01', passed: checks.values.all?, checks: checks, originalFiles: Dir.glob(File.join(original, '*.sql')).map { |path| path.delete_prefix(root + '/') }.sort, forbiddenReferences: references.sort }
out = File.join(root, 'build-only/audit/dbreal01-original-isolation.json')
FileUtils.mkdir_p(File.dirname(out))
File.write(out, JSON.pretty_generate(result) + "\n")
puts "DBREAL01 #{result[:passed] ? 'passed' : 'failed'}: #{result[:originalFiles].length} original dumps are release/runtime unreachable"
exit(result[:passed] ? 0 : 1)
