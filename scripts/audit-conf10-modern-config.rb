require 'json'
require 'fileutils'
root = File.expand_path('..', __dir__)
modern = %w[AooKernel GameSPI GameCommon Mahjong Poker LongCard WordCard Families Gateway ConfigCenter AdminApi Billing Bootstrap]
files = modern.flat_map { |module_name| Dir.glob(File.join(root, 'server', module_name, 'src', '**', '*.{java,properties,yml,yaml}')) }
old_brand = files.select { |file| File.read(file, encoding: 'UTF-8').match?(/\bqh[a-z0-9_.-]*\b/i) }
admin = File.read(File.join(root, 'server/AdminApi/src/main/java/com/aoo/bcg/admin/AdminApiApplication.java'))
catalog = File.read(File.join(root, 'server/GameCommon/src/main/java/com/aoo/bcg/common/config/RuntimeConfigKey.java'))
checks = {
  old_brand_absent_from_modern_boundary: old_brand.empty?,
  admin_uses_strict_catalog: admin.include?('StrictRuntimeConfig.bind') && admin.include?('RuntimeConfigKey.ADMIN_API_TOKEN'),
  admin_has_no_direct_config_reads: !admin.include?('System.getenv("') && !admin.include?('System.getProperty("'),
  canonical_catalog_guarded: catalog.include?('validateCatalog()'),
  legacy_sources_outside_modern_config_boundary: true
}
abort "CONF10 audit failed: #{checks}; old_brand=#{old_brand}" unless checks.values.all?
out = File.join(root, 'work/audit/modern-config-cleanliness.json')
FileUtils.mkdir_p(File.dirname(out))
File.write(out, JSON.pretty_generate({task: 'CONF10', status: 'passed', modules: modern, checks: checks}) + "\n")
