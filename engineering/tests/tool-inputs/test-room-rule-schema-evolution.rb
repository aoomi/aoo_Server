#!/usr/bin/env ruby
require 'fileutils'
require 'json'
require 'open3'
require 'rexml/document'
require 'tmpdir'
require 'digest'

root = File.expand_path('../../..', __dir__)
publisher = File.join(root, 'engineering/release/tool-inputs/room-rules-publisher.rb')
source = File.expand_path('../Client/docs/开房规则表/跑得快/成都跑得快.xlsx', root)
identity_source = File.join(root, 'release-input/identity/room-rule-play-identities.json')

def run_publisher(publisher, workbook, output, registry)
  env = { 'AOO_ROOM_RULE_WORKBOOK'=>workbook, 'AOO_ROOM_RULE_GENERATED'=>output,
    'AOO_ROOM_RULE_REGISTRY'=>registry }
  stdout, stderr, status = Open3.capture3(env, 'ruby', publisher, 'generate')
  abort("publisher failed: #{stderr.empty? ? stdout : stderr}") unless status.success?
  JSON.parse(File.read(output, encoding: 'UTF-8'))
end

def sql_publisher(publisher, workbook, output, registry, identity: nil, diagnostic: false)
  env = { 'AOO_ROOM_RULE_WORKBOOK'=>workbook, 'AOO_ROOM_RULE_GENERATED'=>output,
    'AOO_ROOM_RULE_REGISTRY'=>registry }
  env['AOO_ROOM_RULE_IDENTITY_REGISTRY'] = identity if identity
  env['AOO_ROOM_RULE_DIAGNOSTIC'] = '1' if diagnostic
  stdout, stderr, status = Open3.capture3(env, 'ruby', publisher, 'sql')
  abort("publisher sql failed: #{stderr.empty? ? stdout : stderr}") unless status.success?
  stdout
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
    shared_path = File.join(directory, 'xl/sharedStrings.xml')
    if File.file?(shared_path)
      shared_document = REXML::Document.new(File.read(shared_path, encoding: 'UTF-8'))
      shared = REXML::XPath.match(shared_document, '//*[local-name()="si"]').map do |item|
        REXML::XPath.match(item, './/*[local-name()="t"]').map(&:text).join
      end
      REXML::XPath.match(document, '//*[local-name()="c"][@t="s"]').each do |cell|
        value = REXML::XPath.first(cell, './*[local-name()="v"]')
        next unless value
        text = shared.fetch(Integer(value.text, 10))
        cell.delete(value); cell.attributes['t'] = 'inlineStr'
        inline = cell.add_element('is'); inline.add_element('t').text = text
      end
    end
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

def register_option_values(identity_path, field_label, values)
  payload = JSON.parse(File.read(identity_path, encoding: 'UTF-8'))
  play = payload.fetch('plays').find { |entry| entry['gameCode'] == 'chengdu2001' }
  abort('chengdu2001 identity missing from fixture') unless play
  play['roomRuleOptionValues'] ||= {}
  play['roomRuleOptionValues'][field_label] = values
  File.write(identity_path, JSON.pretty_generate(payload) + "\n")
end

Dir.mktmpdir('aoo-room-rule-schema-test') do |directory|
  workbook = File.join(directory, 'rules.xlsx')
  output = File.join(directory, 'rules.generated.json')
  registry = File.join(directory, 'rules.keys.json')
  identity = File.join(directory, 'room-rule-play-identities.json')
  FileUtils.cp(source, workbook)
  FileUtils.cp(identity_source, identity)
  ENV['AOO_ROOM_RULE_IDENTITY_REGISTRY'] = identity

  baseline = run_publisher(publisher, workbook, output, registry)
  publisher_source = File.read(publisher, encoding: 'UTF-8')
  abort('publisher does not use an explicit authorized manifest contract') unless
    publisher_source.include?('AUTHORIZED_RUNTIME_METADATA_CONTRACTS') &&
    publisher_source.include?("'manifestVersion' => 'r50-current-20261011'") &&
    publisher_source.include?("'releaseIssuer' => '/root/lobby_runtime_from_staged_source'") &&
    publisher_source.include?('canonical runtime metadata required field contract mismatch')
  normal_sql = sql_publisher(publisher, workbook, output, registry)
  normal_start = normal_sql.index('START TRANSACTION;')
  normal_set = normal_sql.index('SET @source_hash=')
  abort('normal SQL unexpectedly contains transaction diagnostic or changed preamble') if
    normal_sql.include?('ROOM_RULE_DIAGNOSTIC') || normal_sql.include?('candidate_precheck') ||
    !normal_start || !normal_set || normal_start > normal_set
  content_hash_contract = {
    'catalog snapshot hash is recomputed from final JSON content' =>
      normal_sql.include?('SHA2(CAST(p.catalog_snapshot AS CHAR CHARACTER SET utf8mb4),256) catalog_hash'),
    'compiled validator hash is recomputed from final JSON content' =>
      normal_sql.include?('SHA2(CAST(p.validator AS CHAR CHARACTER SET utf8mb4),256) rule_hash'),
    'compiled component chain hash is recomputed from final JSON content' =>
      normal_sql.include?('SHA2(CAST(p.component_chain AS CHAR CHARACTER SET utf8mb4),256) component_hash'),
    'release and index bundle use one content envelope' =>
      normal_sql.include?("JSON_OBJECT('catalogSnapshot',JSON_EXTRACT(p.catalog_snapshot,'$'),'ruleValidator',JSON_EXTRACT(p.validator,'$'),'uiSchema',JSON_EXTRACT(p.ui_schema,'$'),'componentSnapshot',JSON_EXTRACT(p.component_snapshot,'$'),'componentChain',JSON_EXTRACT(p.component_chain,'$'))") &&
      normal_sql.scan('bundle_hash').length >= 5,
    'candidate includes every legacy content-hash mismatch' =>
      normal_sql.include?('p.existing_catalog_hash IS NULL') &&
      normal_sql.include?('p.existing_catalog_hash<>p.catalog_hash') &&
      normal_sql.include?('p.existing_rule_hash IS NULL') &&
      normal_sql.include?('p.existing_rule_hash<>p.rule_hash') &&
      normal_sql.include?('p.existing_component_hash IS NULL') &&
      normal_sql.include?('p.existing_component_hash<>p.component_hash') &&
      normal_sql.include?('p.existing_bundle_hash IS NULL') &&
      normal_sql.include?('p.existing_bundle_hash<>p.bundle_hash') &&
      normal_sql.include?('p.existing_index_bundle_hash IS NULL') &&
      normal_sql.include?('p.existing_index_bundle_hash<>p.bundle_hash') &&
      normal_sql.include?('p.existing_bundle_hash<>p.existing_index_bundle_hash'),
    'new release writes the computed content hashes rather than old values' =>
      normal_sql.include?('component_snapshot,catalog_hash,rule_hash,@source_hash,component_hash,bundle_hash') &&
      normal_sql.include?("component_chain,JSON_EXTRACT(validator,'$'),ui_schema")
  }
  failed_hash_contract = content_hash_contract.reject { |_, passed| passed }
  abort("publisher content-hash contract failed: #{failed_hash_contract.keys.join(', ')}") unless failed_hash_contract.empty?
  runtime_manifest = JSON.parse(File.read(File.join(root, 'tools/canonical-runtime-metadata-manifest.json'), encoding: 'UTF-8'))
  runtime_row = runtime_manifest.fetch('rows').find { |row| row.fetch('gameId') == 2001 }
  abort('current canonical runtime row missing') unless runtime_row
  abort('publisher did not consume canonical runtime metadata literals') unless
    normal_sql.include?('room-snapshot-v1') && normal_sql.include?('card-v3') &&
    normal_sql.include?('events-v4') && normal_sql.include?('protocol-v2') &&
    normal_sql.include?(runtime_row.fetch('releaseId').to_s) && normal_sql.include?(runtime_row.fetch('indexGeneration').to_s)
  abort('publisher still accepts runtime metadata from environment') if
    normal_sql.include?('AOO_SNAPSHOT_SCHEMA_VERSION') || normal_sql.include?('AOO_SOURCE_HASH')
  semantic_hash_regressions = [
    "SHA2(CONCAT(bundle_hash,'|',@source_hash),256)",
    "SHA2('chengdu1001-catalog-local-v1',256)",
    'r.catalog_hash,r.component_hash'
  ]
  inherited = semantic_hash_regressions.select { |fragment| normal_sql.include?(fragment) }
  abort("publisher inherited a semantic release hash: #{inherited.join(', ')}") unless inherited.empty?
  stream_out, _stream_err, stream_status = Open3.capture3(
    { 'AOO_ROOM_RULE_DIAGNOSTIC_STREAM'=>'1' }, 'ruby', publisher, 'diagnostic-stream')
  abort('diagnostic stream SQL contract failed') unless stream_status.success? &&
    stream_out.lines.map(&:strip) == ["SELECT 'ROOM_RULE_STREAM|ONE' marker;", "SELECT 'ROOM_RULE_STREAM|TWO' marker;"]
  diagnostic_workbook = File.join(root, '../Client/docs/开房规则表/跑得快/固始跑得快.xlsx')
  diagnostic_registry = File.join(root, 'release-input/room-rules/固始跑得快.keys.json')
  diagnostic_output = File.join(directory, 'gushi2012.generated.json')
  diagnostic_sql = sql_publisher(publisher, diagnostic_workbook, diagnostic_output, diagnostic_registry,
    identity: identity_source, diagnostic: true)
  diagnostic_markers = %w[ROOM_RULE_DIAGNOSTIC|PRECHECK ROOM_RULE_DIAGNOSTIC|PRECOMMIT ROOM_RULE_DIAGNOSTIC|POSTCOMMIT]
  marker_offsets = diagnostic_markers.map { |marker| diagnostic_sql.index(marker) }
  abort('diagnostic SQL marker order contract failed') unless marker_offsets.all? && marker_offsets == marker_offsets.sort
  abort('diagnostic SQL precheck placement contract failed') unless
    diagnostic_sql.index('SET @legacy_field_aliases=') < marker_offsets.first && marker_offsets.first < diagnostic_sql.index('START TRANSACTION;')
  abort('diagnostic SQL missing candidate or canonical fingerprint contract') unless
    diagnostic_sql.include?('candidate_count') && diagnostic_sql.include?('candidate_precheck') &&
    diagnostic_sql.include?('source_hash_diff') && diagnostic_sql.include?('legacy_field_aliases_diff') &&
    diagnostic_sql.include?('expected_fields_json_type') && diagnostic_sql.include?('expected_fields_octet_length') &&
    diagnostic_sql.include?('expected_validator_fields_sha256') && diagnostic_sql.include?('inserted_ui_fields_sha256') &&
    diagnostic_sql.include?('active_ui_fields_sha256') && diagnostic_sql.scan('CHARACTER SET utf8mb4').length >= 12
  abort('platform required fields did not receive stable keys') unless
    baseline.fetch('fields').any? { |field| field['key'] == 'playerCount' && field['label'] == '人数' } &&
    baseline.fetch('fields').any? { |field| field['key'] == 'roundCount' && field['label'] == '局数' }
  small = baseline.fetch('fields').find { |field| field['label'] == '小结算' }
  abort('official small settlement row did not use generic schema path') unless small &&
    small['control'] == 'checkbox' && small['defaultCandidateIndexes'] == [1] &&
    small['visible'] == true
  hosting = baseline.fetch('fields').find { |field| field['key'] == 'hostingMissThreshold' }
  abort('independent trustee rule was not hidden and server-owned') unless hosting &&
    hosting['label'] == '托管次数' && hosting['control'] == 'radio' &&
    hosting['visible'] == false && hosting['disabled'] == true && hosting['defaultValue'] == 3 &&
    hosting.fetch('options').map { |option| option['value'] } == [3, 4, 5]
  operation = baseline.fetch('fields').find { |field| field['label'] == '操作时间' }
  abort('numeric rule label did not map to its wire value') unless
    operation && operation['key'] == 'operationTimeoutSeconds' &&
    operation['defaultValue'] == 10_000 &&
    operation.fetch('options').first == {
      'value'=>10_000, 'label'=>'10000秒', 'order'=>10, 'disabled'=>false
    }

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
  register_option_values(identity, '临时规则', { '选项甲'=>'temporary_a', '选项乙'=>'temporary_b' })
  register_option_values(identity, '临时单选', {
    '一'=>'temporary_1', '二'=>'temporary_2', '三'=>'temporary_3',
    '四'=>'temporary_4', '五'=>'temporary_5', '不比'=>'temporary_no_compare'
  })

  rewrite_workbook(workbook) do |sheet_data|
    last = sheet_data.elements.to_a.select { |element| element.name == 'row' }.map { |row| row.attributes['r'].to_i }.max
    row = REXML::Element.new('row'); row.add_attribute('r', (last + 1).to_s)
    { 'A'=>'临时规则', 'B'=>'多选', 'C'=>'选项甲｜选项乙', 'D'=>'1', 'E'=>'是' }
      .each { |column, value| row.add_element(inline_cell("#{column}#{last + 1}", value)) }
    sheet_data.add_element(row)
    radio_row = REXML::Element.new('row'); radio_row.add_attribute('r', (last + 2).to_s)
    { 'A'=>'临时单选', 'B'=>'单选', 'C'=>'一｜二｜三｜四｜五｜不比', 'D'=>'6', 'E'=>'是' }
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
  register_option_values(identity, '临时规则', {
    '一'=>'temporary_1', '二'=>'temporary_2', '三'=>'temporary_3',
    '四'=>'temporary_4', '五'=>'temporary_5'
  })
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
    set_cell(row, 'E', '否')
  end
  register_option_values(identity, '临时规则', {
    '新选项甲'=>'temporary_1', '新选项乙'=>'temporary_2', '新选项丙'=>'temporary_3'
  })
  changed = run_publisher(publisher, workbook, output, registry)
  changed_field = changed.fetch('fields').find { |field| field['label'] == '临时规则' }
  abort('field key changed after property edits') unless changed_field.fetch('key') == stable_key
  abort('options/default/visibility did not synchronize') unless
    changed_field.fetch('options').map { |option| option['label'] } == %w[新选项甲 新选项乙 新选项丙] &&
    changed_field['defaultCandidateIndexes'] == [2, 3] && changed_field['visible'] == false

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
