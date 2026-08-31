#!/usr/bin/env ruby
require 'fileutils'
require 'json'

root = File.expand_path('..', __dir__)
root_pom = File.read(File.join(root, 'pom.xml'))
poms = ([File.join(root, 'pom.xml')] + Dir.glob(File.join(root, 'server/**/pom.xml'))).to_h { |path| [path, File.read(path)] }
entries = [
  {
    legacyCoordinate: 'aoo.legacy:kernel:1.0.0',
    exposure: 'archive-only',
    activeReplacement: 'server/AooKernel',
    updateAuthority: 'Aoo source ownership plus centrally managed SLF4J/Logback',
    response: 'patch active source/dependencies; never reactivate archive'
  },
  {
    legacyCoordinate: 'aoo.legacy:nettosphere:3.2.2-qh',
    exposure: 'archive-only',
    activeReplacement: 'server/Gateway with centrally managed Netty BOM',
    updateAuthority: 'Netty upstream and Aoo Gateway ownership',
    response: 'upgrade netty.version and run transport/protocol regressions; never reactivate archive'
  }
]
active_pom_text = poms.values.join("\n")
checks = {
  archived_repository_exists: Dir.exist?(File.join(root, 'reference/legacy-2.22/third-party-maven-repository/com')),
  archive_release_excluded: File.read(File.join(root, '.releaseignore')).lines.map(&:strip).include?('reference/'),
  no_legacy_coordinate_active: active_pom_text !~ /com\.aoo\.legacy|nettosphere/,
  current_kernel_source_exists: Dir.exist?(File.join(root, 'server/AooKernel/src/main/java')),
  current_gateway_source_exists: Dir.exist?(File.join(root, 'server/Gateway/src/main/java')),
  netty_centrally_managed: root_pom.match?(/<netty\.version>[^<]+<\/netty\.version>/) && root_pom.include?('<artifactId>netty-bom</artifactId>'),
  no_leaf_netty_versions: poms.reject { |path, pom| path == File.join(root, 'pom.xml') || pom.include?('<artifactId>netty-bom</artifactId>') }.values.none? { |pom| pom.match?(/<artifactId>netty-[^<]+<\/artifactId>\s*<version>/m) },
  every_archive_has_response_path: entries.all? { |entry| !entry[:activeReplacement].empty? && !entry[:response].empty? }
}
ledger = { schemaVersion: 1, task: 'THIRD04', policy: 'docs/第三方安全更新与隔离策略.md', entries: entries }
ledger_path = File.join(root, 'docs/generated/third-party-security-path.json')
FileUtils.mkdir_p(File.dirname(ledger_path))
File.write(ledger_path, JSON.pretty_generate(ledger) + "\n")
result = { task: 'THIRD04', passed: checks.values.all?, checks: checks, ledger: 'docs/generated/third-party-security-path.json' }
out = File.join(root, 'work/audit/third04-security-path.json')
FileUtils.mkdir_p(File.dirname(out))
File.write(out, JSON.pretty_generate(result) + "\n")
puts "THIRD04 #{result[:passed] ? 'passed' : 'failed'}: #{entries.length} legacy capabilities have controlled security paths"
exit(result[:passed] ? 0 : 1)
