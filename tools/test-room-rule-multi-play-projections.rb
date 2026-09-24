#!/usr/bin/env ruby
require 'fileutils'
require 'json'
require 'open3'
require 'tmpdir'

root = File.expand_path('..', __dir__)
project_root = File.expand_path('..', root)
publisher = File.join(root, 'tools/room-rules-publisher.rb')

def generate(publisher, workbook, directory)
  output = File.join(directory, 'rules.generated.json')
  registry = File.join(directory, 'rules.keys.json')
  stdout, stderr, status = Open3.capture3({
    'AOO_ROOM_RULE_WORKBOOK'=>workbook,
    'AOO_ROOM_RULE_GENERATED'=>output,
    'AOO_ROOM_RULE_REGISTRY'=>registry
  }, 'ruby', publisher, 'generate')
  abort("#{File.basename(workbook)} projection failed: #{stderr.empty? ? stdout : stderr}") unless status.success?
  JSON.parse(File.read(output, encoding: 'UTF-8'))
end

cases = [
  {
    code: 'CN298', workbook: 'Client/docs/开房规则表/牛牛/全国牛牛.xlsx',
    keys: %w[rounds maxPlayers startPlayers mode maxRobMultiplier maxPushMultiplier standPolicy fastModeEnabled hostingMissThreshold]
  },
  {
    code: 'CN297', workbook: 'Client/docs/开房规则表/金花/全国金花CN297.xlsx',
    keys: %w[totalRounds seatLimit minimumPlayers operationSeconds compareStartRound maximumBet mustBlindRounds baseBet aaaBonus leopardBonus straightFlushBonus hostingMissThreshold]
  },
  {
    code: 'CD299', workbook: 'Client/docs/开房规则表/扯旋/成都扯旋CD299.xlsx',
    keys: %w[roomEndSelection startPlayers operationSeconds standPolicy mangoFlipMode mangoRaise mangoScore openingBet restMango beatMango everyHandMango firstRoundCanRest eachPlayerMustFollow earthNineKing fireproofCard bigHeadKeepsBase autoOperateAfterTimeouts]
  }
]

cases.each do |entry|
  Dir.mktmpdir("aoo-#{entry.fetch(:code).downcase}-room-rule-projection") do |directory|
    payload = generate(publisher, File.join(project_root, entry.fetch(:workbook)), directory)
    fields = payload.fetch('fields')
    abort("#{entry.fetch(:code)} field projection drifted") unless fields.map { |field| field.fetch('key') } == entry.fetch(:keys)
    hosting = fields.last
    hosting_key = entry.fetch(:code) == 'CD299' ? 'autoOperateAfterTimeouts' : 'hostingMissThreshold'
    abort("#{entry.fetch(:code)} trustee projection drifted") unless
      hosting['key'] == hosting_key && hosting['visible'] == false &&
      hosting['disabled'] == true && hosting['defaultValue'] == 3 &&
      hosting.fetch('options').map { |option| option.fetch('value') } == [3, 4, 5]
    if entry.fetch(:code) == 'CN298'
      fast = fields.find { |field| field['key'] == 'fastModeEnabled' }
      abort('CN298 boolean option projection drifted') unless fast && fast['defaultValue'] == true
    elsif entry.fetch(:code) == 'CN297'
      abort('CN297 nested player projection drifted') unless
        fields.find { |field| field['key'] == 'seatLimit' }.fetch('defaultValue') == 8 &&
        fields.find { |field| field['key'] == 'minimumPlayers' }.fetch('defaultValue') == 2
      abort('CN297 numeric tuple projection drifted') unless
        %w[aaaBonus leopardBonus straightFlushBonus].map do |key|
          fields.find { |field| field['key'] == key }.fetch('defaultValue')
        end == [20, 10, 5]
    else
      ending = fields.find { |field| field['key'] == 'roomEndSelection' }
      abort('CD299 room-end projection drifted') unless ending &&
        ending['defaultValue'] == 'ROUND_COUNT:10' &&
        ending.fetch('options').map { |option| option.fetch('value') } ==
          %w[DURATION_MINUTES:30 DURATION_MINUTES:45 DURATION_MINUTES:60 ROUND_COUNT:10 ROUND_COUNT:20 ROUND_COUNT:30]
      flags = %w[restMango beatMango everyHandMango firstRoundCanRest eachPlayerMustFollow earthNineKing fireproofCard bigHeadKeepsBase]
      abort('CD299 boolean rule projection drifted') unless
        flags.map { |key| fields.find { |field| field['key'] == key }.fetch('defaultValue') } ==
          [true, true, true, true, false, true, true, false]
    end
  end
end

registry = JSON.parse(File.read(File.join(root, 'tools/room-rule-play-identities.json'), encoding: 'UTF-8'))
cd299 = registry.fetch('plays').find { |play| play['gameCode'] == 'CD299' }
abort('CD299 candidate must remain excluded from automatic publication') unless
  cd299 && cd299['publishingEnabled'] == true && cd299['automaticPublishingEnabled'] == false

puts 'multi-play room-rule projection checks passed'
