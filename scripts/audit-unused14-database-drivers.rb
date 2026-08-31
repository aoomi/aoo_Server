require 'json'
require 'fileutils'
require 'rexml/document'

root = File.expand_path('..', __dir__)
jdbc_drivers = {
  'mysql-connector-j' => 'mysql', 'mysql-connector-java' => 'mysql', 'postgresql' => 'postgresql',
  'mariadb-java-client' => 'mariadb', 'ojdbc11' => 'oracle', 'sqlite-jdbc' => 'sqlite', 'h2' => 'h2'
}
declarations = []
Dir.glob(File.join(root, '**/pom.xml')).reject { |path| path.include?('/target/') }.each do |path|
  document = REXML::Document.new(File.read(path))
  REXML::XPath.each(document, '/*[local-name()="project"]/*[local-name()="dependencies"]/*[local-name()="dependency"]') do |dependency|
    artifact = REXML::XPath.first(dependency, '*[local-name()="artifactId"]')&.text
    next unless jdbc_drivers.key?(artifact)
    declarations << { pom: path.delete_prefix(root + '/'), artifact: artifact, scheme: jdbc_drivers.fetch(artifact),
                      scope: REXML::XPath.first(dependency, '*[local-name()="scope"]')&.text || 'compile' }
  end
end
config_files = Dir.glob(File.join(root, 'server/**/*.{yml,yaml,properties,xml}')).reject { |path| path.include?('/target/') || path.include?('/build/') }
configured_schemes = config_files.flat_map do |path|
  File.binread(path).force_encoding('UTF-8').scrub.scan(/jdbc:([a-z0-9]+)/i).flatten.map(&:downcase)
end.uniq.sort
production_drivers = declarations.reject { |item| item[:scope] == 'test' }
declared_schemes = production_drivers.map { |item| item[:scheme] }.uniq.sort
old_drivers = declarations.select { |item| item[:artifact] == 'mysql-connector-java' }
bad_scopes = production_drivers.reject { |item| item[:scope] == 'runtime' }
mongo_configured = config_files.any? { |path| File.binread(path).match?(/mongo(?:db)?[.:_-]/i) }
mongo_declared = Dir.glob(File.join(root, '**/pom.xml')).any? { |path| !path.include?('/target/') && File.read(path).include?('mongodb-driver-sync') }
checks = { jdbc_driver_matches_configured_scheme: (declared_schemes - configured_schemes).empty? && (configured_schemes - declared_schemes).empty?,
           no_legacy_or_duplicate_jdbc_driver: old_drivers.empty?, jdbc_drivers_runtime_only: bad_scopes.empty?,
           mongodb_driver_has_configuration: !mongo_declared || mongo_configured }
result = { task: 'UNUSED14', passed: checks.values.all?, checks: checks, configuredSchemes: configured_schemes,
           declarations: declarations, oldDrivers: old_drivers, badScopes: bad_scopes,
           mongodb: { declared: mongo_declared, configured: mongo_configured } }
FileUtils.mkdir_p(File.join(root, 'work/audit'))
File.write(File.join(root, 'work/audit/unused14-database-drivers.json'), JSON.pretty_generate(result))
puts JSON.generate(result)
abort('UNUSED14 failed') unless result[:passed]
