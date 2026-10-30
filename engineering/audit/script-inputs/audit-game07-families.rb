require 'json'
require 'rexml/document'

root = File.expand_path('../../..', __dir__)
modules = %w[Families Games/Mahjong Games/Poker Games/LongCard Games/LongCard/Compatibility/WordCard]
reports = modules.to_h do |path|
  reports = Dir[File.join(root, "server/#{path}/target/surefire-reports/TEST-*.xml")]
  passed = reports.all? do |report|
    document = REXML::Document.new(File.read(report))
    document.root.attributes['failures'].to_i.zero? && document.root.attributes['errors'].to_i.zero?
  end
  [path, { 'files' => reports.size, 'passed' => passed }]
end
provider_paths = {
  'XCPDK' => 'Games/Poker/PDK',
  'CDXZMJ' => 'Games/Mahjong/CDXZMJ',
  'SCJYMJ' => 'Games/Mahjong/SCJYMJ',
  'ZJH' => 'Games/Poker/ZJH',
  'ZYPK' => 'Games/Poker/ZYPK'
}
providers = provider_paths.to_h do |name, path|
  source = Dir[File.join(root, "server/#{path}/src/**/*GameProvider.java")].map { |file| File.read(file) }.join
  [name, {
    'categoryProvider' => source.include?('PokerGameProvider') || source.include?('MahjongGameProvider'),
    'legacyFactory' => source.include?('createLegacyRoom'),
    'authoritativeRestore' => source.include?('restoreAuthoritativeSession')
  }]
end
out = {
  'task' => 'GAME07',
  'status' => 'blocked-representative-legacy-boundary',
  'categoryTests' => reports,
  'representativeProviders' => providers,
  'unresolved' => [
    'XCPDK, CDXZMJ and SCJYMJ factories still construct legacy rooms alongside common authoritative sessions',
    'ZJH and ZYPK still expose legacy aggregate handles and lack authoritative restore SPI',
    'four-layer abstractions therefore are not the sole effective production path'
  ]
}
File.write(File.join(root, 'docs/generated/game07-family-coverage.json'), JSON.pretty_generate(out) + "\n")
puts 'GAME07 evidence recorded'
