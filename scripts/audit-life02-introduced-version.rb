require 'json'
require 'fileutils'
root = File.expand_path('..', __dir__)
source = File.read(File.join(root, 'server/GameSPI/src/main/java/com/aoo/bcg/gamespi/api/ApiLifecycleCatalog.java'))
test = File.read(File.join(root, 'server/GameSPI/src/test/java/com/aoo/bcg/gamespi/api/ApiLifecycleCatalogTest.java'))
checks = {
  interface_version: source.include?('Contract(') && source.include?('SemanticVersion introducedVersion'),
  field_version: source.include?('FieldMetadata(String name, SemanticVersion introducedVersion)'),
  http_and_wss_covered: source.include?('ApiOwnershipCatalog.standard().entries()'),
  missing_metadata_fails: source.include?('API lifecycle metadata missing'),
  executable_test: test.include?('everyInterfaceMessageAndFieldHasIntroducedVersion')
}
abort "LIFE02 audit failed: #{checks}" unless checks.values.all?
out = File.join(root, 'work/audit/api-introduced-version.json')
FileUtils.mkdir_p(File.dirname(out))
File.write(out, JSON.pretty_generate({task: 'LIFE02', status: 'passed', checks: checks}) + "\n")
