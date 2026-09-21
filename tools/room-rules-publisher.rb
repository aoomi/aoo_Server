#!/usr/bin/env ruby
require 'json'
require 'digest'
require 'rexml/document'

SERVER_ROOT = File.expand_path('..', __dir__)
PROJECT_ROOT = File.expand_path('..', SERVER_ROOT)
WORKBOOK = File.expand_path(ENV.fetch('AOO_ROOM_RULE_WORKBOOK', File.join(PROJECT_ROOT, 'Client/docs/开房规则表/跑得快/成都跑得快.xlsx')))
OUTPUT = File.expand_path(ENV.fetch('AOO_ROOM_RULE_GENERATED', File.join(SERVER_ROOT, "work/generated/room-rules/#{File.basename(WORKBOOK, '.xlsx')}.generated.json")))
REGISTRY = File.expand_path(ENV.fetch('AOO_ROOM_RULE_REGISTRY', OUTPUT.sub(/\.generated\.json\z/, '.keys.json')))
IDENTITY_REGISTRY = File.expand_path(ENV.fetch('AOO_ROOM_RULE_IDENTITY_REGISTRY', File.join(__dir__, 'room-rule-play-identities.json')))
HEADERS = %w[界面显示文字 控件类型 可选项显示文字 默认勾选 托管次数 是否显示].freeze
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

def positive_integer!(value, field)
  number = Integer(value.to_s.strip, 10)
  fail!("#{field} 必须是正整数") unless number.positive?
  number
rescue ArgumentError
  fail!("#{field} 必须是正整数")
end

def resolve_play_identity!(source_identity)
  payload = JSON.parse(File.read(IDENTITY_REGISTRY, encoding: 'UTF-8'))
  matches = payload.fetch('plays').select { |play| play.fetch('sourceIdentity') == source_identity }
  fail!('玩法身份必须在外部技术身份注册表唯一登记') unless matches.length == 1
  matches.first.reject { |key, _| %w[sourceIdentity workbook].include?(key) }
rescue Errno::ENOENT, JSON::ParserError, KeyError => error
  fail!("玩法技术身份注册表无效: #{error.message}")
end

def load_registry(technical)
  payload = if File.file?(REGISTRY)
    JSON.parse(File.read(REGISTRY, encoding: 'UTF-8'))
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

def reconcile_options!(technical, field, field_label, labels)
  field['options'] ||= []
  values = labels.map { |label| stable_option_value!(technical, field, field_label, label) }
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

def parse!
  fail!("缺少权威 Excel: #{WORKBOOK}") unless File.file?(WORKBOOK)
  table = rows
  identity = %w[省份 地区名称 显示名称].map do |label|
    matches = table.values.select { |row| row['A'].to_s.strip == label }
    fail!("玩法身份字段#{label}必须且只能存在一次") unless matches.length == 1
    matches.first['B'].to_s.strip
  end
  technical = resolve_play_identity!(identity)
  header_lines = table.keys.select do |line|
    ('A'..'F').map { |column| table.fetch(line, {})[column].to_s.strip } == HEADERS
  end
  fail!("必须且只能存在一组六列表头: #{HEADERS.join('、')}") unless header_lines.length == 1
  header_line = header_lines.first
  source_rows = []
  ((header_line + 1)..10_000).each do |line|
    row = table[line]; next if row.empty? || row['A'].to_s.strip.empty?
    label, control_text, option_text = row.values_at('A', 'B', 'C').map { |value| value.to_s.strip }
    control = CONTROLS[control_text]; fail!("#{label} 使用未知控件 #{control_text}") unless control
    labels = option_text.split(/[|｜]/).map(&:strip)
    fail!("#{label} 存在空选项") if labels.empty? || labels.any?(&:empty?)
    visible = boolean!(row['F'], "#{label}/是否显示")
    candidates = defaults!(row['D'], labels.length,
      "#{File.basename(WORKBOOK)}!sheet1 第#{line}行 #{label}/默认勾选(D#{line})" \
      "（控件类型=#{control}，可选项数=#{labels.length}，默认原值=#{row['D'].to_s.strip.inspect}）",
      control)
    trustee_count = positive_integer!(row['E'], "#{label}/托管次数")
    source_rows << { 'line'=>line, 'label'=>label, 'control'=>control, 'labels'=>labels,
      'visible'=>visible, 'candidates'=>candidates, 'trusteeCount'=>trustee_count }
  end
  fail!('权威 Excel 没有可发布规则') if source_rows.empty?
  registry = load_registry(technical)
  registry['fields'] ||= []
  required_fields = technical.fetch('requiredRoomRuleFields', [])
  registered_fields = reconcile_entries!(registry['fields'], source_rows.map { |row| row.fetch('label') }, lambda do |index|
    label = source_rows[index].fetch('label')
    required = required_fields.find { |field| field.fetch('sourceLabel') == label }
    existing = required && registry['fields'].find { |field| field['key'] == required.fetch('key') }
    next existing if existing
    entry = { 'key'=>required ? required.fetch('key') : allocate_field_key!(registry), 'label'=>label,
      'position'=>index + 1, 'active'=>true, 'nextOptionSequence'=>1, 'options'=>[] }
    registry['fields'] << entry
    entry
  end)
  fields = source_rows.each_with_index.map do |source, index|
    registered = registered_fields[index]
    options = reconcile_options!(technical, registered, source.fetch('label'), source.fetch('labels'))
    values = options.map { |option| option.fetch('value') }
    control = source.fetch('control'); candidates = source.fetch('candidates')
    default_value = if control == 'radio'
      candidates.empty? ? nil : values[candidates.first - 1]
    else
      candidates.map { |index| values[index - 1] }
    end
    fail!("#{source.fetch('label')} 单选必须且只能配置一个默认项") if control == 'radio' && candidates.length != 1
    { 'key'=>registered.fetch('key'), 'label'=>source.fetch('label'), 'control'=>control,
      'order'=>(source.fetch('line') - 1) * 10, 'visible'=>source.fetch('visible'),
      'disabled'=>false, 'required'=>control == 'radio', 'trusteeCount'=>source.fetch('trusteeCount'),
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
def quote(value)
  "'#{value.to_s.gsub("'", "''")}'"
end
def publish_sql(technical, fields, source_hash)
  display_name = technical.fetch('displayName', File.basename(WORKBOOK, '.xlsx'))
  ui_fields = JSON.generate(fields)
  validator_fields = fields.select { |field| field['visible'] }.map { |field| field.reject { |key, _| %w[label visible defaultCandidateIndexes].include?(key) } }
  trustee_counts = fields.map { |field| field['trusteeCount'].to_i }.select(&:positive?).uniq
  fail!("#{File.basename(WORKBOOK)} 托管次数必须唯一") unless trustee_counts.length == 1
  # 托管次数来自权威规则表，但不是玩家可编辑的界面选项。作为禁用的服务端字段
  # 写入校验器，使 Hall 规范化房间规则时能够保留该值，客户端无法伪造。
  validator_fields << { 'key'=>'hostingMissThreshold', 'control'=>'number', 'order'=>9_999,
    'disabled'=>true, 'required'=>true, 'defaultValue'=>trustee_counts.first,
    'min'=>1, 'max'=>100, 'step'=>1, 'options'=>[] }
  # payerMode 是平台计费治理字段，不属于用户维护的玩法 Excel，也不应显示在规则界面。
  # 服务端发布器提供唯一默认值，保证客户端无法伪造付费策略且计费 Saga 始终获得确定输入。
  validator_fields << { 'key'=>'payerMode', 'control'=>'radio', 'order'=>10_000,
    'disabled'=>false, 'required'=>true, 'defaultValue'=>'OWNER',
    'options'=>[{ 'value'=>'OWNER', 'order'=>10, 'disabled'=>false }] }
  validator_fields = JSON.generate(validator_fields)
  <<~SQL
    START TRANSACTION;
    SET @source_hash=#{quote(source_hash)};
    SET @fields=#{quote(ui_fields)};
    SET @validator_fields=#{quote(validator_fields)};
    -- 本地 seed 与规则发布必须收敛到同一可读目录身份。客户端只展示 catalog 的
    -- displayName，不能为了修 UI 再引入 gameId/code 到中文名称的平行映射。
    UPDATE aoo_game_catalog
       SET display_name=#{quote(display_name)}, family_code=#{quote(technical['family'])},
           row_version=row_version+1
     WHERE game_id=#{technical['gameId']}
       AND (display_name<>#{quote(display_name)} OR family_code<>#{quote(technical['family'])});
    DROP TEMPORARY TABLE IF EXISTS chengdu_pdk_publish;
    CREATE TEMPORARY TABLE chengdu_pdk_publish AS
      SELECT a.game_id,a.region_code,a.play_version,a.index_generation old_generation,a.release_id old_release_id,
             i.component_chain,i.ui_schema old_ui,i.bundle_hash,
             JSON_OBJECT('source',#{quote(technical['gameCode'] + '-room-rule-workbook')},'sourceHash',@source_hash,
                         'playVersion',a.play_version,'fields',JSON_EXTRACT(@validator_fields,'$')) validator,
             COALESCE((SELECT MAX(x.index_generation) FROM aoo_compiled_room_create_index x WHERE x.game_id=a.game_id AND x.region_code=a.region_code AND x.play_version=a.play_version),0)+1 new_generation,
             COALESCE((SELECT MAX(x.release_version) FROM aoo_game_release x WHERE x.game_id=a.game_id AND x.play_version=a.play_version),0)+1 release_version
      FROM aoo_compiled_index_active a JOIN aoo_compiled_room_create_index i
        ON i.game_id=a.game_id AND i.region_code=a.region_code AND i.play_version=a.play_version AND i.index_generation=a.index_generation
      WHERE a.game_id=#{technical['gameId']} AND a.play_version=#{quote(technical['playVersion'])}
        AND (JSON_UNQUOTE(JSON_EXTRACT(i.ui_schema,'$.roomRuleSourceHash'))<>@source_hash
          OR JSON_EXTRACT(i.ui_schema,'$.roomRuleSourceHash') IS NULL
          OR CAST(JSON_EXTRACT(i.ui_schema,'$.fields') AS CHAR)<>CAST(JSON_EXTRACT(@fields,'$') AS CHAR)
          OR CAST(JSON_EXTRACT(i.rule_validator,'$.fields') AS CHAR)<>CAST(JSON_EXTRACT(@validator_fields,'$') AS CHAR));
    INSERT INTO aoo_game_release(release_id,game_id,play_version,release_version,release_scope,catalog_snapshot,rule_snapshot,ui_snapshot,component_snapshot,catalog_hash,rule_hash,ui_hash,component_hash,bundle_hash,status,rollout_percent,created_by,reason,validated_at,activated_at)
      SELECT game_id*1000000+release_version,game_id,play_version,release_version,'REGIONAL',JSON_OBJECT('gameId',game_id,'family','poker:pao-de-kuai'),JSON_OBJECT('roomRuleSchema',JSON_EXTRACT(validator,'$')),JSON_OBJECT('roomRuleSourceHash',@source_hash),JSON_OBJECT('providerVersion','1.0.0'),SHA2(CONCAT(game_id,'|catalog'),256),SHA2(validator,256),@source_hash,SHA2('pdk-provider-1.0.0',256),SHA2(CONCAT(bundle_hash,'|',@source_hash),256),'ACTIVE',100,1,#{quote("Publish #{technical['gameCode']} seven-column room rules")},CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3) FROM chengdu_pdk_publish;
    INSERT INTO aoo_game_release_region(release_id,region_code,rollout_percent,status) SELECT game_id*1000000+release_version,region_code,100,'ACTIVE' FROM chengdu_pdk_publish;
    INSERT INTO aoo_compiled_room_create_index(game_id,region_code,play_version,index_generation,release_id,component_chain,rule_validator,ui_schema,lookup_hash,bundle_hash,lifecycle_state,validated_at,activated_at)
      SELECT game_id,region_code,play_version,new_generation,game_id*1000000+release_version,component_chain,JSON_EXTRACT(validator,'$'),JSON_SET(old_ui,'$.fields',JSON_EXTRACT(@fields,'$'),'$.roomRuleSourceHash',@source_hash),SHA2(CONCAT(game_id,'|',region_code,'|',play_version,'|',@source_hash),256),SHA2(CONCAT(bundle_hash,'|',@source_hash),256),'ACTIVE',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3) FROM chengdu_pdk_publish;
    UPDATE aoo_compiled_index_active a JOIN chengdu_pdk_publish p ON p.game_id=a.game_id AND p.region_code=a.region_code SET a.index_generation=p.new_generation,a.release_id=p.game_id*1000000+p.release_version,a.cache_epoch=a.cache_epoch+1,a.activation_reason=#{quote(technical['gameCode'] + ' room rules publication')},a.activated_at=CURRENT_TIMESTAMP(3);
    UPDATE aoo_compiled_room_create_index i JOIN chengdu_pdk_publish p ON p.game_id=i.game_id AND p.region_code=i.region_code AND p.old_generation=i.index_generation SET i.lifecycle_state='RETIRED',i.retired_at=CURRENT_TIMESTAMP(3);
    UPDATE aoo_game_release r JOIN chengdu_pdk_publish p ON p.old_release_id=r.release_id SET r.status='RETIRED',r.retired_at=CURRENT_TIMESTAMP(3);
    SELECT game_id,region_code,play_version,new_generation AS index_generation,game_id*1000000+release_version AS release_id FROM chengdu_pdk_publish;
    COMMIT;
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
  command = ARGV[0] || 'validate'; technical, fields, registry = parse!; source_hash = Digest::SHA256.file(WORKBOOK).hexdigest
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
  when 'sql' then print publish_sql(technical, fields, source_hash)
  when 'rollback-sql' then print rollback_sql(ARGV[1])
  else fail!("未知命令: #{command}")
  end
rescue StandardError => error
  warn "房间规则发布失败: #{error.message}"; exit 1
end
