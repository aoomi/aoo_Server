require 'json'
require 'fileutils'

root = File.expand_path('../../..', __dir__)
category_paths = {
  'Mahjong' => 'mahjong',
  'Poker' => 'poker',
  'LongCard' => 'long-card',
  'WordCard' => 'long-card/compatibility/WordCard'
}
game_paths = {
  'CDXZMJ' => 'mahjong/CDXZMJ',
  'SCJYMJ' => 'mahjong/SCJYMJ',
  'XCPDK' => 'poker/PDK',
  'ZJH' => 'poker/zjh',
  'ZYPK' => 'poker/ZYPK'
}
categories = category_paths.keys
games = game_paths.keys
category_sources = category_paths.to_h { |name, path| [name, Dir[File.join(root, "modules/gameplay/games/#{path}/src/main/java/**/*.java")] ] }
provider_sources = game_paths.to_h { |name, path| [name, Dir[File.join(root, "modules/gameplay/games/#{path}/src/**/*GameProvider.java")] ] }
category_poms = category_paths.to_h { |name, path| [name, File.read(File.join(root, "modules/gameplay/games/#{path}/pom.xml"))] }
game_poms = game_paths.to_h { |name, path| [name, File.read(File.join(root, "modules/gameplay/games/#{path}/pom.xml"))] }
expected = {
  'CDXZMJ' => 'game-category-mahjong',
  'SCJYMJ' => 'game-category-mahjong',
  'XCPDK' => 'game-category-poker',
  'ZJH' => 'game-category-poker',
  'ZYPK' => 'game-category-poker'
}
checks = {
  four_category_modules: category_sources.values.all? { |files| !files.empty? },
  categories_own_core_rule_types: category_sources.values.all? { |files| files.any? { |p| File.basename(p).match?(/CoreEngine|RuleSet|RuleFamily/) } },
  concrete_providers: provider_sources.values.all? { |files| files.length == 1 },
  concrete_games_depend_on_category: expected.all? { |name, artifact| game_poms[name].include?("<artifactId>#{artifact}</artifactId>") },
  categories_do_not_depend_on_concrete_games: category_poms.values.none? { |pom| games.any? { |name| pom.downcase.include?("<artifactid>#{name.downcase}</artifactid>") } }
}
result = { task: 'REAL11', passed: checks.values.all?, checks: checks,
  categorySourceCounts: category_sources.transform_values(&:length),
  providerFiles: provider_sources.transform_values { |files| files.map { |p| p.delete_prefix(root + '/') } } }
out = File.join(root, 'build-only/audit/real11-category-game-separation.json')
FileUtils.mkdir_p(File.dirname(out))
File.write(out, JSON.pretty_generate(result) + "\n")
puts "REAL11 #{result[:passed] ? 'passed' : 'failed'}: category commons and concrete games are separated"
exit(result[:passed] ? 0 : 1)
