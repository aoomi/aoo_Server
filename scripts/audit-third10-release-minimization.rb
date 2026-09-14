#!/usr/bin/env ruby
require 'digest'
require 'fileutils'
require 'json'

root = File.expand_path('..', __dir__)
package_pom_path = File.join(root, 'server/LegacyAccountServer/package_hall/pom.xml')
package_pom = File.read(package_pom_path)
lib_dir = File.join(root, 'server/LegacyAccountServer/package_hall/target/lib')
jars = Dir.glob(File.join(lib_dir, '*.jar')).sort
foreign_native = jars.select { |path| File.basename(path).match?(/-(?:linux|osx|windows)-(?:x86_64|aarch_64|riscv64)\.jar$/) }
forbidden = jars.select { |path| path.include?('/reference/') || File.basename(path).match?(/(?:junit|surefire|mockito|assertj)/i) }
inventory = jars.map do |path|
  { name: File.basename(path), bytes: File.size(path), sha256: Digest::SHA256.file(path).hexdigest }
end
required_classifiers = %w[linux-aarch_64 linux-riscv64 linux-x86_64 osx-aarch_64 osx-x86_64 windows-x86_64]
checks = {
  runtime_scope_only: package_pom.include?('<includeScope>runtime</includeScope>'),
  transitive_runtime_dependencies_retained: package_pom.include?('<excludeTransitive>false</excludeTransitive>'),
  all_foreign_native_classifiers_excluded: required_classifiers.all? { |classifier| package_pom.include?(classifier) },
  generated_package_has_no_foreign_native: foreign_native.empty?,
  generated_package_has_no_test_or_reference_jar: forbidden.empty?,
  generated_inventory_hashed: inventory.all? { |entry| entry[:sha256].length == 64 },
  historical_roots_release_excluded: File.read(File.join(root, '.releaseignore')).lines.map(&:strip).include?('reference/'),
  stale_game_server_build_absent: !Dir.exist?(File.join(root, 'server/gameServer/build'))
}
result = {
  task: 'THIRD10', passed: checks.values.all?, checks: checks,
  packageEvidenceAvailable: Dir.exist?(lib_dir), jarCount: jars.length,
  totalBytes: inventory.sum { |entry| entry[:bytes] },
  foreignNativeJars: foreign_native.map { |path| File.basename(path) },
  forbiddenJars: forbidden.map { |path| File.basename(path) },
  inventory: inventory,
  policy: 'docs/发布类路径最小化规范.md'
}
out = File.join(root, 'work/audit/third10-release-minimization.json')
FileUtils.mkdir_p(File.dirname(out))
File.write(out, JSON.pretty_generate(result) + "\n")
puts "THIRD10 #{result[:passed] ? 'passed' : 'failed'}: #{jars.length} runtime jars, #{result[:totalBytes]} bytes"
exit(result[:passed] ? 0 : 1)
