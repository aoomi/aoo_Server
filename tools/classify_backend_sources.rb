#!/usr/bin/env ruby
# frozen_string_literal: true

root, output = ARGV
abort 'usage: classify_backend_sources.rb SOURCE_ROOT OUTPUT' unless root && output

def category_from_module(module_name)
  code = module_name.upcase
  return 'WORD_CARD' if code.match?(/(?:PHZ|ZP|SDR|CHZ|HP)$/)
  return 'LONG_CARD' if code.match?(/(?:CP|DSS)$/)
  return 'POKER' if code.match?(/(?:PDK|DDZ|PK)$/)
  return 'MAHJONG' if code.end_with?('MJ')

  nil
end

def classify(module_name, content)
  scores = {
    'MAHJONG' => content.scan(/business\.global\.mj|MJTemplate|MahjongRoom|AbsMJ|MJRoom/).size,
    'POKER' => content.scan(/business\.global\.pk|PockerRoom|PKRoom|BasePocker|PKCard/).size,
    'LONG_CARD' => content.scan(/longcard|LongCard|LongPoker|DSSRoom|CPRoom/).size,
    'WORD_CARD' => content.scan(/paohuzi|PaoHuZi|PHZRoom|ZiPai|ZPRoom|WordCard/).size
  }
  if %w[common commdef gamehall gameserver test].include?(module_name.downcase)
    return ['INFRASTRUCTURE', 'backend-framework', scores]
  end
  # NTCP is Ningde Mahjong. Its historical suffix means "宁德/宁城特色牌",
  # not the CP long-card family; the authority extends AbsMJ throughout and
  # opens one or two wildcard "金" tiles.
  return ['MAHJONG', 'mahjong-lai-zi', scores] if module_name.casecmp?('NTCP')
  # Historical CP suffixes are ambiguous. Authority inheritance wins over names:
  # CP/CQCP are PKRoom games; ZGCP/ZGDSS are MahjongRoom games.
  return ['POKER', 'poker-510k', scores] if module_name.casecmp?('CP')
  return ['POKER', 'poker-trick-taking', scores] if module_name.casecmp?('CQCP')
  # These reuse MahjongRoom mechanics internally, but authoritative clients identify
  # their physical deck games as 自贡长牌 and 自贡斗十四 rather than Mahjong.
  return ['LONG_CARD', 'long-card-zigong', scores] if module_name.casecmp?('ZGCP')
  return ['LONG_CARD', 'long-card-zigong-da-si-shi', scores] if module_name.casecmp?('ZGDSS')
  # HNDZP is 海南地主牌 (landlord bidding/play), while LHZP is 莲花猪牌,
  # a four-seat climbing/shedding game.  Their historical ZP suffix does not
  # denote the pao-hu-zi word-card family.
  return ['POKER', 'poker-hainan-landlord', scores] if module_name.casecmp?('HNDZP')
  return ['POKER', 'poker-climbing', scores] if module_name.casecmp?('LHZP')

  named_category = category_from_module(module_name)
  category, score = named_category ? [named_category, scores.fetch(named_category)] : scores.max_by { |_key, value| value }
  return ['REVIEW_REQUIRED', 'source-analysis-required', scores] if named_category.nil? && score.zero?

  family = case category
           when 'MAHJONG'
             if content.match?(/XueZhan|xuezhan|血战/) then 'mahjong-xue-zhan'
             elsif content.match?(/XueLiu|xueliu|血流/) then 'mahjong-xue-liu'
             elsif content.match?(/TuiDaoHu|tuidaohu|推倒胡/) then 'mahjong-tui-dao-hu'
             elsif content.match?(/LaiZi|laizi|HunPai|百搭/) then 'mahjong-lai-zi'
             else 'mahjong-standard'
             end
           when 'POKER'
             if content.match?(/PDK|PaoDeKuai|跑得快/) then 'poker-pao-de-kuai'
             elsif content.match?(/DDZ|DouDiZhu|斗地主/) then 'poker-dou-di-zhu'
             elsif content.match?(/ShengJi|Tractor|拖拉机|升级/) then 'poker-sheng-ji'
             else 'poker-trick-taking'
             end
           when 'LONG_CARD' then 'long-card-regional'
           when 'WORD_CARD' then 'word-card-pao-hu-zi'
           end
  [category, family, scores]
end

rows = []
Dir.children(root).sort.each do |module_name|
  src = File.join(root, module_name, 'src')
  next unless File.directory?(src)
  files = Dir.glob(File.join(src, '**', '*.java'))
  next if files.empty?

  content = files.map { |file| File.read(file, encoding: 'UTF-8', invalid: :replace, undef: :replace) }.join("\n")
  category, family, scores = classify(module_name, content)
  evidence = scores.map { |key, value| "#{key}:#{value}" }.join(',')
  rows << [module_name, files.size, category, family, evidence]
end

File.open(output, 'w:UTF-8') do |file|
  file.puts "module\tjavaFiles\tcategory\tfamily\tevidenceScores"
  rows.each { |row| file.puts row.join("\t") }
end

counts = rows.group_by { |row| row[2] }.transform_values(&:size)
warn "modules=#{rows.size} " + counts.sort.map { |key, value| "#{key}=#{value}" }.join(' ')
game_rows = rows.reject { |row| row[2] == 'INFRASTRUCTURE' }
abort "classification must contain exactly 528 games, got #{game_rows.size}" unless game_rows.size == 528
abort 'unclassified backend sources remain' unless counts.fetch('REVIEW_REQUIRED', 0).zero?
expected = {'MAHJONG' => 364, 'POKER' => 150, 'WORD_CARD' => 10, 'LONG_CARD' => 4}
actual = game_rows.group_by { |row| row[2] }.transform_values(&:size)
abort "classification category mismatch: #{actual}" unless actual == expected
