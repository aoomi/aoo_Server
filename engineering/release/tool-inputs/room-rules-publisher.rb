#!/usr/bin/env ruby
require 'json'
require 'digest'
require 'rexml/document'
require 'tmpdir'

SERVER_ROOT = File.expand_path('../../..', __dir__)
PROJECT_ROOT = File.expand_path('..', SERVER_ROOT)
WORKBOOK = File.expand_path(ENV.fetch('AOO_ROOM_RULE_WORKBOOK', File.join(PROJECT_ROOT, 'Client/docs/开房规则表/跑得快/成都跑得快.xlsx')))
OUTPUT = File.expand_path(ENV.fetch('AOO_ROOM_RULE_GENERATED', File.join(SERVER_ROOT, "release-input/room-rules/#{File.basename(WORKBOOK, '.xlsx')}.generated.json")))
REGISTRY = File.expand_path(ENV.fetch('AOO_ROOM_RULE_REGISTRY', OUTPUT.sub(/\.generated\.json\z/, '.keys.json')))
IDENTITY_REGISTRY = File.expand_path(ENV.fetch('AOO_ROOM_RULE_IDENTITY_REGISTRY', File.join(__dir__, '../../../release-input/identity/room-rule-play-identities.json')))
CANONICAL_RUNTIME_METADATA_MANIFEST = File.join(__dir__, '../../../tools/canonical-runtime-metadata-manifest.json')
LEGACY_RUNTIME_METADATA_MANIFEST = File.join(__dir__, '../../../tools/runtime-metadata-canonical-manifest.json')
AUTHORIZED_RUNTIME_METADATA_CONTRACTS = {
  'r15-cd2999-20261012' => {
    'manifestPath' => CANONICAL_RUNTIME_METADATA_MANIFEST,
    'manifestVersion' => 'r50-current-20261011',
    'releaseIssuer' => '/root/lobby_runtime_from_staged_source',
    'releaseEdition' => 'r15-cd2999-20261012',
    'manifestSha256' => 'a453cb986a674f0f6044a581f8e69927a4cfbf94f531567ef021dfaee07a8282'
  },
  # Explicit legacy path only. It is never selected by default; current
  # identity rows must not silently fall back to historical 630 metadata.
  'r9-v4' => {
    'manifestPath' => LEGACY_RUNTIME_METADATA_MANIFEST,
    'manifestVersion' => 'r9-v4',
    'releaseIssuer' => '/root/map_runtime_release_values',
    'releaseEdition' => 'r9-v4',
    'manifestSha256' => '7f26b45d6a3dec5290004367f5db1f83c223b08cbf883f2e0356810531add613'
  }
}.freeze
HEADERS = %w[界面显示文字 控件类型 可选项显示文字 默认勾选 是否显示].freeze
CONTROLS = { '单选' => 'radio', '多选' => 'checkbox' }.freeze
# Platform fields have stable wire types.  Their display labels and values still
# come exclusively from the user workbook, but a newly introduced region must
# not need a second, hand-maintained numeric mapping before a value such as
# `10000秒` can reach Hall and the client.  The field key is the protocol
# identity; the suffix only validates the workbook's human-readable unit.
PLATFORM_NUMERIC_FIELDS = {
  'playerCount' => '人',
  'roundCount' => '局',
  'operationTime' => '秒',
  'operationTimeoutSeconds' => '秒'
}.freeze

def fail!(message)
  raise(message)
end
def xml(entry)
  value = IO.popen(['unzip', '-p', WORKBOOK, entry], err: File::NULL, &:read)
  fail!("Excel 缺少 #{entry}") if value.empty?
  REXML::Document.new(value)
end
def shared_strings
  document = xml('xl/sharedStrings.xml')
  REXML::XPath.match(document, '//*[local-name()="si"]').map do |item|
    REXML::XPath.match(item, './/*[local-name()="t"]').map(&:text).join
  end
rescue StandardError
  []
end
def rows
  strings = shared_strings
  document = xml('xl/worksheets/sheet1.xml')
  result = Hash.new { |hash, key| hash[key] = {} }
  REXML::XPath.each(document, '//*[local-name()="c"]') do |cell|
    reference = cell.attributes['r']; next unless reference
    raw = REXML::XPath.first(cell, './*[local-name()="v"]')&.text.to_s
    value = if cell.attributes['t'] == 's' then strings.fetch(raw.to_i)
            elsif cell.attributes['t'] == 'inlineStr' then REXML::XPath.match(cell, './/*[local-name()="t"]').map(&:text).join
            elsif raw.match?(/^-?\d+(\.\d+)?$/) then raw.include?('.') ? raw.to_f : raw.to_i
            else raw end
    result[reference[/\d+/].to_i][reference[/[A-Z]+/]] = value
  end
  result
end
def boolean!(value, field)
  fail!("#{field} 只能填写是或否") unless %w[是 否].include?(value.to_s.strip)
  value.to_s.strip == '是'
end
def defaults!(raw, count, field, control)
  text = raw.to_s.strip
  return [] if text.empty? || text == '0'
  # A radio cell is one complete 1-based index (for example `6` means only
  # option six). Compact digit expansion is exclusively the workbook's legacy
  # checkbox notation, where `123456` means indexes 1 through 6.
  parts = control == 'radio' ? [text] : text.split(/[,，]/)
  parts = text.chars if control == 'checkbox' && parts.length == 1 &&
    text.match?(/\A[1-9]+\z/) && text.to_i > count
  indexes = parts.map { |value| Integer(value.strip, 10) }
  if control == 'checkbox'
    # 多选沿用历史紧凑写法，表格删减选项后只保留仍存在的索引。
    # 保持首次出现顺序并去重，避免旧默认值阻断整份权威规则发布。
    indexes = indexes.select { |index| index.between?(1, count) }.uniq
  else
    unless indexes.length == 1 && indexes.first.between?(1, count)
      warn("房间规则发布警告: #{field} 单选默认序号无效，已规范化为第1项")
      indexes = [1]
    end
  end
  indexes
rescue ArgumentError
  fail!("#{field} 默认序号必须是1基整数")
end

def resolve_play_entry!(source_identity)
  payload = JSON.parse(File.read(IDENTITY_REGISTRY, encoding: 'UTF-8'))
  matches = payload.fetch('plays').select { |play| play.fetch('sourceIdentity') == source_identity }
  fail!('玩法身份必须在外部技术身份注册表唯一登记') unless matches.length == 1
  play = matches.first
  unless play.fetch('publishingEnabled', true)
    fail!("#{play.fetch('gameCode')} 自动发布已禁用: #{play.fetch('publishingBlockReason', '未满足发布前置契约')}")
  end
  play
rescue Errno::ENOENT, JSON::ParserError, KeyError => error
  fail!("玩法技术身份注册表无效: #{error.message}")
end

def resolve_play_identity!(source_identity)
  resolve_play_entry!(source_identity).reject do |key, _|
    %w[sourceIdentity workbookPath legacyKeyOwnerAliases].include?(key)
  end
end

def published_source_labels!(technical)
  return nil unless technical.key?('publishedSourceLabels')
  labels = technical.fetch('publishedSourceLabels')
  unless labels.is_a?(Array) && !labels.empty? && labels.all? { |label| label.is_a?(String) && !label.strip.empty? }
    fail!('publishedSourceLabels 必须是非空字符串数组')
  end
  fail!('publishedSourceLabels 不得重复') unless labels.uniq.length == labels.length
  required_labels = technical.fetch('requiredRoomRuleFields', []).map { |field| field.fetch('sourceLabel') }
  missing = required_labels - labels
  fail!("publishedSourceLabels 缺少平台必填字段: #{missing.join('、')}") unless missing.empty?
  labels
rescue KeyError => error
  fail!("publishedSourceLabels 身份配置缺少字段: #{error.message}")
end

# The watcher uses this read-only probe to distinguish an Excel rule workbook
# from a documentation/summary workbook. It returns only the exact identity
# cells already present in the sheet; it never derives game codes or versions.
def workbook_source_identity(table = nil)
  fail!("缺少权威 Excel: #{WORKBOOK}") unless File.file?(WORKBOOK)
  table ||= rows
  labels = %w[省份 地区名称 显示名称]
  matches = labels.map do |label|
    table.values.select { |row| row['A'].to_s.strip == label }
  end
  return nil if matches.all?(&:empty?)
  labels.each_with_index do |label, index|
    fail!("玩法身份字段#{label}必须且只能存在一次") unless matches[index].length == 1
  end
  identity = matches.map { |entry| entry.first['B'].to_s.strip }
  fail!('玩法身份字段不得为空') if identity.any?(&:empty?)
  identity
end

def projection_for(technical, label)
  technical.fetch('roomRuleRowProjections', []).find { |entry| entry.fetch('sourceLabel') == label }
end

def assert_projection_source!(projection, control_text, labels, source_label)
  expected_control = projection.fetch('sourceControl')
  fail!("#{source_label} 控件类型变化，拒绝按旧投影发布") unless control_text == expected_control
  expected_labels = projection.fetch('sourceOptions', labels)
  fail!("#{source_label} 可选项变化，必须先更新显式投影") unless labels == expected_labels
end

def project_source_row!(technical, raw)
  projection = projection_for(technical, raw.fetch('label'))
  return [raw] unless projection
  assert_projection_source!(projection, raw.fetch('controlText'), raw.fetch('labels'), raw.fetch('label'))
  case projection.fetch('kind')
  when 'boolean-option'
    fail!("#{raw.fetch('label')} boolean-option 只能包含一个选项") unless raw.fetch('labels').length == 1
    output = projection.fetch('output')
    [raw.merge('label'=>output.fetch('label'), 'key'=>output.fetch('key'),
      'control'=>'checkbox', 'booleanScalar'=>true,
      'optionValues'=>[output.fetch('trueValue', true)])]
  when 'boolean-options'
    outputs = projection.fetch('outputs')
    fail!("#{raw.fetch('label')} boolean-options 子项数量与源选项不一致") unless outputs.length == raw.fetch('labels').length
    outputs.each_with_index.map do |output, index|
      fail!("#{raw.fetch('label')} boolean-options 子项顺序变化，必须先更新显式投影") unless
        output.fetch('sourceOption') == raw.fetch('labels')[index]
      enabled = raw.fetch('candidates').include?(index + 1)
      raw.merge('label'=>output.fetch('label'), 'key'=>output.fetch('key'),
        'control'=>'checkbox', 'labels'=>[output.fetch('sourceOption')],
        'optionValues'=>[output.fetch('trueValue', true)],
        'candidates'=>enabled ? [1] : [], 'booleanScalar'=>true,
        'order'=>output.fetch('order'))
    end
  when 'nested-radio'
    source_default = defaults!(raw.fetch('rawDefault'), raw.fetch('labels').length,
      "#{raw.fetch('label')}/默认勾选", 'radio').first
    projection.fetch('outputs').map do |output|
      options = output.fetch('options')
      candidate = if output['sourceDefaultMap']
        output.fetch('sourceDefaultMap').fetch(source_default.to_s)
      else
        output.fetch('defaultIndex')
      end
      raw.merge('label'=>output.fetch('label'), 'key'=>output.fetch('key'),
        'control'=>'radio', 'labels'=>options.map { |option| option.fetch('label') },
        'optionValues'=>options.map { |option| option.fetch('value') },
        'candidates'=>[candidate], 'order'=>output.fetch('order'))
    end
  when 'number-tuple'
    defaults = raw.fetch('rawDefault').to_s.split(/[|｜]/).map(&:strip)
    outputs = projection.fetch('outputs')
    fail!("#{raw.fetch('label')} 数字默认值数量与子项不一致") unless defaults.length == outputs.length
    outputs.each_with_index.map do |output, index|
      fail!("#{raw.fetch('label')} 数字子项顺序变化，必须先更新显式投影") unless
        raw.fetch('labels')[index] == output.fetch('sourceOption')
      value = Integer(defaults[index], 10)
      raw.merge('label'=>output.fetch('label'), 'key'=>output.fetch('key'),
        'control'=>'number', 'defaultValue'=>value, 'min'=>output.fetch('min'),
        'max'=>output.fetch('max'), 'step'=>output.fetch('step'),
        'labels'=>[], 'candidates'=>[], 'order'=>output.fetch('order'))
    rescue ArgumentError
      fail!("#{raw.fetch('label')}/#{output.fetch('sourceOption')} 默认值必须是整数")
    end
  else
    fail!("#{raw.fetch('label')} 使用未知显式投影 #{projection.fetch('kind')}")
  end
end

def load_registry(technical, registry_path = REGISTRY)
  payload = if File.file?(registry_path)
    JSON.parse(File.read(registry_path, encoding: 'UTF-8'))
  elsif File.file?(OUTPUT)
    previous = JSON.parse(File.read(OUTPUT, encoding: 'UTF-8'))
    { 'version'=>1, 'gameCode'=>technical.fetch('gameCode'), 'nextFieldSequence'=>1,
      'fields'=>previous.fetch('fields', []).each_with_index.map do |field, index|
        { 'key'=>field.fetch('key'), 'label'=>field.fetch('label'), 'position'=>index + 1,
          'active'=>true, 'nextOptionSequence'=>1,
          'options'=>field.fetch('options', []).each_with_index.map do |option, option_index|
            { 'value'=>option.fetch('value'), 'label'=>option.fetch('label'),
              'position'=>option_index + 1, 'active'=>true }
          end }
      end }
  else
    { 'version'=>1, 'gameCode'=>technical.fetch('gameCode'), 'nextFieldSequence'=>1, 'fields'=>[] }
  end
  fail!('稳定键注册表所属玩法不匹配') unless payload['gameCode'] == technical.fetch('gameCode')
  payload['nextFieldSequence'] = [payload.fetch('nextFieldSequence', 1).to_i,
    payload.fetch('fields', []).count { |field| field['key'].to_s.start_with?('rule_') } + 1].max
  payload
rescue JSON::ParserError, KeyError => error
  fail!("稳定键注册表无效: #{error.message}")
end

def allocate_field_key!(registry)
  sequence = registry.fetch('nextFieldSequence')
  registry['nextFieldSequence'] = sequence + 1
  "rule_#{registry.fetch('gameCode').downcase}_#{sequence.to_s.rjust(4, '0')}"
end

def reconcile_entries!(registered, current_labels, create_entry)
  available = registered.each_index.select { |index| registered[index].fetch('active', true) }
  matches = Array.new(current_labels.length)
  current_labels.each_with_index do |label, current_index|
    exact = available.find { |index| registered[index]['label'] == label }
    next unless exact
    matches[current_index] = registered[exact]
    available.delete(exact)
  end
  unmatched = matches.each_index.select { |index| matches[index].nil? }
  # Without a technical key column, a pure rename is identifiable only when the unmatched
  # old/new cardinality is equal. Persisting the assigned key here keeps later text edits
  # independent of Chinese display wording; insertions/deletions allocate/tombstone instead.
  if unmatched.length == available.length
    unmatched.zip(available).each { |current_index, registered_index| matches[current_index] = registered[registered_index] }
    available.clear
  end
  unmatched.each { |index| matches[index] ||= create_entry.call(index) }
  registered.each { |entry| entry['active'] = false }
  matches.each_with_index do |entry, index|
    entry['label'] = current_labels[index]; entry['position'] = index + 1; entry['active'] = true
  end
  matches
end

def numeric_option_value!(technical, field, field_label, option_label)
  specification = technical.fetch('numericRoomRuleFields', []).find do |entry|
    entry.fetch('sourceLabel') == field_label
  end
  platform_suffix = PLATFORM_NUMERIC_FIELDS[field['key']]
  specification ||= { 'suffix'=>platform_suffix } unless platform_suffix.nil?
  return nil unless specification
  suffix = specification.fetch('suffix', '')
  match = option_label.match(/\A(-?\d+)#{Regexp.escape(suffix)}\z/)
  fail!("#{field_label} 的数值选项 #{option_label.inspect} 必须完整匹配整数#{suffix}") unless match
  Integer(match[1], 10)
end

def stable_option_value!(technical, field, field_label, option_label)
  explicit = technical.fetch('roomRuleOptionValues', {}).fetch(field_label, {})
  if explicit.key?(option_label)
    value = explicit.fetch(option_label)
    fail!("#{field_label}/#{option_label} 的显式协议值不能为空") if value.nil? || value == ''
    return value
  end
  numeric = numeric_option_value!(technical, field, field_label, option_label)
  return numeric unless numeric.nil?
  registered = field.fetch('options', []).find do |entry|
    entry.fetch('active', true) && entry['label'] == option_label
  end
  return registered.fetch('value') if registered&.key?('value')
  fail!("#{field_label} 存在未登记显示项 #{option_label.inspect}；请先配置稳定协议值，禁止从显示文字猜值")
end

def reconcile_options!(technical, field, field_label, labels, declared_values = nil)
  field['options'] ||= []
  values = declared_values || labels.map { |label| stable_option_value!(technical, field, field_label, label) }
  identities = values.map { |value| [value.class.name, value] }
  fail!("#{field_label} 的稳定协议值重复") unless identities.uniq.length == identities.length
  options = reconcile_entries!(field['options'], labels, lambda do |index|
    entry = { 'label'=>labels[index], 'position'=>index + 1, 'active'=>true }
    entry['value'] = values[index]
    field['options'] << entry
    entry
  end)
  options.each_with_index do |option, index|
    # The external identity registry is authoritative when present and repairs a
    # polluted persisted registry. Numeric fallback is allowed only for fields
    # explicitly declared numeric and only when the complete label matches.
    option['value'] = values[index]
  end
  options
end

def save_registry!(registry)
  require 'fileutils'
  FileUtils.mkdir_p(File.dirname(REGISTRY))
  temporary = "#{REGISTRY}.tmp-#{Process.pid}"
  File.open(temporary, 'wb', 0o600) do |file|
    file.write(JSON.pretty_generate(registry) + "\n"); file.flush; file.fsync
  end
  File.rename(temporary, REGISTRY)
ensure
  File.delete(temporary) if defined?(temporary) && File.exist?(temporary)
end

def parse!(registry_path = REGISTRY)
  table = rows
  identity = workbook_source_identity(table)
  fail!('权威 Excel 缺少玩法身份三元组') unless identity
  technical = resolve_play_identity!(identity)
  header_lines = table.keys.select do |line|
    ('A'..'E').map { |column| table.fetch(line, {})[column].to_s.strip } == HEADERS
  end
  fail!("必须且只能存在一组五列表头: #{HEADERS.join('、')}") unless header_lines.length == 1
  header_line = header_lines.first
  published_labels = published_source_labels!(technical)
  allowed_labels = published_labels && published_labels.to_h { |label| [label, true] }
  body_rows = table.select do |line, row|
    line > header_line && line <= 10_000 && !row['A'].to_s.strip.empty?
  end.sort_by(&:first)
  if published_labels
    counts = body_rows.each_with_object(Hash.new(0)) do |(_, row), result|
      label = row['A'].to_s.strip
      result[label] += 1 if allowed_labels.key?(label)
    end
    missing = published_labels.select { |label| counts[label].zero? }
    duplicated = published_labels.select { |label| counts[label] > 1 }
    fail!("publishedSourceLabels 对应的工作簿行缺失: #{missing.join('、')}") unless missing.empty?
    fail!("publishedSourceLabels 对应的工作簿行重复: #{duplicated.join('、')}") unless duplicated.empty?
  end
  source_rows = []
  body_rows.each do |line, row|
    label, control_text, option_text = row.values_at('A', 'B', 'C').map { |value| value.to_s.strip }
    next if allowed_labels && !allowed_labels.key?(label)
    projection = projection_for(technical, label)
    if projection
      labels = projection.fetch('sourceOptions')
      fail!("#{label} 可选项变化，必须先更新显式投影") unless option_text == labels.join('｜')
    else
      labels = option_text.split(/[|｜]/).map(&:strip)
    end
    fail!("#{label} 存在空选项") if labels.empty? || labels.any?(&:empty?)
    visible = boolean!(row['E'], "#{label}/是否显示")
    control = CONTROLS[control_text]
    fail!("#{label} 使用未知控件 #{control_text}") unless control || projection
    candidates = control ? defaults!(row['D'], labels.length,
      "#{File.basename(WORKBOOK)}!sheet1 第#{line}行 #{label}/默认勾选(D#{line})" \
      "（控件类型=#{control}，可选项数=#{labels.length}，默认原值=#{row['D'].to_s.strip.inspect}）",
      control) : []
    raw = { 'line'=>line, 'label'=>label, 'controlText'=>control_text, 'control'=>control,
      'labels'=>labels, 'visible'=>visible, 'candidates'=>candidates,
      'rawDefault'=>row['D'], 'order'=>(line - 1) * 10 }
    source_rows.concat(project_source_row!(technical, raw))
  end
  fail!('权威 Excel 没有可发布规则') if source_rows.empty?
  registry = load_registry(technical, registry_path)
  registry['fields'] ||= []
  required_fields = technical.fetch('requiredRoomRuleFields', [])
  registered_fields = reconcile_entries!(registry['fields'], source_rows.map { |row| row.fetch('label') }, lambda do |index|
    label = source_rows[index].fetch('label')
    required = required_fields.find do |field|
      field.fetch('sourceLabel') == label || field.fetch('key') == source_rows[index]['key']
    end
    existing = required && registry['fields'].find { |field| field['key'] == required.fetch('key') }
    next existing if existing
    entry = { 'key'=>required ? required.fetch('key') : allocate_field_key!(registry), 'label'=>label,
      'position'=>index + 1, 'active'=>true, 'nextOptionSequence'=>1, 'options'=>[] }
    registry['fields'] << entry
    entry
  end)
  fields = source_rows.each_with_index.map do |source, index|
    registered = registered_fields[index]
    control = source.fetch('control'); candidates = source.fetch('candidates')
    if control == 'number'
      registered['options'] ||= []
      registered['options'].each { |option| option['active'] = false }
      next({ 'key'=>registered.fetch('key'), 'label'=>source.fetch('label'), 'control'=>'number',
        'order'=>source.fetch('order'), 'visible'=>source.fetch('visible'), 'disabled'=>false,
        'required'=>true, 'defaultValue'=>source.fetch('defaultValue'), 'min'=>source.fetch('min'),
        'max'=>source.fetch('max'), 'step'=>source.fetch('step'), 'options'=>[] })
    end
    options = reconcile_options!(technical, registered, source.fetch('label'), source.fetch('labels'),
      source['optionValues'])
    values = options.map { |option| option.fetch('value') }
    default_value = if control == 'radio'
      candidates.empty? ? nil : values[candidates.first - 1]
    elsif source['booleanScalar']
      candidates.include?(1)
    else
      candidates.map { |index| values[index - 1] }
    end
    fail!("#{source.fetch('label')} 单选必须且只能配置一个默认项") if control == 'radio' && candidates.length != 1
    required = required_fields.find { |field| field.fetch('key') == registered.fetch('key') }
    server_owned = required&.fetch('serverOwned', false) == true
    fail!("#{source.fetch('label')} 服务端托管字段必须隐藏") if server_owned && source.fetch('visible')
    { 'key'=>registered.fetch('key'), 'label'=>source.fetch('label'), 'control'=>control,
      'order'=>required&.fetch('order', source.fetch('order')) || source.fetch('order'),
      'visible'=>source.fetch('visible'),
      'disabled'=>server_owned, 'required'=>control == 'radio',
      'defaultCandidateIndexes'=>candidates, 'defaultValue'=>default_value,
      'options'=>source.fetch('labels').each_index.map { |option_index| { 'value'=>values[option_index],
        'label'=>source.fetch('labels')[option_index], 'order'=>(option_index + 1) * 10, 'disabled'=>false } } }
  end
  fail!('稳定字段键重复') unless fields.map { |field| field['key'] }.uniq.length == fields.length
  active_keys = fields.map { |field| field.fetch('key') }
  missing_required_keys = required_fields.map { |field| field.fetch('key') }.reject { |key| active_keys.include?(key) }
  unless missing_required_keys.empty?
    descriptions = missing_required_keys.map do |key|
      registered = registry.fetch('fields', []).find { |field| field['key'] == key }
      registered ? "#{key}（历史显示名=#{registered['label']}）" : key
    end
    fail!("#{File.basename(WORKBOOK)} 权威 Excel 缺少平台必填规则字段: #{descriptions.join('、')}")
  end
  [technical, fields, registry]
end

def canonical_legacy_field_aliases(technical)
  aliases = technical.fetch('legacyFieldKeyAliases', {})
  fail!('legacyFieldKeyAliases 必须为对象') unless aliases.is_a?(Hash)
  fail!('legacyFieldKeyAliases 不得重复目标键') unless aliases.values.uniq.length == aliases.length
  aliases.each do |legacy_key, canonical_key|
    unless legacy_key.is_a?(String) && !legacy_key.strip.empty? &&
        canonical_key.is_a?(String) && !canonical_key.strip.empty? && legacy_key != canonical_key
      fail!('legacyFieldKeyAliases 必须是不同的非空稳定键映射')
    end
  end
  aliases
end

def schema_legacy_field_aliases(technical)
  canonical_legacy_field_aliases(technical).to_h do |legacy_key, canonical_key|
    [legacy_key, { 'gameId'=>technical.fetch('gameId'),
      'gameCode'=>technical.fetch('gameCode'), 'playVersion'=>technical.fetch('playVersion'),
      'canonicalKey'=>canonical_key }]
  end
end

def migrate_key_owner!
  source_identity = workbook_source_identity
  fail!('权威 Excel 缺少玩法身份三元组') unless source_identity
  entry = resolve_play_entry!(source_identity)
  registered_workbook = File.expand_path(entry.fetch('workbookPath'), PROJECT_ROOT)
  fail!('当前工作簿路径与身份注册表 workbookPath 不一致') unless WORKBOOK == registered_workbook
  technical = entry.reject { |key, _| %w[sourceIdentity workbookPath legacyKeyOwnerAliases].include?(key) }
  aliases = entry.fetch('legacyKeyOwnerAliases', [])
  unless aliases.is_a?(Array) && aliases.all? { |owner| owner.is_a?(String) && !owner.strip.empty? } &&
      aliases.uniq.length == aliases.length
    fail!('legacyKeyOwnerAliases 必须为唯一、非空字符串数组')
  end
  fail!('稳定键注册表缺失，拒绝 owner migration') unless File.file?(REGISTRY)
  payload = JSON.parse(File.read(REGISTRY, encoding: 'UTF-8'))
  fail!('稳定键注册表必须是对象') unless payload.is_a?(Hash)
  owner = payload['gameCode']
  canonical = technical.fetch('gameCode')
  fail!('稳定键注册表 owner 不在该玩法明确迁移白名单中') unless owner == canonical || aliases.count(owner) == 1

  field_aliases = canonical_legacy_field_aliases(technical)
  migrated = Marshal.load(Marshal.dump(payload))
  migrated['gameCode'] = canonical
  migrated_fields = migrated.fetch('fields', [])
  fail!('稳定键注册表 fields 必须是数组') unless migrated_fields.is_a?(Array)
  field_aliases.each do |legacy_key, canonical_key|
    legacy_matches = migrated_fields.select { |field| field['key'] == legacy_key }
    canonical_matches = migrated_fields.select { |field| field['key'] == canonical_key }
    fail!("稳定键字段 #{legacy_key} 重复，拒绝迁移") if legacy_matches.length > 1
    fail!("稳定键字段 #{canonical_key} 已存在，拒绝与历史字段冲突") if
      !legacy_matches.empty? && !canonical_matches.empty?
    legacy_matches.each { |field| field['key'] = canonical_key }
  end
  expected = Marshal.load(Marshal.dump(payload))
  expected['gameCode'] = canonical
  expected.fetch('fields', []).each do |field|
    field_aliases.each { |legacy_key, canonical_key| field['key'] = canonical_key if field['key'] == legacy_key }
  end
  fail!('迁移只能改 owner 与白名单字段 key，稳定值/选项/顺序不得变化') unless migrated == expected
  if migrated == payload
    parse!(REGISTRY)
    return { 'status'=>'already-canonical', 'gameId'=>technical.fetch('gameId'),
      'gameCode'=>canonical, 'playVersion'=>technical.fetch('playVersion') }
  end

  # Run the complete ordinary parser against a temporary canonicalized registry
  # before writing. This ensures a key-owner rename cannot bless an invalid
  # workbook or a conflicting field/option schema.
  Dir.mktmpdir('aoo-room-rule-key-owner-validate') do |directory|
    candidate = File.join(directory, 'candidate.keys.json')
    File.write(candidate, JSON.pretty_generate(migrated) + "\n", mode: 'wb', perm: 0o600)
    validated_technical, = parse!(candidate)
    fail!('迁移候选身份在标准解析中发生变化') unless
      validated_technical.values_at('gameId', 'gameCode', 'playVersion') ==
      technical.values_at('gameId', 'gameCode', 'playVersion')
  end

  save_registry!(migrated)
  { 'status'=>'migrated', 'gameId'=>technical.fetch('gameId'),
    'gameCode'=>canonical, 'previousOwner'=>owner,
    'migratedFieldKeys'=>field_aliases.select { |legacy_key, _| payload.fetch('fields', []).any? { |field| field['key'] == legacy_key } },
    'playVersion'=>technical.fetch('playVersion') }
end

def quote(value)
  "'#{value.to_s.gsub("'", "''")}'"
end

def explicit_publish_runtime_contract!(edition, contract_path, contract_sha)
  fail!('runtime contract path and SHA must be supplied together') unless
    contract_path && contract_sha && contract_sha.match?(/\A[0-9a-f]{64}\z/)
  fail!('runtime contract must be an explicit absolute file') unless
    contract_path.start_with?('/') && File.file?(contract_path)
  bytes = File.binread(contract_path)
  fail!('runtime contract SHA mismatch') unless Digest::SHA256.hexdigest(bytes) == contract_sha
  contract = JSON.parse(bytes)
  expected_keys = %w[contractKind schemaVersion manifestPath manifestVersion releaseIssuer releaseEdition manifestSha256]
  fail!('runtime contract field set mismatch') unless contract.keys.sort == expected_keys.sort
  fail!('runtime contract kind mismatch') unless
    contract['contractKind'] == 'approved-room-rule-runtime-contract' && contract['schemaVersion'] == 1
  fail!('runtime contract edition mismatch') unless contract['releaseEdition'] == edition
  %w[manifestPath manifestVersion releaseIssuer releaseEdition manifestSha256].each do |key|
    fail!('runtime contract field missing') unless contract[key].is_a?(String) && !contract[key].strip.empty?
  end
  fail!('runtime contract manifest path must be absolute') unless contract['manifestPath'].start_with?('/')
  fail!('runtime contract manifest SHA invalid') unless contract['manifestSha256'].match?(/\A[0-9a-f]{64}\z/)
  contract
end

def required_publish_runtime_metadata!(technical, edition = 'r15-cd2999-20261012', contract_path = nil, contract_sha = nil)
  contract = if contract_path || contract_sha
    explicit_publish_runtime_contract!(edition, contract_path, contract_sha)
  else
    AUTHORIZED_RUNTIME_METADATA_CONTRACTS.fetch(edition) do
      fail!("canonical runtime metadata release edition is not authorized: #{edition}")
    end
  end
  manifest_path = contract.fetch('manifestPath')
  fail!("canonical runtime metadata manifest missing: #{manifest_path}") unless File.file?(manifest_path)
  manifest_sha = Digest::SHA256.file(manifest_path).hexdigest
  fail!('canonical runtime metadata manifest source hash is not authorized') unless
    manifest_sha == contract.fetch('manifestSha256')
  payload = JSON.parse(File.read(manifest_path, encoding: 'UTF-8'))
  fail!('canonical runtime metadata manifest kind is not canonical') unless
    payload['manifestKind'] == 'runtime-metadata-canonical'
  fail!('canonical runtime metadata manifest version is not authorized') unless
    payload['manifestVersion'] == contract.fetch('manifestVersion')
  fail!('canonical runtime metadata release issuer is not authorized') unless
    payload['releaseIssuer'] == contract.fetch('releaseIssuer')
  fail!('canonical runtime metadata release edition is not authorized') unless
    payload['releaseEdition'] == contract.fetch('releaseEdition')
  required_fields = contract.fetch('manifestVersion') == 'r9-v4' ?
    %w[snapshotSchemaVersion cardCodecVersion eventInterpreterVersion protocolVersion sourceHash releaseId indexGeneration] :
    %w[snapshotSchemaVersion cardCodecVersion eventInterpreterVersion protocolVersion contractVersion sourceHash releaseId indexGeneration]
  fail!('canonical runtime metadata required field contract mismatch') unless
    payload['requiredFields'] == required_fields
  row = payload.fetch('rows').find do |candidate|
    candidate['gameId'] == technical.fetch('gameId') &&
      candidate['gameCode'] == technical.fetch('gameCode') &&
      candidate['playVersion'] == technical.fetch('playVersion') &&
      candidate['regionCode'] == technical.fetch('regionCode')
  end
  fail!("canonical runtime metadata row missing for #{technical.fetch('gameCode')}") unless row
  fail!("canonical runtime metadata row is not consumable for #{technical.fetch('gameCode')}") unless
    payload['manifestStatus'] == 'SIGNED_CANONICAL_INPUT' && payload['consumable'] == true && row['admission'] == 'ACCEPTED'
  %w[snapshotSchemaVersion cardCodecVersion eventInterpreterVersion protocolVersion].each do |field|
    value = row[field].to_s
    fail!("canonical runtime metadata #{field} missing for #{technical.fetch('gameCode')}") unless
      value.match?(/\A[A-Za-z0-9][A-Za-z0-9._-]{0,63}\z/)
  end
  source_hash = row['sourceHash'].to_s
  fail!("canonical runtime metadata sourceHash invalid for #{technical.fetch('gameCode')}") unless source_hash.match?(/\A[0-9a-f]{64}\z/)
  %w[releaseId indexGeneration].each do |field|
    value = row[field]
    fail!("canonical runtime metadata #{field} invalid for #{technical.fetch('gameCode')}") unless value.is_a?(Integer) && value.positive?
  end
  row.slice('snapshotSchemaVersion', 'cardCodecVersion', 'eventInterpreterVersion', 'protocolVersion', 'contractVersion',
            'sourceHash', 'releaseId', 'indexGeneration')
rescue JSON::ParserError, KeyError => error
  fail!("canonical runtime metadata manifest invalid: #{error.message}")
end

def local_transaction_diagnostic?(technical)
  enabled = ENV['AOO_ROOM_RULE_DIAGNOSTIC'].to_s == '1'
  return false unless enabled
  fail!('transaction diagnostic is restricted to gameId=2012') unless technical.fetch('gameId') == 2012
  true
end

def diagnostic_stream_sql
  fail!('diagnostic stream requires explicit opt-in') unless ENV['AOO_ROOM_RULE_DIAGNOSTIC_STREAM'].to_s == '1'
  <<~SQL
    SELECT 'ROOM_RULE_STREAM|ONE' marker;
    SELECT 'ROOM_RULE_STREAM|TWO' marker;
  SQL
end

def diagnostic_json_fingerprint(expression, prefix)
  # Every diagnostic point uses this same explicit utf8mb4 JSON text form.
  canonical = "CAST(#{expression} AS CHAR CHARACTER SET utf8mb4)"
  <<~SQL.strip
    JSON_TYPE(#{expression}) #{prefix}_json_type,
           CHAR_LENGTH(#{canonical}) #{prefix}_char_length,
           OCTET_LENGTH(#{canonical}) #{prefix}_octet_length,
           SHA2(#{canonical},256) #{prefix}_sha256
  SQL
end

def content_hash_sql(expression)
  "SHA2(CAST(#{expression} AS CHAR CHARACTER SET utf8mb4),256)"
end

def canonical_bundle_snapshot_sql(catalog_snapshot, validator, ui_schema, component_snapshot, component_chain)
  "JSON_OBJECT('catalogSnapshot',JSON_EXTRACT(#{catalog_snapshot},'$')," \
    "'ruleValidator',JSON_EXTRACT(#{validator},'$'),'uiSchema',JSON_EXTRACT(#{ui_schema},'$')," \
    "'componentSnapshot',JSON_EXTRACT(#{component_snapshot},'$')," \
    "'componentChain',JSON_EXTRACT(#{component_chain},'$'))"
end

def publish_sql(technical, fields, runtime_metadata)
  snapshot_schema_version = runtime_metadata.fetch('snapshotSchemaVersion')
  source_hash = runtime_metadata.fetch('sourceHash')
  release_id = runtime_metadata.fetch('releaseId')
  index_generation = runtime_metadata.fetch('indexGeneration')
  display_name = technical.fetch('displayName', File.basename(WORKBOOK, '.xlsx'))
  diagnostic = local_transaction_diagnostic?(technical)
  ui_fields = JSON.generate(fields)
  # 隐藏且禁用的服务端字段仍进入 validator：Hall 使用表内默认项规范化房间快照，
  # 客户端既不展示也不能提交覆盖。普通隐藏字段不会因此成为第二套服务端状态源。
  # Hidden workbook fields remain part of the authoritative room contract when
  # the identity marks them as required (roundCount/paymentMode are commonly
  # hidden from the UI but still drive billing and snapshot validation). UI
  # visibility must not decide whether Hall can validate or bill a room.
  required_keys = technical.fetch('requiredRoomRuleFields', []).map { |field| field.fetch('key') }
  validator_fields = fields.select { |field| field['visible'] || field['disabled'] || required_keys.include?(field['key']) }.map do |field|
    field.reject { |key, _| %w[label visible defaultCandidateIndexes].include?(key) }
  end
  # payerMode 是平台计费治理字段，不属于用户维护的玩法 Excel，也不应显示在规则界面。
  # 服务端发布器提供唯一默认值，保证客户端无法伪造付费策略且计费 Saga 始终获得确定输入。
  validator_fields << { 'key'=>'payerMode', 'control'=>'radio', 'order'=>10_000,
    'disabled'=>false, 'required'=>true, 'defaultValue'=>'OWNER',
    'options'=>[{ 'value'=>'OWNER', 'order'=>10, 'disabled'=>false }] }
  validator_fields = JSON.generate(validator_fields)
  legacy_aliases = JSON.generate(schema_legacy_field_aliases(technical))
  # The persisted release hashes are content commitments. `source_hash` remains
  # the immutable workbook identity; it is deliberately not reused as a JSON
  # snapshot hash. The bundle envelope gives release and compiled index one
  # common canonical content source without inheriting a semantic version hash.
  candidate_disjuncts = {
    'source_hash_diff'=>"JSON_UNQUOTE(JSON_EXTRACT(p.old_ui,'$.roomRuleSourceHash'))<>@source_hash",
    'source_hash_missing'=>"JSON_EXTRACT(p.old_ui,'$.roomRuleSourceHash') IS NULL",
    'ui_fields_diff'=>"CAST(JSON_EXTRACT(p.old_ui,'$.fields') AS CHAR CHARACTER SET utf8mb4)<>CAST(JSON_EXTRACT(@fields,'$') AS CHAR CHARACTER SET utf8mb4)",
    'validator_fields_diff'=>"CAST(JSON_EXTRACT(p.old_validator,'$.fields') AS CHAR CHARACTER SET utf8mb4)<>CAST(JSON_EXTRACT(@validator_fields,'$') AS CHAR CHARACTER SET utf8mb4)",
    'validator_game_id_missing'=>"JSON_EXTRACT(p.old_validator,'$.gameId') IS NULL",
    'validator_game_id_diff'=>"JSON_EXTRACT(p.old_validator,'$.gameId')<>#{technical['gameId']}",
    'validator_game_code_diff'=>"JSON_UNQUOTE(JSON_EXTRACT(p.old_validator,'$.gameCode'))<>#{quote(technical['gameCode'])}",
    'validator_play_version_diff'=>"JSON_UNQUOTE(JSON_EXTRACT(p.old_validator,'$.playVersion'))<>#{quote(technical['playVersion'])}",
    'legacy_field_aliases_diff'=>"CAST(JSON_EXTRACT(p.old_validator,'$.legacyFieldAliases') AS CHAR CHARACTER SET utf8mb4)<>CAST(JSON_EXTRACT(@legacy_field_aliases,'$') AS CHAR CHARACTER SET utf8mb4)",
    'catalog_hash_missing'=>"p.existing_catalog_hash IS NULL",
    'catalog_hash_diff'=>"p.existing_catalog_hash<>p.catalog_hash",
    'rule_hash_missing'=>"p.existing_rule_hash IS NULL",
    'rule_hash_diff'=>"p.existing_rule_hash<>p.rule_hash",
    'component_hash_missing'=>"p.existing_component_hash IS NULL",
    'component_hash_diff'=>"p.existing_component_hash<>p.component_hash",
    'release_bundle_hash_missing'=>"p.existing_bundle_hash IS NULL",
    'release_bundle_hash_diff'=>"p.existing_bundle_hash<>p.bundle_hash",
    'index_bundle_hash_missing'=>"p.existing_index_bundle_hash IS NULL",
    'index_bundle_hash_diff'=>"p.existing_index_bundle_hash<>p.bundle_hash",
    'release_index_bundle_hash_diff'=>"p.existing_bundle_hash<>p.existing_index_bundle_hash"
  }
  candidate_predicate = candidate_disjuncts.values.join("\n          OR ")
  candidate_source = <<~SQL
      FROM aoo_compiled_index_active a JOIN aoo_compiled_room_create_index i
        ON i.game_id=a.game_id AND i.region_code=a.region_code AND i.play_version=a.play_version AND i.index_generation=a.index_generation
      JOIN aoo_game_release r ON r.release_id=a.release_id
      WHERE a.game_id=#{technical['gameId']} AND a.play_version=#{quote(technical['playVersion'])}
  SQL
  publish_base_select = <<~SQL
      SELECT a.game_id,a.region_code,a.play_version,a.index_generation old_generation,a.release_id old_release_id,
             i.component_chain,i.rule_validator old_validator,i.ui_schema old_ui,
             i.bundle_hash existing_index_bundle_hash,
             r.catalog_snapshot,r.component_snapshot,r.catalog_hash existing_catalog_hash,
             r.rule_hash existing_rule_hash,r.ui_hash existing_ui_hash,
             r.component_hash existing_component_hash,r.bundle_hash existing_bundle_hash,
             JSON_OBJECT('source',#{quote(technical['gameCode'] + '-room-rule-workbook')},'sourceHash',@source_hash,
                         'gameId',#{technical['gameId']},'gameCode',#{quote(technical['gameCode'])},
                         'playVersion',a.play_version,'legacyFieldAliases',JSON_EXTRACT(@legacy_field_aliases,'$'),
                         'fields',JSON_EXTRACT(@validator_fields,'$')) validator,
             JSON_SET(i.ui_schema,'$.fields',JSON_EXTRACT(@fields,'$'),'$.roomRuleSourceHash',@source_hash) ui_schema,
             #{quote(snapshot_schema_version)} snapshot_schema_version,
             #{quote(runtime_metadata.fetch('cardCodecVersion'))} card_codec_version,
             #{quote(runtime_metadata.fetch('eventInterpreterVersion'))} event_interpreter_version,
             #{quote(runtime_metadata.fetch('protocolVersion'))} protocol_version,
             #{quote(source_hash)} source_hash,
             #{index_generation} new_generation,
             #{release_id} release_id,
             #{release_id} release_version
    #{candidate_source.rstrip}
  SQL
  bundle_snapshot = canonical_bundle_snapshot_sql('p.catalog_snapshot', 'p.validator', 'p.ui_schema',
    'p.component_snapshot', 'p.component_chain')
  content_hash_projection = <<~SQL
      SELECT p.*,
             #{content_hash_sql('p.catalog_snapshot')} catalog_hash,
             #{content_hash_sql('p.validator')} rule_hash,
             #{content_hash_sql('p.component_chain')} component_hash,
             #{content_hash_sql(bundle_snapshot)} bundle_hash
        FROM (#{publish_base_select.rstrip}) p
  SQL
  room_rule_publish_select = <<~SQL
      SELECT p.*
        FROM (#{content_hash_projection.rstrip}) p
       WHERE (#{candidate_predicate})
  SQL
  session_fields = <<~SQL
    SET @source_hash=#{quote(source_hash)};
    SET @fields=#{quote(ui_fields)};
    SET @validator_fields=#{quote(validator_fields)};
    SET @legacy_field_aliases=#{quote(legacy_aliases)};
  SQL
  precheck_diagnostic = diagnostic ? <<~SQL : ''
    SELECT 'ROOM_RULE_DIAGNOSTIC|PRECHECK' marker,DATABASE() schema_name,@@hostname host_name,
           p.game_id,p.region_code,p.play_version,
           #{diagnostic_json_fingerprint("JSON_EXTRACT(@fields,'$')", 'expected_fields')},
           #{diagnostic_json_fingerprint("JSON_EXTRACT(@validator_fields,'$')", 'expected_validator_fields')},
           #{diagnostic_json_fingerprint("JSON_EXTRACT(p.old_ui,'$.fields')", 'active_ui_fields')},
           #{diagnostic_json_fingerprint("JSON_EXTRACT(p.old_validator,'$.fields')", 'active_validator_fields')},
           #{candidate_disjuncts.map { |name, expression| "(#{expression}) #{name}" }.join(",\n           ")},
           (SELECT COUNT(*) FROM (#{room_rule_publish_select.rstrip}) candidate_precheck) candidate_count
      FROM (#{content_hash_projection.rstrip}) p;
  SQL
  precommit_diagnostic = diagnostic ? <<~SQL : ''
    SELECT 'ROOM_RULE_DIAGNOSTIC|PRECOMMIT' marker,DATABASE() schema_name,@@hostname host_name,
           p.game_id,p.region_code,p.play_version,p.new_generation,p.release_id,
           #{diagnostic_json_fingerprint("JSON_EXTRACT(@fields,'$')", 'expected_fields')},
           #{diagnostic_json_fingerprint("JSON_EXTRACT(@validator_fields,'$')", 'expected_validator_fields')},
           #{diagnostic_json_fingerprint("JSON_EXTRACT(i.ui_schema,'$.fields')", 'inserted_ui_fields')},
           #{diagnostic_json_fingerprint("JSON_EXTRACT(i.rule_validator,'$.fields')", 'inserted_validator_fields')}
      FROM room_rule_publish p JOIN aoo_compiled_room_create_index i
        ON i.game_id=p.game_id AND i.region_code=p.region_code AND i.play_version=p.play_version
       AND i.index_generation=p.new_generation;
  SQL
  postcommit_diagnostic = diagnostic ? <<~SQL : ''
    SELECT 'ROOM_RULE_DIAGNOSTIC|POSTCOMMIT' marker,DATABASE() schema_name,@@hostname host_name,
           a.game_id,a.region_code,a.play_version,a.index_generation,a.release_id,a.cache_epoch,
           #{diagnostic_json_fingerprint("JSON_EXTRACT(@fields,'$')", 'expected_fields')},
           #{diagnostic_json_fingerprint("JSON_EXTRACT(@validator_fields,'$')", 'expected_validator_fields')},
           #{diagnostic_json_fingerprint("JSON_EXTRACT(i.ui_schema,'$.fields')", 'active_ui_fields')},
           #{diagnostic_json_fingerprint("JSON_EXTRACT(i.rule_validator,'$.fields')", 'active_validator_fields')}
      FROM aoo_compiled_index_active a JOIN aoo_compiled_room_create_index i
        ON i.game_id=a.game_id AND i.region_code=a.region_code AND i.play_version=a.play_version
       AND i.index_generation=a.index_generation
     WHERE a.game_id=#{technical['gameId']} AND a.play_version=#{quote(technical['playVersion'])};
  SQL
  transaction_preamble = diagnostic ? "#{session_fields}\n#{precheck_diagnostic}START TRANSACTION;\n" : "START TRANSACTION;\n#{session_fields}"
  <<~SQL
    #{transaction_preamble}
    -- 本地 seed 与规则发布必须收敛到同一可读目录身份。客户端只展示 catalog 的
    -- displayName，不能为了修 UI 再引入 gameId/code 到中文名称的平行映射。
    UPDATE aoo_game_catalog
       SET display_name=#{quote(display_name)}, family_code=#{quote(technical['family'])},
           row_version=row_version+1
     WHERE game_id=#{technical['gameId']}
       AND (display_name<>#{quote(display_name)} OR family_code<>#{quote(technical['family'])});
    DROP TEMPORARY TABLE IF EXISTS room_rule_publish;
    CREATE TEMPORARY TABLE room_rule_publish AS
    #{room_rule_publish_select};
    INSERT INTO aoo_game_release(release_id,game_id,play_version,snapshot_schema_version,card_codec_version,event_interpreter_version,protocol_version,source_hash,release_version,release_scope,catalog_snapshot,rule_snapshot,ui_snapshot,component_snapshot,catalog_hash,rule_hash,ui_hash,component_hash,bundle_hash,status,rollout_percent,created_by,reason,validated_at,activated_at)
      SELECT release_id,game_id,play_version,snapshot_schema_version,card_codec_version,event_interpreter_version,protocol_version,source_hash,release_version,#{quote(technical.fetch('releaseScope'))},catalog_snapshot,JSON_OBJECT('roomRuleSchema',JSON_EXTRACT(validator,'$')),JSON_OBJECT('roomRuleSourceHash',@source_hash),component_snapshot,catalog_hash,rule_hash,@source_hash,component_hash,bundle_hash,'ACTIVE',100,1,#{quote("Publish #{technical['gameCode']} canonical runtime metadata")},CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3) FROM room_rule_publish;
    INSERT INTO aoo_game_release_region(release_id,region_code,rollout_percent,status) SELECT release_id,region_code,100,'ACTIVE' FROM room_rule_publish;
    INSERT INTO aoo_compiled_room_create_index(game_id,region_code,play_version,index_generation,release_id,component_chain,rule_validator,ui_schema,lookup_hash,bundle_hash,lifecycle_state,validated_at,activated_at)
      SELECT game_id,region_code,play_version,new_generation,release_id,component_chain,JSON_EXTRACT(validator,'$'),ui_schema,SHA2(CONCAT(game_id,'|',region_code,'|',play_version,'|',@source_hash),256),bundle_hash,'ACTIVE',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3) FROM room_rule_publish;
    #{precommit_diagnostic}
    UPDATE aoo_compiled_index_active a JOIN room_rule_publish p ON p.game_id=a.game_id AND p.region_code=a.region_code SET a.index_generation=p.new_generation,a.release_id=p.release_id,a.cache_epoch=a.cache_epoch+1,a.activation_reason=#{quote(technical['gameCode'] + ' room rules publication')},a.activated_at=CURRENT_TIMESTAMP(3);
    UPDATE aoo_compiled_room_create_index i JOIN room_rule_publish p ON p.game_id=i.game_id AND p.region_code=i.region_code AND p.old_generation=i.index_generation SET i.lifecycle_state='RETIRED',i.retired_at=CURRENT_TIMESTAMP(3);
    UPDATE aoo_game_release r JOIN room_rule_publish p ON p.old_release_id=r.release_id SET r.status='RETIRED',r.retired_at=CURRENT_TIMESTAMP(3);
    SELECT game_id,region_code,play_version,new_generation AS index_generation,release_id FROM room_rule_publish;
    COMMIT;
    #{postcommit_diagnostic}
  SQL
end

def rollback_sql(release_id)
  fail!('rollback releaseId 必须是正整数') unless release_id.to_s.match?(/\A[1-9][0-9]*\z/)
  <<~SQL
    START TRANSACTION;
    SET @target_release=#{release_id};
    DROP TEMPORARY TABLE IF EXISTS chengdu_pdk_rollback;
    CREATE TEMPORARY TABLE chengdu_pdk_rollback AS
      SELECT a.game_id,a.region_code,a.play_version,a.index_generation target_generation,
             (SELECT MAX(i.index_generation) FROM aoo_compiled_room_create_index i
               WHERE i.game_id=a.game_id AND i.region_code=a.region_code
                 AND i.play_version=a.play_version AND i.release_id<>@target_release) previous_generation
        FROM aoo_compiled_index_active a
       WHERE a.release_id=@target_release;
    DELIMITER //
    CREATE PROCEDURE assert_chengdu_pdk_rollback()
    BEGIN
      IF NOT EXISTS (SELECT 1 FROM chengdu_pdk_rollback) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='rollback target is not active';
      END IF;
      IF EXISTS (SELECT 1 FROM chengdu_pdk_rollback WHERE previous_generation IS NULL) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='rollback has no previous generation';
      END IF;
    END//
    DELIMITER ;
    CALL assert_chengdu_pdk_rollback();
    DROP PROCEDURE assert_chengdu_pdk_rollback;
    UPDATE aoo_compiled_index_active a JOIN chengdu_pdk_rollback r
       ON r.game_id=a.game_id AND r.region_code=a.region_code AND r.play_version=a.play_version
       SET a.index_generation=r.previous_generation,
           a.release_id=(SELECT i.release_id FROM aoo_compiled_room_create_index i
                          WHERE i.game_id=r.game_id AND i.region_code=r.region_code
                            AND i.play_version=r.play_version AND i.index_generation=r.previous_generation),
           a.cache_epoch=a.cache_epoch+1,a.activation_reason='Rollback Chengdu PDK room rules',
           a.activated_at=CURRENT_TIMESTAMP(3);
    UPDATE aoo_compiled_room_create_index i JOIN chengdu_pdk_rollback r
       ON i.game_id=r.game_id AND i.region_code=r.region_code AND i.play_version=r.play_version
       SET i.lifecycle_state=IF(i.index_generation=r.previous_generation,'ACTIVE','RETIRED'),
           i.activated_at=IF(i.index_generation=r.previous_generation,CURRENT_TIMESTAMP(3),i.activated_at),
           i.retired_at=IF(i.index_generation=r.target_generation,CURRENT_TIMESTAMP(3),i.retired_at)
     WHERE i.index_generation IN (r.previous_generation,r.target_generation);
    UPDATE aoo_game_release SET status='RETIRED',retired_at=CURRENT_TIMESTAMP(3)
     WHERE release_id=@target_release;
    UPDATE aoo_game_release g JOIN chengdu_pdk_rollback r
       ON g.release_id=(SELECT i.release_id FROM aoo_compiled_room_create_index i
                         WHERE i.game_id=r.game_id AND i.region_code=r.region_code
                           AND i.play_version=r.play_version AND i.index_generation=r.previous_generation)
       SET g.status='ACTIVE',g.retired_at=NULL,g.activated_at=CURRENT_TIMESTAMP(3);
    COMMIT;
  SQL
end

begin
  command = ARGV[0] || 'validate'
  if command == 'source-identity'
    puts JSON.generate(workbook_source_identity)
    exit 0
  end
  if command == 'migrate-key-owner'
    puts JSON.generate(migrate_key_owner!)
    exit 0
  end
  technical, fields, registry = parse!
  fail!('explicit runtime contract arguments are restricted to sql') if command != 'sql' && ARGV.length > 2
  fail!('sql accepts edition and one explicit contract path/SHA pair') if command == 'sql' && ![1, 2, 4].include?(ARGV.length)
  runtime_metadata_edition = ARGV[1] || 'r15-cd2999-20261012'
  canonical_runtime_metadata = command == 'sql' ? required_publish_runtime_metadata!(technical, runtime_metadata_edition, ARGV[2], ARGV[3]) : nil
  source_hash = canonical_runtime_metadata ? canonical_runtime_metadata.fetch('sourceHash') : Digest::SHA256.file(WORKBOOK).hexdigest
  payload = { '_generated'=>'自动生成，权威来源为只读 Excel', 'sourceHash'=>source_hash,
    'identity'=>technical, 'targets'=>[{ 'gameId'=>technical['gameId'], 'playVersion'=>technical['playVersion'] }], 'fields'=>fields }
  case command
  when 'validate' then puts "OK #{technical['gameCode']} #{fields.length} 个字段 sourceHash=#{source_hash}"
  when 'preview' then puts JSON.pretty_generate(payload)
  when 'generate'
    require 'fileutils'
    FileUtils.mkdir_p(File.dirname(OUTPUT))
    save_registry!(registry)
    # 生成物会被 Hall 发布流程并发读取。先写同目录临时文件再 rename，
    # 可保证读取方永远只看到完整的上一版或完整的新版本。
    temporary = "#{OUTPUT}.tmp-#{Process.pid}"
    begin
      File.open(temporary, 'wb', 0o600) do |file|
        file.write(JSON.pretty_generate(payload) + "\n")
        file.flush
        file.fsync
      end
      File.rename(temporary, OUTPUT)
    ensure
      File.delete(temporary) if File.exist?(temporary)
    end
    puts OUTPUT
  when 'sql' then print publish_sql(technical, fields, canonical_runtime_metadata)
  when 'diagnostic-stream' then print diagnostic_stream_sql
  when 'rollback-sql' then print rollback_sql(ARGV[1])
  else fail!("未知命令: #{command}")
  end
rescue StandardError => error
  warn "房间规则发布失败: #{error.message}"; exit 1
end
