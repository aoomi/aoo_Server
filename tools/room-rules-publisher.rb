#!/usr/bin/env ruby
require 'json'
require 'digest'
require 'rexml/document'

SERVER_ROOT = File.expand_path('..', __dir__)
PROJECT_ROOT = File.expand_path('..', SERVER_ROOT)
WORKBOOK = File.join(PROJECT_ROOT, '玩法文档/跑得快/成都跑得快开房规则表.xlsx')
OUTPUT = File.join(SERVER_ROOT, 'work/generated/room-rules/成都跑得快.generated.json')
HEADERS = %w[界面显示文字 控件类型 可选项显示文字 默认勾选 是否显示 是否可选 是否必选].freeze
CONTROLS = { '单选' => 'radio', '多选' => 'checkbox' }.freeze
# 人工表只维护可读身份。技术身份由服务端注册表确定，显示名变化不能静默生成新 code，
# 成都规则也绝不能因为同属跑得快玩法族而发布到内江 gameId。
PLAY_IDENTITIES = {
  ['四川', '成都', '跑得快', '成都跑得快'] => {
    'gameId'=>8, 'gameCode'=>'chengdu-pdk', 'playVersion'=>'1.0.0',
    'family'=>'poker:pao-de-kuai', 'regionCode'=>'CN-51-01'
  }
}.freeze
FIELD_KEYS = {
  '人数' => ['playerCount', [2, 3]], '局数' => ['roundCount', [8, 12, 16]],
  '操作时间' => ['operationTimeoutSeconds', [10, 15, 20]],
  '先出牌' => ['firstPlayRule', %w[winner_first spade_three_first]],
  '炸弹记分' => ['bombScore', [5, 10, 20]],
  '玩法' => ['playRule', %w[three_no_attachment four_with_two triple_ace_bomb remove_three_four require_spade_three]],
  '其他' => ['roomRestriction', %w[ip_limit gps_limit timeout_auto_play distance_warning interaction_forbidden chat_muted]]
}.freeze

def fail!(message)
  raise(message)
end
def xml(entry)
  value = IO.popen(['unzip', '-p', WORKBOOK, entry], &:read)
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
def defaults!(raw, count, field)
  text = raw.to_s.strip
  return [] if text.empty?
  indexes = text.split(/[,，]/).map { |value| Integer(value.strip, 10) }
  fail!("#{field} 默认序号重复") unless indexes.uniq.length == indexes.length
  fail!("#{field} 默认序号越界") unless indexes.all? { |index| index.between?(1, count) }
  indexes
rescue ArgumentError
  fail!("#{field} 默认序号必须是1基整数")
end
def parse!
  fail!("缺少权威 Excel: #{WORKBOOK}") unless File.file?(WORKBOOK)
  table = rows
  identity = ['四川', '成都', '跑得快', '成都跑得快'].map.with_index(3) do |expected, line|
    value = table.fetch(line, {})['B'].to_s.strip
    fail!("玩法身份第#{line}行必须为 #{expected}") unless value == expected
    value
  end
  technical = PLAY_IDENTITIES[identity]
  fail!('玩法身份未在服务端注册表唯一登记') unless technical
  header_lines = table.keys.select do |line|
    ('A'..'G').map { |column| table.fetch(line, {})[column].to_s.strip } == HEADERS
  end
  fail!("必须且只能存在一组七列表头: #{HEADERS.join('、')}") unless header_lines.length == 1
  header_line = header_lines.first
  fields = []
  ((header_line + 1)..10_000).each do |line|
    row = table[line]; next if row.empty? || row['A'].to_s.strip.empty?
    label, control_text, option_text = row.values_at('A', 'B', 'C').map { |value| value.to_s.strip }
    control = CONTROLS[control_text]; fail!("#{label} 使用未知控件 #{control_text}") unless control
    mapping = FIELD_KEYS[label]; fail!("#{label} 缺少服务端稳定键映射") unless mapping
    labels = option_text.split(/[|｜]/).map(&:strip)
    fail!("#{label} 存在空选项") if labels.empty? || labels.any?(&:empty?)
    key, values = mapping; fail!("#{label} 选项数量与稳定键映射不一致") unless labels.length == values.length
    visible = boolean!(row['E'], "#{label}/是否显示")
    enabled = boolean!(row['F'], "#{label}/是否可选")
    required = boolean!(row['G'], "#{label}/是否必选")
    candidates = defaults!(row['D'], labels.length, label)
    fail!("#{label} 隐藏字段不能必选") if !visible && required
    fail!("#{label} 禁用字段不能设置默认候选") if !enabled && !candidates.empty?
    fields << { 'key'=>key, 'label'=>label, 'control'=>control, 'order'=>(line - 1) * 10,
      'visible'=>visible, 'disabled'=>!enabled, 'required'=>required,
      'defaultCandidateIndexes'=>candidates,
      'options'=>labels.each_index.map { |index| { 'value'=>values[index], 'label'=>labels[index], 'order'=>(index + 1) * 10, 'disabled'=>!enabled } } }
  end
  fail!('权威 Excel 没有可发布规则') if fields.empty?
  fail!('稳定字段键重复') unless fields.map { |field| field['key'] }.uniq.length == fields.length
  [technical, fields]
end
def quote(value)
  "'#{value.to_s.gsub("'", "''")}'"
end
def publish_sql(technical, fields, source_hash)
  ui_fields = JSON.generate(fields)
  validator_fields = fields.select { |field| field['visible'] }.map { |field| field.reject { |key, _| %w[label visible defaultCandidateIndexes].include?(key) } }
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
    DROP TEMPORARY TABLE IF EXISTS chengdu_pdk_publish;
    CREATE TEMPORARY TABLE chengdu_pdk_publish AS
      SELECT a.game_id,a.region_code,a.play_version,a.index_generation old_generation,a.release_id old_release_id,
             i.component_chain,i.ui_schema old_ui,i.bundle_hash,
             JSON_OBJECT('source','chengdu-pdk-room-rule-workbook','sourceHash',@source_hash,
                         'playVersion',a.play_version,'fields',JSON_EXTRACT(@validator_fields,'$')) validator,
             COALESCE((SELECT MAX(x.index_generation) FROM aoo_compiled_room_create_index x WHERE x.game_id=a.game_id AND x.region_code=a.region_code AND x.play_version=a.play_version),0)+1 new_generation,
             COALESCE((SELECT MAX(x.release_version) FROM aoo_game_release x WHERE x.game_id=a.game_id AND x.play_version=a.play_version),0)+1 release_version
      FROM aoo_compiled_index_active a JOIN aoo_compiled_room_create_index i
        ON i.game_id=a.game_id AND i.region_code=a.region_code AND i.play_version=a.play_version AND i.index_generation=a.index_generation
      WHERE a.game_id=#{technical['gameId']} AND a.play_version=#{quote(technical['playVersion'])}
        AND (JSON_UNQUOTE(JSON_EXTRACT(i.ui_schema,'$.roomRuleSourceHash'))<>@source_hash
          OR JSON_EXTRACT(i.ui_schema,'$.roomRuleSourceHash') IS NULL
          OR JSON_LENGTH(JSON_EXTRACT(i.ui_schema,'$.fields'))<>JSON_LENGTH(JSON_EXTRACT(@fields,'$'))
          OR JSON_LENGTH(JSON_EXTRACT(i.rule_validator,'$.fields'))<>JSON_LENGTH(JSON_EXTRACT(@validator_fields,'$')));
    INSERT INTO aoo_game_release(release_id,game_id,play_version,release_version,release_scope,catalog_snapshot,rule_snapshot,ui_snapshot,component_snapshot,catalog_hash,rule_hash,ui_hash,component_hash,bundle_hash,status,rollout_percent,created_by,reason,validated_at,activated_at)
      SELECT game_id*1000000+release_version,game_id,play_version,release_version,'REGIONAL',JSON_OBJECT('gameId',game_id,'family','poker:pao-de-kuai'),JSON_OBJECT('roomRuleSchema',JSON_EXTRACT(validator,'$')),JSON_OBJECT('roomRuleSourceHash',@source_hash),JSON_OBJECT('providerVersion','1.0.0'),SHA2(CONCAT(game_id,'|catalog'),256),SHA2(validator,256),@source_hash,SHA2('pdk-provider-1.0.0',256),SHA2(CONCAT(bundle_hash,'|',@source_hash),256),'ACTIVE',100,1,'Publish Chengdu PDK seven-column room rules',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3) FROM chengdu_pdk_publish;
    INSERT INTO aoo_game_release_region(release_id,region_code,rollout_percent,status) SELECT game_id*1000000+release_version,region_code,100,'ACTIVE' FROM chengdu_pdk_publish;
    INSERT INTO aoo_compiled_room_create_index(game_id,region_code,play_version,index_generation,release_id,component_chain,rule_validator,ui_schema,lookup_hash,bundle_hash,lifecycle_state,validated_at,activated_at)
      SELECT game_id,region_code,play_version,new_generation,game_id*1000000+release_version,component_chain,JSON_EXTRACT(validator,'$'),JSON_SET(old_ui,'$.fields',JSON_EXTRACT(@fields,'$'),'$.roomRuleSourceHash',@source_hash),SHA2(CONCAT(game_id,'|',region_code,'|',play_version,'|',@source_hash),256),SHA2(CONCAT(bundle_hash,'|',@source_hash),256),'ACTIVE',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3) FROM chengdu_pdk_publish;
    UPDATE aoo_compiled_index_active a JOIN chengdu_pdk_publish p ON p.game_id=a.game_id AND p.region_code=a.region_code SET a.index_generation=p.new_generation,a.release_id=p.game_id*1000000+p.release_version,a.cache_epoch=a.cache_epoch+1,a.activation_reason='Chengdu PDK room rules publication',a.activated_at=CURRENT_TIMESTAMP(3);
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
  command = ARGV[0] || 'validate'; technical, fields = parse!; source_hash = Digest::SHA256.file(WORKBOOK).hexdigest
  payload = { '_generated'=>'自动生成，权威来源为只读 Excel', 'sourceHash'=>source_hash,
    'identity'=>technical, 'targets'=>[{ 'gameId'=>technical['gameId'], 'playVersion'=>technical['playVersion'] }], 'fields'=>fields }
  case command
  when 'validate' then puts "OK 成都跑得快 #{fields.length} 个字段 sourceHash=#{source_hash}"
  when 'preview' then puts JSON.pretty_generate(payload)
  when 'generate'
    require 'fileutils'
    FileUtils.mkdir_p(File.dirname(OUTPUT))
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
