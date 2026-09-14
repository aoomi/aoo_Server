require 'json'
require 'fileutils'

root = File.expand_path('..', __dir__)
legacy = File.join(root, 'server/LegacyCommDef')
modern = %w[GameSPI GameCommon Mahjong Poker LongCard WordCard Families Gateway ConfigCenter AdminApi Billing Bootstrap]
modern_sources = modern.flat_map { |name| Dir[File.join(root, "server/#{name}/src/**/*.{java,kt}")] }
forbidden_imports = modern_sources.map do |path|
  text = File.binread(path).force_encoding('UTF-8').scrub
  next unless text.match?(/^\s*import\s+(?:jsproto|cenum)\./)
  path.delete_prefix(root + '/')
end.compact
game_spi_pom = File.read(File.join(root, 'server/GameSPI/pom.xml'))
legacy_launchers = Dir[File.join(legacy, 'src/**/*.java')].map do |path|
  text = File.binread(path).force_encoding('UTF-8').scrub
  class_name = File.basename(path, '.java')
  path.delete_prefix(root + '/') if class_name.match?(/(?:App|Application|Server|Bootstrap)$/) && text.match?(/public\s+static\s+void\s+main\s*\(/)
end.compact
checks = {
  legacy_boundary_exists: Dir.exist?(legacy),
  obsolete_public_directory_absent: !Dir.children(File.join(root, 'server')).include?('commdef'),
  source_only_boundary: !File.exist?(File.join(legacy, 'pom.xml')) && Dir[File.join(legacy, '**/*.jar')].empty?,
  no_legacy_launcher: legacy_launchers.empty?,
  game_spi_has_no_legacy_dependency: !game_spi_pom.match?(/commdef|jsproto|cenum/i),
  modern_modules_have_no_legacy_imports: forbidden_imports.empty?,
  private_adapter_is_explicit: File.read(File.join(root, 'server/gameServer/pom.xml')).include?('../LegacyCommDef/src')
}
result = { task: 'REAL02', passed: checks.values.all?, checks: checks,
  modernModulesScanned: modern.length, modernSourcesScanned: modern_sources.length,
  forbiddenImports: forbidden_imports, legacyLaunchers: legacy_launchers }
out = File.join(root, 'work/audit/real02-protocol-boundary.json')
FileUtils.mkdir_p(File.dirname(out))
File.write(out, JSON.pretty_generate(result) + "\n")
puts "REAL02 #{result[:passed] ? 'passed' : 'failed'}: GameSPI is the public contract; LegacyCommDef is private wire compatibility"
exit(result[:passed] ? 0 : 1)
