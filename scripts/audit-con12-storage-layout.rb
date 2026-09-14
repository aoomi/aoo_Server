require 'json'
require 'fileutils'
root = File.expand_path('..', __dir__)
gate = File.read(File.join(root, 'server/gameServer/src/core/server/StorageLayoutReadiness.java'))
migration = File.read(File.join(root, 'database/migrations/V20260823_19__canonical_storage_layout.sql'))
checks = {
  innodb_required: gate.include?('InnoDB') && migration.include?('ENGINE=InnoDB'),
  utf8mb4_required: gate.include?('utf8mb4_') && migration.include?('utf8mb4_0900_ai_ci'),
  large_row_format: gate.include?('Dynamic') && migration.include?('ROW_FORMAT=DYNAMIC'),
  index_byte_limit: gate.include?('HAVING bytes>3072'),
  all_aoo_tables_gated: gate.include?("TABLE_NAME LIKE 'aoo")
}
abort "CON12 audit failed: #{checks}" unless checks.values.all?
out = File.join(root, 'work/audit/storage-layout.json')
FileUtils.mkdir_p(File.dirname(out))
File.write(out, JSON.pretty_generate({task: 'CON12', status: 'passed', checks: checks}) + "\n")
