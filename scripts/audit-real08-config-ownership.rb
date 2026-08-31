require 'json'; require 'fileutils'
root=File.expand_path('..',__dir__)
modern=%w[ConfigCenter Bootstrap Hall Gateway GameCommon GameSPI Mahjong Poker LongCard WordCard Families]
modern_sources=modern.flat_map{|m|Dir[File.join(root,"server/#{m}/src/main/java/**/*.java")]}
legacy_sources=Dir[File.join(root,'server/gameServer/src/**/*.java')]
legacy_config=legacy_sources.select do |p|
  text=File.read(p)
  text.match?(/GameListConfigMgr|loadRemoteConfig|System\.getProperty\("(?:game_sid|GameServer\.|Redis\.)/)
end
config_pom=File.read(File.join(root,'server/ConfigCenter/pom.xml'))
checks={config_center_focused:!config_pom.match?(/game-server-framework|aoo-kernel/),modern_modules_no_legacy_config:modern_sources.none?{|p|File.read(p).match?(/GameListConfigMgr|loadRemoteConfig/)},legacy_fallback_unreachable:legacy_config.empty?||!File.read(File.join(root,'pom.xml')).include?('<module>server/gameServer</module>')}
result={task:'REAL08',passed:checks.values.all?,checks:checks,legacyReachableConfigFiles:legacy_config.map{|p|p.delete_prefix(root+'/')},legacyReachableConfigFileCount:legacy_config.length};out=File.join(root,'work/audit/real08-config-ownership.json');FileUtils.mkdir_p(File.dirname(out));File.write(out,JSON.pretty_generate(result)+"\n");puts "REAL08 #{result[:passed]?'passed':'blocked'}: #{legacy_config.length} reachable legacy configuration files";exit(result[:passed]?0:2)
