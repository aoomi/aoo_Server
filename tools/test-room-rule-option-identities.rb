#!/usr/bin/env ruby
require 'fileutils'
require 'json'
require 'open3'
require 'tmpdir'

root = File.expand_path('..', __dir__)
publisher = File.join(root, 'tools/room-rules-publisher.rb')
workbook = File.expand_path('../Client/docs/开房规则表/跑得快/成都跑得快.xlsx', root)
identity_source = File.join(root, 'tools/room-rule-play-identities.json')

def publish(publisher, workbook, output, registry, identity)
  Open3.capture3({
    'AOO_ROOM_RULE_WORKBOOK'=>workbook,
    'AOO_ROOM_RULE_GENERATED'=>output,
    'AOO_ROOM_RULE_REGISTRY'=>registry,
    'AOO_ROOM_RULE_IDENTITY_REGISTRY'=>identity
  }, 'ruby', publisher, 'generate')
end

def cd201_identity(path)
  payload = JSON.parse(File.read(path, encoding: 'UTF-8'))
  [payload, payload.fetch('plays').find { |entry| entry['gameCode'] == 'CD201' }]
end

def write_identity(path, payload)
  File.write(path, JSON.pretty_generate(payload) + "\n")
end

Dir.mktmpdir('aoo-room-rule-option-identities') do |directory|
  identity = File.join(directory, 'identities.json')
  output = File.join(directory, 'rules.generated.json')
  registry = File.join(directory, 'rules.keys.json')
  FileUtils.cp(identity_source, identity)

  stdout, stderr, status = publish(publisher, workbook, output, registry, identity)
  abort("baseline publisher failed: #{stderr.empty? ? stdout : stderr}") unless status.success?
  baseline = JSON.parse(File.read(output, encoding: 'UTF-8'))
  by_field = baseline.fetch('fields').to_h do |field|
    [field.fetch('label'), field.fetch('options').to_h { |option| [option.fetch('label'), option.fetch('value')] }]
  end
  expected = {
    '人数'=>{'2人'=>2, '3人'=>3},
    '局数'=>{'8局'=>8, '12局'=>12, '16局'=>16},
    '操作时间'=>{'10000秒'=>10_000, '15秒'=>15, '20秒'=>20},
    '先出牌'=>{'赢家先出'=>'winner_first', '黑桃3优先'=>'spade_three_first'},
    '炸弹记分'=>{'5分'=>5, '10分'=>10, '20分'=>20, '30分'=>30},
    '小结算'=>{'小局结算弹窗'=>'option_0001'},
    '玩法'=>{
      '三不带'=>'three_no_attachment', '四带二'=>'four_with_two',
      '3A算炸弹'=>'triple_ace_bomb', '去掉3、4'=>'remove_three_four',
      '必须出黑桃3'=>'require_spade_three'
    },
    '其他'=>{
      'IP限制'=>'ip_limit', 'GPS限制'=>'gps_limit',
      '超时托管'=>'timeout_auto_play', '距离过近警告'=>'distance_warning',
      '禁止互动'=>'interaction_forbidden', '禁言'=>'chat_muted'
    }
  }
  abort('CD201 full display-to-wire mapping drifted') unless by_field == expected
  baseline.fetch('fields').each do |field|
    identities = field.fetch('options').map { |option| [option.fetch('value').class.name, option.fetch('value')] }
    abort("duplicate option identity in #{field['label']}") unless identities.uniq.length == identities.length
  end

  polluted = JSON.parse(File.read(registry, encoding: 'UTF-8'))
  play = polluted.fetch('fields').find { |field| field['key'] == 'playRule' }
  play.fetch('options').each do |option|
    option['value'] = 3 if %w[3A算炸弹 去掉3、4 必须出黑桃3].include?(option['label'])
  end
  File.write(registry, JSON.pretty_generate(polluted) + "\n")
  _repair_stdout, repair_stderr, repair_status = publish(publisher, workbook, output, registry, identity)
  abort("explicit mapping did not repair polluted registry: #{repair_stderr}") unless repair_status.success?
  repaired = JSON.parse(File.read(registry, encoding: 'UTF-8'))
  repaired_values = repaired.fetch('fields').find { |field| field['key'] == 'playRule' }
    .fetch('options').to_h { |option| [option['label'], option['value']] }
  abort('polluted numeric threes survived registry repair') unless
    repaired_values.values_at('3A算炸弹', '去掉3、4', '必须出黑桃3') ==
      %w[triple_ace_bomb remove_three_four require_spade_three]

  numeric_identity = File.join(directory, 'numeric-identities.json')
  FileUtils.cp(identity_source, numeric_identity)
  numeric_payload, numeric_play = cd201_identity(numeric_identity)
  numeric_play.fetch('roomRuleOptionValues').fetch('炸弹记分').delete('30分')
  write_identity(numeric_identity, numeric_payload)
  numeric_output = File.join(directory, 'numeric.generated.json')
  numeric_registry = File.join(directory, 'numeric.keys.json')
  _numeric_stdout, numeric_stderr, numeric_status = publish(
    publisher, workbook, numeric_output, numeric_registry, numeric_identity)
  abort("declared numeric option failed: #{numeric_stderr}") unless numeric_status.success?
  numeric_field = JSON.parse(File.read(numeric_output, encoding: 'UTF-8')).fetch('fields')
    .find { |field| field['label'] == '炸弹记分' }
  abort('complete numeric label did not parse to its integer') unless
    numeric_field.fetch('options').find { |option| option['label'] == '30分' }.fetch('value') == 30

  contains_digit_identity = File.join(directory, 'contains-digit-identities.json')
  FileUtils.cp(identity_source, contains_digit_identity)
  contains_payload, contains_play = cd201_identity(contains_digit_identity)
  contains_play.fetch('roomRuleOptionValues').fetch('玩法').delete('3A算炸弹')
  write_identity(contains_digit_identity, contains_payload)
  _digit_stdout, digit_stderr, digit_status = publish(publisher, workbook,
    File.join(directory, 'contains-digit.generated.json'),
    File.join(directory, 'contains-digit.keys.json'), contains_digit_identity)
  abort('number-containing semantic label was guessed as an integer') if digit_status.success?
  abort('number-containing semantic label did not fail with exact diagnostics') unless
    digit_stderr.include?('玩法') && digit_stderr.include?('3A算炸弹') && digit_stderr.include?('未登记显示项')

  unknown_identity = File.join(directory, 'unknown-identities.json')
  FileUtils.cp(identity_source, unknown_identity)
  unknown_payload, unknown_play = cd201_identity(unknown_identity)
  unknown_play.fetch('roomRuleOptionValues').fetch('玩法').delete('三不带')
  write_identity(unknown_identity, unknown_payload)
  _unknown_stdout, unknown_stderr, unknown_status = publish(publisher, workbook,
    File.join(directory, 'unknown.generated.json'),
    File.join(directory, 'unknown.keys.json'), unknown_identity)
  abort('unknown display option unexpectedly published') if unknown_status.success?
  abort('unknown display option did not fail closed with diagnostics') unless
    unknown_stderr.include?('玩法') && unknown_stderr.include?('三不带') && unknown_stderr.include?('禁止从显示文字猜值')
end

puts 'room-rule option identity checks passed'

liangshan_workbook = File.expand_path(
  '../Client/docs/开房规则表/跑得快/凉山跑得快.xlsx', root)
Dir.mktmpdir('aoo-liangshan-room-rule-numeric-identities') do |directory|
  output = File.join(directory, 'rules.generated.json')
  registry = File.join(directory, 'rules.keys.json')
  identity = File.join(directory, 'identities.json')
  identity_payload = JSON.parse(File.read(identity_source, encoding: 'UTF-8'))
  liangshan_identity = identity_payload.fetch('plays').find { |entry| entry['gameCode'] == 'LS201' }
  # The regression is specifically that a workbook value change used to need a
  # second manual numericRoomRuleFields edit.  Remove that optional declaration
  # and prove the durable technical identity can rebuild the complete
  # publication from an empty generated-key registry.
  liangshan_identity.delete('numericRoomRuleFields')
  write_identity(identity, identity_payload)
  stdout, stderr, status = publish(
    publisher, liangshan_workbook, output, registry, identity)
  abort("LS201 cold-start room-rule publication failed: #{stderr.empty? ? stdout : stderr}") unless status.success?

  fields = JSON.parse(File.read(output, encoding: 'UTF-8')).fetch('fields')
  by_key = fields.to_h { |field| [field.fetch('key'), field] }
  expected_keys = %w[playerCount roundCount operationTime dealCardCount jinHuaScore
    robDealerRule rule_ls201_0001 playRule roomRestriction]
  abort('LS201 cold-start field identities drifted') unless by_key.keys == expected_keys

  operation_time = by_key.fetch('operationTime')
  abort('LS201 operationTime did not map 10000秒 to integer 10000') unless
    operation_time.fetch('options').any? do |option|
      option['label'] == '10000秒' && option['value'] == 10_000
    end
  deal_counts = by_key.fetch('dealCardCount').fetch('options')
    .to_h { |option| [option.fetch('label'), option.fetch('value')] }
  abort('LS201 deal-card identities were not rebuilt without cache') unless
    deal_counts == {'8张(7-A)'=>8, '10张(5-A)'=>10}
  abort('LS201 cold-start registry was not persisted') unless File.file?(registry)
end

puts 'LS201 cold-start numeric room-rule identity checks passed'
