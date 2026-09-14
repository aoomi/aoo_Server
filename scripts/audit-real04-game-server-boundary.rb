require 'json'
require 'fileutils'
root = File.expand_path('..', __dir__)
framework = File.join(root, 'server/gameServer')
sources = Dir[File.join(framework, 'src/**/*.java')]
business = sources.select { |p| p.include?('/business/') }
game_poms = Dir[File.join(root, 'server/{CDXZMJ,NJPDK,SCJYMJ,XCPDK,ZJH,ZYPK}/pom.xml')]
dependents = game_poms.select { |p| File.read(p).include?('<artifactId>game-server-framework</artifactId>') }
root_pom = File.read(File.join(root, 'pom.xml'))
checks = {
  giant_framework_outside_default_runtime: !root_pom.include?('<module>server/gameServer</module>'),
  concrete_games_use_category_spi_only: dependents.empty?,
  no_business_implementations_in_framework: business.empty?
}
result = { task: 'REAL04', passed: checks.values.all?, checks: checks,
  frameworkJavaSources: sources.length, frameworkBusinessSources: business.length,
  legacyDependentModules: dependents.map { |p| File.basename(File.dirname(p)) } }
out = File.join(root, 'work/audit/real04-game-server-boundary.json'); FileUtils.mkdir_p(File.dirname(out)); File.write(out, JSON.pretty_generate(result) + "\n")
puts "REAL04 #{result[:passed] ? 'passed' : 'blocked'}: #{business.length} business sources and #{dependents.length} concrete modules remain coupled"
exit(result[:passed] ? 0 : 2)
