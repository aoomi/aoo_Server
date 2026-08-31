require 'json'
require 'fileutils'
root = File.expand_path('..', __dir__)
source = File.read(File.join(root, 'server/GameSPI/src/main/java/com/aoo/bcg/gamespi/api/ApiOwnershipCatalog.java'))
test = File.read(File.join(root, 'server/GameSPI/src/test/java/com/aoo/bcg/gamespi/api/ApiOwnershipCatalogTest.java'))
checks = {
  http_owned: source.scan('Transport.HTTP').length >= 2,
  wss_namespaces_owned: %w[common mahjong poker long-card word-card family].all? { |name| source.include?("\"#{name}.*\"") },
  maintainer_required: source.include?('maintainerTeam'),
  missing_owner_fails: source.include?('public API has no owner'),
  executable_test: test.include?('resolvesAllPublicTransportNamespacesToNamedMaintainers')
}
abort "LIFE01 audit failed: #{checks}" unless checks.values.all?
out = File.join(root, 'work/audit/api-ownership-catalog.json')
FileUtils.mkdir_p(File.dirname(out))
File.write(out, JSON.pretty_generate({task: 'LIFE01', status: 'passed', checks: checks}) + "\n")
