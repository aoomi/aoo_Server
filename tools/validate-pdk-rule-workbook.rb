#!/usr/bin/env ruby
require 'rexml/document'
require 'open3'

SERVER_ROOT = File.expand_path('..', __dir__)
PROJECT_ROOT = File.expand_path('..', SERVER_ROOT)
WORKBOOK = File.join(PROJECT_ROOT, '玩法文档/跑得快/跑得快可配置规则总表.xlsx')
REQUIRED_SHEETS = %w[总览 全部源规则台账 公共基础规则 可选牌型 流程规则 结算规则 互斥依赖 地区差异 迁移证据 未决项].freeze
EXPECTED_HEADERS = %w[源规则编号 源类型 源名称 源参数 组序列 Excel位置 源代码证据 准确语义/证据边界 依赖规则 源默认值 Aoo归属 目标字段键 后端策略 状态].freeze
EXPECTED_IDS = (500001..500135).to_a.freeze
EXPECTED_TYPES = [8, 10, 28, 50, 51, 52, 54, 60, 61, 62, 63, 64, 65, 68, 71, 74, 75, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122, 123, 150, 151, 152, 153, 154, 155, 156, 157, 158, 159, 160, 161, 162, 171, 181, 182, 183, 184].freeze

def fail!(message)
  warn "跑得快迁移台账校验失败: #{message}"
  exit 1
end

def unzip_xml(entry)
  xml, status = Open3.capture2('unzip', '-p', WORKBOOK, entry)
  fail!("Excel 文件损坏或缺少 #{entry}") unless status.success? && !xml.empty?
  REXML::Document.new(xml)
rescue REXML::ParseException => error
  fail!("#{entry} 无法解析: #{error.message}")
end

def workbook_sheets
  document = unzip_xml('xl/workbook.xml')
  REXML::XPath.match(document, '//*[local-name()="sheet"]').map { |sheet| sheet.attributes['name'] }
end

def ledger_rows
  document = unzip_xml('xl/worksheets/sheet2.xml')
  rows = Hash.new { |hash, key| hash[key] = {} }
  REXML::XPath.each(document, '//*[local-name()="c"]') do |cell|
    reference = cell.attributes['r']
    next unless reference
    value = REXML::XPath.first(cell, './*[local-name()="v"]')&.text.to_s.strip
    rows[reference[/\d+/].to_i][reference[/[A-Z]+/]] = value
  end
  rows
end

def validate!
  fail!("缺少迁移台账: #{WORKBOOK}") unless File.file?(WORKBOOK)
  sheets = workbook_sheets
  fail!("工作表必须严格为: #{REQUIRED_SHEETS.join('、')}") unless sheets == REQUIRED_SHEETS

  rows = ledger_rows
  columns = ('A'..'N').to_a
  headers = columns.map { |column| rows.fetch(4, {})[column].to_s }
  fail!("台账表头不允许修改，应为: #{EXPECTED_HEADERS.join('、')}") unless headers == EXPECTED_HEADERS

  ledger = rows.keys.sort.map do |row_number|
    next if row_number < 5
    cells = columns.map { |column| rows.fetch(row_number, {})[column].to_s.strip }
    next if cells.all?(&:empty?)
    required_indexes = [0, 1, 2, 5, 6, 7, 8, 9, 10, 11, 12, 13]
    fail!("第 #{row_number} 行缺少必填台账列") if required_indexes.any? { |index| cells[index].empty? }
    id, type, name, _params, _group, location, evidence, semantics, _dependencies,
      default_value, layer, field_key, strategy, status = cells
    fail!("第 #{row_number} 行源规则编号非法: #{id}") unless id.match?(/\A500\d{3}\z/)
    fail!("第 #{row_number} 行源类型非法: #{type}") unless type.match?(/\A\d+\z/)
    fail!("第 #{row_number} 行规则名称为空") if name.empty?
    expected_location = "chessRule_50!A#{row_number - 1}:F#{row_number - 1}"
    fail!("第 #{row_number} 行Excel位置错误，应为 #{expected_location}") unless location == expected_location
    fail!("第 #{row_number} 行缺少证据边界") if semantics.length < 12
    fail!("第 #{row_number} 行不得伪造源默认值") unless default_value.include?('不声明') || default_value.include?('未声明')
    fail!("第 #{row_number} 行Aoo归属为空") if layer.empty?
    fail!("第 #{row_number} 行目标字段键格式非法: #{field_key}") unless field_key.match?(/\Axqp\.pdk\.type\d+\.sid#{id}\z/)
    fail!("第 #{row_number} 行后端策略为空") if strategy.empty?
    if evidence == '无'
      source_noop = type.to_i == 60 && semantics.include?('未消费该样本')
      fail!("第 #{row_number} 行无代码证据但未FAIL-CLOSED") unless status.start_with?('阻塞') || source_noop
    else
      allowed_prefixes = ['assets/Script/Remote/Game/Chess/Pdk/',
        'assets/Script/Remote/Game/Chess/Common/', 'assets/Script/Remote/Home/',
        'assets/Script/Remote/Club/',
        'XQP Client Git ']
      fail!("第 #{row_number} 行代码证据不是XQP规则路径") unless evidence.lines.all? { |line| allowed_prefixes.any? { |prefix| line.start_with?(prefix) } }
      fail!("第 #{row_number} 行有证据却仍标记为无证据阻塞") if status == '阻塞：XQP Client未找到业务判断证据'
    end
    { id: id.to_i, type: type.to_i, status: status, evidence: evidence }
  end.compact

  ids = ledger.map { |row| row[:id] }
  fail!("必须逐条覆盖135条源规则，实际 #{ledger.length}") unless ledger.length == 135
  fail!('源规则编号重复') unless ids.uniq.length == ids.length
  missing = EXPECTED_IDS - ids
  extra = ids - EXPECTED_IDS
  fail!("源规则覆盖不完整，缺少=#{missing.inspect}，多出=#{extra.inspect}") unless missing.empty? && extra.empty?
  types = ledger.map { |row| row[:type] }.uniq.sort
  fail!("源类型覆盖不完整: #{types.inspect}") unless types == EXPECTED_TYPES
  blocked = ledger.count { |row| row[:status].start_with?('阻塞') }
  migrated = ledger.count { |row| row[:status].start_with?('已迁移') }
  evidenced = ledger.count { |row| row[:status].start_with?('已取证') || row[:status].start_with?('已迁移') || row[:status].start_with?('阻塞') }
  [ledger.length, evidenced, migrated, blocked]
end

total, evidenced, migrated, blocked = validate!
puts "跑得快迁移台账校验通过: 源规则 #{total} 条，代码证据 #{evidenced} 条，已迁移 #{migrated} 条，明确阻塞 #{blocked} 条"
