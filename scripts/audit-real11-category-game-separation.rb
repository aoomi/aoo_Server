require 'json';require 'fileutils'
root=File.expand_path('..',__dir__);categories=%w[Mahjong Poker LongCard WordCard];games=%w[CDXZMJ NJPDK SCJYMJ XCPDK ZJH ZYPK]
category_sources=categories.to_h{|m|[m,Dir[File.join(root,"server/#{m}/src/main/java/**/*.java")]]}
provider_sources=games.to_h{|m|[m,Dir[File.join(root,"server/#{m}/src/**/*GameProvider.java")]]}
category_poms=categories.to_h{|m|[m,File.read(File.join(root,"server/#{m}/pom.xml"))]}
game_poms=games.to_h{|m|[m,File.read(File.join(root,"server/#{m}/pom.xml"))]}
expected={'CDXZMJ'=>'game-category-mahjong','SCJYMJ'=>'game-category-mahjong','NJPDK'=>'game-category-poker','XCPDK'=>'game-category-poker','ZJH'=>'game-category-poker','ZYPK'=>'game-category-poker'}
checks={four_category_modules:category_sources.values.all?{|v|!v.empty?},categories_own_core_rule_types:category_sources.values.all?{|v|v.any?{|p|File.basename(p).match?(/CoreEngine|RuleSet|RuleFamily/)}},six_concrete_providers:provider_sources.values.all?{|v|v.length==1},concrete_games_depend_on_category:expected.all?{|m,a|game_poms[m].include?("<artifactId>#{a}</artifactId>")},categories_do_not_depend_on_concrete_games:category_poms.values.none?{|p|games.any?{|m|p.downcase.include?("<artifactId>#{m.downcase}</artifactId>")}}}
result={task:'REAL11',passed:checks.values.all?,checks:checks,categorySourceCounts:category_sources.transform_values(&:length),providerFiles:provider_sources.transform_values{|v|v.map{|p|p.delete_prefix(root+'/')}}};out=File.join(root,'work/audit/real11-category-game-separation.json');FileUtils.mkdir_p(File.dirname(out));File.write(out,JSON.pretty_generate(result)+"\n");puts "REAL11 #{result[:passed]?'passed':'failed'}: category commons and concrete games are separated";exit(result[:passed]?0:1)
