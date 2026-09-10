#!/usr/bin/env ruby
require 'fileutils'
require 'json'
require 'open3'
require 'rexml/document'
require 'tmpdir'

root = File.expand_path('..', __dir__)
publisher = File.join(root, 'tools/room-rules-publisher.rb')
source = File.expand_path('../Client/docs/开房规则表/跑得快/成都跑得快.xlsx', root)

def run_publisher(publisher, workbook, output, registry)
  env = { 'AOO_ROOM_RULE_WORKBOOK'=>workbook, 'AOO_ROOM_RULE_GENERATED'=>output,
    'AOO_ROOM_RULE_REGISTRY'=>registry }
  stdout, stderr, status = Open3.capture3(env, 'ruby', publisher, 'generate')
  abort("publisher failed: #{stderr.empty? ? stdout : stderr}") unless status.success?
  JSON.parse(File.read(output, encoding: 'UTF-8'))
end

def inline_cell(reference, value)
  cell = REXML::Element.new('c'); cell.add_attribute('r', reference); cell.add_attribute('t', 'inlineStr')
  inline = cell.add_element('is'); inline.add_element('t').text = value.to_s
  cell
end

def rewrite_workbook(workbook)
  Dir.mktmpdir('aoo-room-rule-xlsx') do |directory|
    abort('unzip failed') unless system('unzip', '-qq', workbook, '-d', directory)
    sheet_path = File.join(directory, 'xl/worksheets/sheet1.xml')
    document = REXML::Document.new(File.read(sheet_path, encoding: 'UTF-8'))
    sheet_data = REXML::XPath.first(document, '//*[local-name()="sheetData"]')
    yield sheet_data
    File.write(sheet_path, document.to_s)
    File.delete(workbook)
    abort('zip failed') unless system('zip', '-qr', workbook, '.', chdir: directory)
  end
end

def set_cell(row, column, value)
  reference = "#{column}#{row.attributes['r']}"
  old = row.elements.to_a.find { |element| element.attributes['r'] == reference }
  row.delete(old) if old
  row.add_element(inline_cell(reference, value))
end

Dir.mktmpdir('aoo-room-rule-schema-test') do |directory|
  workbook = File.join(directory, 'rules.xlsx')
  output = File.join(directory, 'rules.generated.json')
  registry = File.join(directory, 'rules.keys.json')
  FileUtils.cp(source, workbook)

  baseline = run_publisher(publisher, workbook, output, registry)
  abort('platform required fields did not receive stable keys') unless
    baseline.fetch('fields').any? { |field| field['key'] == 'playerCount' && field['label'] == '人数' } &&
    baseline.fetch('fields').any? { |field| field['key'] == 'roundCount' && field['label'] == '局数' }
  small = baseline.fetch('fields').find { |field| field['label'] == '小结算' }
  abort('official small settlement row did not use generic schema path') unless small &&
    small['control'] == 'checkbox' && small['defaultCandidateIndexes'] == [1] &&
    small['trusteeCount'] == 3 && small['visible'] == true

  last_good = File.binread(output)
  rewrite_workbook(workbook) do |sheet_data|
    player_count = sheet_data.elements.to_a.find do |row|
      REXML::XPath.match(row, './*[local-name()="c"]/*[local-name()="is"]/*[local-name()="t"]')
        .any? { |text| text.text == '人数' }
    end
    abort('official player count row missing from fixture') unless player_count
    sheet_data.delete(player_count)
  end
  _stdout, missing_stderr, missing_status = Open3.capture3(
    { 'AOO_ROOM_RULE_WORKBOOK'=>workbook, 'AOO_ROOM_RULE_GENERATED'=>output,
      'AOO_ROOM_RULE_REGISTRY'=>registry }, 'ruby', publisher, 'generate')
  abort('missing required playerCount unexpectedly published') if missing_status.success?
  abort("missing required field error lacks stable identity: #{missing_stderr}") unless
    missing_stderr.include?('缺少平台必填规则字段') && missing_stderr.include?('playerCount')
  abort('missing required field overwrote last good output') unless File.binread(output) == last_good
  FileUtils.cp(source, workbook)

  rewrite_workbook(workbook) do |sheet_data|
    last = sheet_data.elements.to_a.select { |element| element.name == 'row' }.map { |row| row.attributes['r'].to_i }.max
    row = REXML::Element.new('row'); row.add_attribute('r', (last + 1).to_s)
    { 'A'=>'临时规则', 'B'=>'多选', 'C'=>'选项甲｜选项乙', 'D'=>'1', 'E'=>'3', 'F'=>'是' }
      .each { |column, value| row.add_element(inline_cell("#{column}#{last + 1}", value)) }
    sheet_data.add_element(row)
    radio_row = REXML::Element.new('row'); radio_row.add_attribute('r', (last + 2).to_s)
    { 'A'=>'临时单选', 'B'=>'单选', 'C'=>'一｜二｜三｜四｜五｜不比', 'D'=>'6', 'E'=>'3', 'F'=>'是' }
      .each { |column, value| radio_row.add_element(inline_cell("#{column}#{last + 2}", value)) }
    sheet_data.add_element(radio_row)
  end
  added = run_publisher(publisher, workbook, output, registry)
  added_field = added.fetch('fields').find { |field| field['label'] == '临时规则' }
  abort('arbitrary added row was not generated') unless added_field
  radio_field = added.fetch('fields').find { |field| field['label'] == '临时单选' }
  abort('radio default 6 was not mapped exclusively to option six') unless
    radio_field && radio_field['defaultCandidateIndexes'] == [6] &&
    radio_field['defaultValue'] == radio_field.fetch('options')[5].fetch('value')
  stable_key = added_field.fetch('key')

  rewrite_workbook(workbook) do |sheet_data|
    row = sheet_data.elements.to_a.find do |candidate|
      REXML::XPath.match(candidate, './*[local-name()="c"]/*[local-name()="is"]/*[local-name()="t"]')
        .any? { |text| text.text == '临时规则' }
    end
    set_cell(row, 'C', '一｜二｜三｜四｜五')
    set_cell(row, 'D', '123456')
  end
  tolerant = run_publisher(publisher, workbook, output, registry)
  tolerant_field = tolerant.fetch('fields').find { |field| field['label'] == '临时规则' }
  abort('checkbox defaults did not preserve existing indexes and ignore overflow') unless
    tolerant_field && tolerant_field['defaultCandidateIndexes'] == [1, 2, 3, 4, 5]

  rewrite_workbook(workbook) do |sheet_data|
    row = sheet_data.elements.to_a.find do |candidate|
      REXML::XPath.match(candidate, './*[local-name()="c"]/*[local-name()="is"]/*[local-name()="t"]')
        .any? { |text| text.text == '临时单选' }
    end
    set_cell(row, 'C', '一｜二｜三｜四｜五')
  end
  radio_six, radio_six_stderr, radio_six_status = Open3.capture3(
    { 'AOO_ROOM_RULE_WORKBOOK'=>workbook, 'AOO_ROOM_RULE_GENERATED'=>output,
      'AOO_ROOM_RULE_REGISTRY'=>registry }, 'ruby', publisher, 'generate')
  abort("out-of-range radio default did not normalize: #{radio_six_stderr}") unless radio_six_status.success?
  radio_six_payload = JSON.parse(File.read(output, encoding: 'UTF-8'))
  radio_six_field = radio_six_payload.fetch('fields').find { |field| field['label'] == '临时单选' }
  abort('radio 5 options default 6 did not normalize to first option') unless
    radio_six_field['defaultCandidateIndexes'] == [1] && radio_six_stderr.include?('已规范化为第1项')

  rewrite_workbook(workbook) do |sheet_data|
    row = sheet_data.elements.to_a.find do |candidate|
      REXML::XPath.match(candidate, './*[local-name()="c"]/*[local-name()="is"]/*[local-name()="t"]')
        .any? { |text| text.text == '临时单选' }
    end
    set_cell(row, 'D', '16')
  end
  _radio_sixteen_stdout, radio_sixteen_stderr, radio_sixteen_status = Open3.capture3(
    { 'AOO_ROOM_RULE_WORKBOOK'=>workbook, 'AOO_ROOM_RULE_GENERATED'=>output,
      'AOO_ROOM_RULE_REGISTRY'=>registry }, 'ruby', publisher, 'generate')
  abort("multi-digit radio default did not normalize: #{radio_sixteen_stderr}") unless radio_sixteen_status.success?
  radio_sixteen_payload = JSON.parse(File.read(output, encoding: 'UTF-8'))
  radio_sixteen_field = radio_sixteen_payload.fetch('fields').find { |field| field['label'] == '临时单选' }
  abort('radio 5 options default 16 did not normalize to first option') unless
    radio_sixteen_field['defaultCandidateIndexes'] == [1] && radio_sixteen_stderr.include?('已规范化为第1项')

  rewrite_workbook(workbook) do |sheet_data|
    row = sheet_data.elements.to_a.find do |candidate|
      REXML::XPath.match(candidate, './*[local-name()="c"]/*[local-name()="is"]/*[local-name()="t"]')
        .any? { |text| text.text == '临时单选' }
    end
    set_cell(row, 'C', '一｜二｜三｜四｜五｜不比')
    set_cell(row, 'D', '6')
  end

  rewrite_workbook(workbook) do |sheet_data|
    row = sheet_data.elements.to_a.find do |candidate|
      REXML::XPath.match(candidate, './*[local-name()="c"]/*[local-name()="is"]/*[local-name()="t"]')
        .any? { |text| text.text == '临时规则' }
    end
    abort('fixture row missing') unless row
    set_cell(row, 'C', '新选项甲｜新选项乙｜新选项丙')
    set_cell(row, 'D', '2,3')
    set_cell(row, 'E', '5')
    set_cell(row, 'F', '否')
  end
  changed = run_publisher(publisher, workbook, output, registry)
  changed_field = changed.fetch('fields').find { |field| field['label'] == '临时规则' }
  abort('field key changed after property edits') unless changed_field.fetch('key') == stable_key
  abort('options/default/trustee/visibility did not synchronize') unless
    changed_field.fetch('options').map { |option| option['label'] } == %w[新选项甲 新选项乙 新选项丙] &&
    changed_field['defaultCandidateIndexes'] == [2, 3] && changed_field['trusteeCount'] == 5 && changed_field['visible'] == false

  rewrite_workbook(workbook) do |sheet_data|
    row = sheet_data.elements.to_a.find do |candidate|
      REXML::XPath.match(candidate, './*[local-name()="c"]/*[local-name()="is"]/*[local-name()="t"]')
        .any? { |text| text.text == '临时规则' }
    end
    abort('fixture row missing before invalid-default check') unless row
    set_cell(row, 'D', '2,x')
  end
  _stdout, invalid_stderr, invalid_status = Open3.capture3(
    { 'AOO_ROOM_RULE_WORKBOOK'=>workbook, 'AOO_ROOM_RULE_GENERATED'=>output,
      'AOO_ROOM_RULE_REGISTRY'=>registry }, 'ruby', publisher, 'generate')
  abort('invalid checkbox default unexpectedly published') if invalid_status.success?
  abort("default error lacks exact cell diagnostics: #{invalid_stderr}") unless
    invalid_stderr.include?('临时规则/默认勾选(D') && invalid_stderr.include?('默认序号必须是1基整数')

  rewrite_workbook(workbook) do |sheet_data|
    row = sheet_data.elements.to_a.find do |candidate|
      REXML::XPath.match(candidate, './*[local-name()="c"]/*[local-name()="is"]/*[local-name()="t"]')
        .any? { |text| text.text == '临时规则' }
    end
    set_cell(row, 'D', '2,3')
  end

  rewrite_workbook(workbook) do |sheet_data|
    row = sheet_data.elements.to_a.find do |candidate|
      REXML::XPath.match(candidate, './*[local-name()="c"]/*[local-name()="is"]/*[local-name()="t"]')
        .any? { |text| text.text == '临时规则' }
    end
    sheet_data.delete(row)
  end
  removed = run_publisher(publisher, workbook, output, registry)
  abort('deleted row remained in generated schema') if removed.fetch('fields').any? { |field| field['key'] == stable_key }
  tombstone = JSON.parse(File.read(registry, encoding: 'UTF-8')).fetch('fields').find { |field| field['key'] == stable_key }
  abort('deleted row did not retain a stable tombstone') unless tombstone && tombstone['active'] == false
end

puts 'room-rule schema evolution checks passed'
