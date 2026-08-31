require 'json'
require 'fileutils'
root = File.expand_path('..', __dir__)
pom = File.read(File.join(root, 'pom.xml'))
hall_pom = File.read(File.join(root, 'server/Hall/pom.xml'))
bootstrap = File.read(File.join(root, 'server/Bootstrap/src/main/java/com/aoo/bcg/bootstrap/BootstrapAPP.java'))
hall_sources = Dir[File.join(root, 'server/Hall/src/main/java/**/*.java')].map { |p| File.read(p) }.join("\n")
checks = {
  modern_hall_in_reactor: pom.include?('<module>server/Hall</module>'),
  legacy_hall_outside_reactor: !pom.include?('<module>server/LegacyGameHall</module>') && !pom.include?('<module>server/LegacyGameHall</module>'),
  no_legacy_framework_dependency: !hall_pom.include?('game-server-framework') && !hall_pom.include?('aoo-kernel'),
  four_capability_boundaries: %w[account-session game-catalog room-management club-management].all? { |x| hall_sources.include?(x) },
  gameplay_authority_excluded: !hall_sources.match?(/playCard|settleRound|dealCards/),
  bootstrap_uses_modern_hall: bootstrap.include?('com.aoo.bcg.hall.HallApplication') && !bootstrap.include?('core.server.gamehall')
}
result = { task: 'REAL03', passed: checks.values.all?, checks: checks }
out = File.join(root, 'work/audit/real03-hall-layering.json'); FileUtils.mkdir_p(File.dirname(out)); File.write(out, JSON.pretty_generate(result) + "\n")
puts "REAL03 #{result[:passed] ? 'passed' : 'failed'}: modern hall owns four application capabilities; legacy hall is quarantined"
exit(result[:passed] ? 0 : 1)
