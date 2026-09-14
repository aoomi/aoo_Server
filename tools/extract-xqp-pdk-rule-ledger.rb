#!/usr/bin/env ruby
require 'csv'
require 'json'
require 'fileutils'
require 'open3'
require 'rexml/document'
require 'digest'
require 'tmpdir'

project_root = File.expand_path('../..', __dir__)
source_root = '/Users/aoo/Code/Game/BCG/XQP/Client'
source_table = File.join(source_root, 'assets/Bundle/Config/Sample/chessRule_50.txt')
source_excel = File.join(source_root, 'config/excel/00050_跑得快总规则.xlsx')
pdk_code_root = File.join(source_root, 'assets/Script/Remote/Game/Chess/Pdk')
prefab_root = File.join(source_root, 'assets/Bundle/Game/Chess/PDK/RoomCreate')
output = File.join(project_root, 'Server/work/audit/room-rules/xqp-pdk-migration-ledger.json')

abort('XQP跑得快源Excel缺失') unless File.file?(source_excel)
abort('XQP跑得快导出表缺失') unless File.file?(source_table)

def xlsx_xml(path, entry)
  xml, status = Open3.capture2('unzip', '-p', path, entry)
  abort("源Excel缺少#{entry}") unless status.success? && !xml.empty?
  REXML::Document.new(xml)
end

def audit_source_workbook(path, exported_rows)
  workbook = xlsx_xml(path, 'xl/workbook.xml')
  sheets = REXML::XPath.match(workbook, '//*[local-name()="sheet"]').map { |node| node.attributes['name'] }
  abort("源Excel工作表异常: #{sheets.inspect}") unless sheets == ['chessRule_50']
  strings_xml = xlsx_xml(path, 'xl/sharedStrings.xml')
  strings = REXML::XPath.match(strings_xml, '//*[local-name()="si"]').map do |node|
    REXML::XPath.match(node, './/*[local-name()="t"]').map(&:text).join
  end
  sheet = xlsx_xml(path, 'xl/worksheets/sheet1.xml')
  cells = {}
  REXML::XPath.each(sheet, '//*[local-name()="c"]') do |cell|
    reference = cell.attributes['r']
    value = REXML::XPath.first(cell, './*[local-name()="v"]')&.text.to_s
    value = strings.fetch(value.to_i) if cell.attributes['t'] == 's'
    cells[reference] = value
  end
  headers = (1..3).map { |row| ('A'..'F').map { |column| cells["#{column}#{row}"].to_s } }
  expected_headers = [
    %w[唯一标识 索引 类型 参数 描述 组序列],
    %w[sid key type array memo groupOrder],
    %w[int int int int[] string int]
  ]
  abort("源Excel表头异常: #{headers.inspect}") unless headers == expected_headers
  excel_rows = (4..138).map do |row|
    ('A'..'F').map { |column| cells["#{column}#{row}"].to_s }
  end
  normalized_export = exported_rows.map { |row| row.first(6).map(&:to_s) }
  abort('源Excel与导出chessRule_50.txt逐行不一致') unless excel_rows == normalized_export
  { 'sheets'=>sheets, 'dimension'=>'A1:K138', 'dataRows'=>excel_rows.length,
    'headers'=>headers, 'exportMatchesExcel'=>true }
end

enum_file = File.join(pdk_code_root, 'ChessPdkRuleType.ts')
enum_source = File.read(enum_file, encoding: 'UTF-8')
enum_names = enum_source.scan(/(RULETYPE_[A-Z0-9_]+)\s*=\s*(\d+)/)
        .to_h { |name, value| [value.to_i, name] }
code_files = Dir[File.join(pdk_code_root, '**/*.ts')].sort.reject { |path| path == enum_file }

SEMANTICS = {
  8 => '数组第1项是房间总局数；通用创建房间代码将所选样本作为局数规则提交。',
  10 => '数组第1项是人数、第2项是每人手牌数；客户端据此恢复人数/牌数选择并以第2项展示牌数。后续sid是牌库、首出和扣分规则依赖。',
  117 => '接三带牌时先比较三张主体点数，再比较所带牌中的最大点数；两者都必须更大。',
  120 => '启用后识别连续的四张同点数组合为连续炸弹；旧客户端的连续炸弹提示函数仍为TODO，因此只证明判型，不证明提示行为。',
  122 => '下家剩1张且玩家人数达到参数阈值时，当前玩家出单张必须出手牌最大单张。',
  123 => '下家剩2张且玩家人数达到参数阈值时，当前玩家出对子必须出手牌中最大对子。',
  150 => '存在该类型才允许单张，且牌数必须为1。',
  151 => '存在该类型才允许对子，且两张必须同点；王不能组成普通对子。',
  152 => '存在该类型才允许三不带；参数1要求仅最后一手，参数2允许任意时机，参数0还受抢庄者/接牌分支限制。',
  153 => '三带牌参数：0允许带单张或一对，1只允许带单张，2只允许带一对。',
  154 => '允许三张主体带两张；参数2在客户端按三带一的带牌识别路径兼容，但最终仍由服务端判型。',
  155 => '参数是顺子最少张数；顺子最多12张，2和大小王不能进入顺子。',
  156 => '参数是连对最少对数；2和大小王不能进入连对。',
  157 => '飞机不带必须至少两个连续三张组；参数1只允许最后一手，其他已见参数允许常规时机。',
  158 => '飞机带牌参数：0允许每组三带单张或一对，1只带单张，2只带对子。',
  159 => '四带牌参数：0允许带两张或两对，1只带两张，2只带两对。',
  171 => '数组最后一项是构成炸弹所需张数，前面的点数均可按该张数识别为特殊炸弹。',
  182 => '飞机每组三张主体带两张；参数2通过另一判型路径支持比较带牌的玩法组合。',
  183 => '参数1只保留最后一次出牌，参数2按顺序显示全部出牌；这是客户端展示规则，不参与服务端出牌合法性。'
}.merge({
  28 => 'RoomRule构造时读取数组第1项作为baseNum；跑得快全部手牌、春天、炸弹、明堂和金花分均乘该底分。',
  50 => 'RoomHandler按参数0在各玩家入座时分别扣房卡、参数1由房主承担；参数2在该服务端二进制中没有完整扣费分支，保持阻塞。',
  51 => '发起解散时，离线超过数组秒数的其他玩家自动同意；在线或未超时玩家仍需投票。',
  52 => 'Room构造时把数组秒数转换成毫秒，作为每次操作的服务端超时时间。',
  54 => '参数1在入座和每局结算后由服务端触发自动准备；参数0保留手动准备。',
  60 => '源表声明单局最大秒数，但所给服务端Room构造仍把endTime固定为1800秒，未消费该样本；这是源实现缺口。',
  61 => '三人以上入座时必须提供有效经纬度，并拒绝与已入座玩家小于系统安全距离的加入。',
  62 => '三人以上入座时拒绝与已入座玩家相同IP的加入。',
  63 => '存在该类型时服务端拒绝文字聊天；不影响预设语音交互。',
  64 => '只控制小结算客户端呈现方式；服务端二进制未以该参数改变分数。',
  65 => '参数0按参与者入座，房满/开局后拒绝；参数1先以观战者进入，再显式抢座。',
  68 => '数组1至5只证明可选倍数。XQP客户端的俱乐部房使用独立baseNum/enterNum字段，历史服务端包含cent_carry账户，但没有找到type=68规则到入场分的换算、扣减或返还调用；应用时机与公式不能推断。',
  71 => '数组第1项是连续未操作次数；达到阈值后服务端把玩家置为托管，任意人工操作清零并取消托管。',
  74 => '客户端提示距离风险；该服务端二进制未以该类型拒绝操作，不能当作GPS限制。',
  75 => '存在该类型时服务端拒绝预设互动和语音互动；文字聊天由类型63单独控制。',
  100 => '春天逐个检查输家出牌次数为0；参数模式1按固定分，模式2按剩余手牌阶梯分再乘参数倍数。',
  101 => '反春检查输家仅出牌1次且已出牌张数不超过阈值；默认只检查庄家，可由可选参数扩展到全部输家，计分模式同春天。',
  102 => '存在该类型且未被首局不抢规则跳过时，发牌后进入服务端抢庄阶段。',
  103 => '存在时从庄家下家开始抢庄；参数1表示闲家全不抢时庄家不能抢并直接由庄家先出。',
  104 => '存在时仅第一局跳过抢庄，从第二局开始恢复抢庄阶段。',
  105 => '数组第1项是每名玩家参与明堂比较的初始牌型数量上限；0关闭，多板用大值表示不截断。',
  106 => '初始完整手牌中四张A记一个特殊牌型。',
  107 => '初始完整手牌中四张指定点数记一个特殊牌型；源实现分别区分四个5和其他配置点数。',
  108 => '初始完整手牌每个点数都只有一张时记全单。',
  109 => '初始完整手牌整体能按顺子判型时记全顺子。',
  110 => '初始完整手牌整体能按连对判型时记全连对。',
  111 => '初始完整手牌中每个出现的点数张数均为偶数时记全对。',
  112 => '初始完整手牌花色全部为黑桃或梅花时记全黑。',
  113 => '初始完整手牌花色全部为红桃或方片时记全红。',
  114 => '忽略10和A后，其余初始手牌点数全部不小于10时记全大。',
  115 => '忽略10和A后，其余初始手牌点数全部小于10时记全小。',
  116 => '参数大于0时从每人完整手牌选最强三张金花组合；最强者向每个对手收取参数×底分。参数0关闭。',
  118 => '数组是精确牌库ID集合；类型10引用该sid后，服务端只洗混并发放这个牌库。',
  119 => '数组第1项是选庄基准牌，第2项是抢庄结算类型；存在该规则时每局重新按基准牌/最小牌确定庄家。',
  121 => '数组按“最少剩余张数|分值”成对排列；服务端从最高阈值向下选择剩余手牌扣分。',
  160 => '模式1把每个输家的牌分乘2的总炸弹数次方并按参数封顶；模式2按固定炸弹分转移，炸弹压炸弹时先扣除被压炸弹计数。',
  161 => '手牌完整包含数组列出的全部牌ID时直接获胜。',
  162 => '手牌完整包含数组列出的全部牌ID时直接获胜；与161使用同一服务端判断。',
  181 => '数组第1项是首手必须包含的牌ID，第2项是生效局数；若庄家没有该牌，则改为庄家手中最小牌。有人抢庄后取消该限制。',
  184 => '初始牌型按两两数量差计分；默认每个差值3×底分，模式1改用数组第2项×底分。'
}).freeze

TYPE_LINES = {
  64 => [['assets/Script/Remote/Game/Chess/Common/Message/Handler/ChessRoomHandler.ts', 755]],
  74 => [['assets/Script/Remote/Game/Chess/Common/Window/ChessRoomWindow.ts', 1121]],
  8 => [['assets/Script/Remote/Home/HomeCreateRoomWindow.ts', 363]],
  10 => [['assets/Script/Remote/Game/Chess/Pdk/Window/ChessPdkRoomWindow.ts', 185],
         ['assets/Script/Remote/Game/Chess/Pdk/Window/ChessLsPdkCreateRoomWindow.ts', 91]],
  68 => [['assets/Script/Remote/Club/ClubRoomFeeWindow.ts', 160],
         ['assets/Script/Remote/Club/ClubHomeWindow.ts', 1883]],
  117 => [['assets/Script/Remote/Game/Chess/Pdk/Window/ChessPdkRoomWindow.ts', 655],
          ['assets/Script/Remote/Game/Chess/Pdk/ChessPdkPlayRule.ts', 1290]],
  120 => [['assets/Script/Remote/Game/Chess/Pdk/ChessPdkPlayRule.ts', 506],
          ['assets/Script/Remote/Game/Chess/Pdk/ChessPdkPlayRule.ts', 1323]],
  122 => [['assets/Script/Remote/Game/Chess/Pdk/Window/ChessPdkRoomWindow.ts', 465]],
  123 => [['assets/Script/Remote/Game/Chess/Pdk/Window/ChessPdkRoomWindow.ts', 489]],
  150 => [['assets/Script/Remote/Game/Chess/Pdk/ChessPdkPlayRule.ts', 72]],
  151 => [['assets/Script/Remote/Game/Chess/Pdk/ChessPdkPlayRule.ts', 82]],
  152 => [['assets/Script/Remote/Game/Chess/Pdk/ChessPdkPlayRule.ts', 108],
          ['assets/Script/Remote/Game/Chess/Pdk/Window/ChessPdkRoomWindow.ts', 523]],
  153 => [['assets/Script/Remote/Game/Chess/Pdk/ChessPdkPlayRule.ts', 134]],
  154 => [['assets/Script/Remote/Game/Chess/Pdk/ChessPdkPlayRule.ts', 169],
          ['assets/Script/Remote/Game/Chess/Pdk/Window/ChessPdkRoomWindow.ts', 744]],
  155 => [['assets/Script/Remote/Game/Chess/Pdk/ChessPdkPlayRule.ts', 197]],
  156 => [['assets/Script/Remote/Game/Chess/Pdk/ChessPdkPlayRule.ts', 236]],
  157 => [['assets/Script/Remote/Game/Chess/Pdk/ChessPdkPlayRule.ts', 271]],
  158 => [['assets/Script/Remote/Game/Chess/Pdk/ChessPdkPlayRule.ts', 312]],
  159 => [['assets/Script/Remote/Game/Chess/Pdk/ChessPdkPlayRule.ts', 423]],
  171 => [['assets/Script/Remote/Game/Chess/Pdk/ChessPdkPlayRule.ts', 541],
          ['assets/Script/Remote/Game/Chess/Pdk/ChessPdkPlayRule.ts', 1201]],
  182 => [['assets/Script/Remote/Game/Chess/Pdk/ChessPdkPlayRule.ts', 375],
          ['assets/Script/Remote/Game/Chess/Pdk/Window/ChessPdkRoomWindow.ts', 744]],
  183 => [['assets/Script/Remote/Game/Chess/Pdk/Window/ChessPdkRoomWindow.ts', 324],
          ['assets/Script/Remote/Game/Chess/Pdk/Window/ChessPdkRoomWindow.ts', 969]]
}.freeze

SERVER_TYPE_METHODS = {
  8=>['ww/chess/room/Room.class','<init>'],
  10=>['ww/chess/pdk/PDKRoom.class','deal'],
  28=>['ww/chess/room/RoomRule.class','<init>'],
  50=>['ww/chess/room/RoomHandler.class','checkRefundRoomCard'],
  51=>['ww/chess/room/RoomHandler.class','applyDissolveRoom'],
  52=>['ww/chess/room/Room.class','<init>'],
  54=>['ww/chess/room/RoomHandler.class','ready'],
  61=>['ww/chess/room/RoomHandler.class','joinRoom'],
  62=>['ww/chess/room/RoomHandler.class','joinRoom'],
  63=>['ww/chess/room/RoomHandler.class','textChat'],
  65=>['ww/chess/room/Room.class','addRole'],
  68=>['ww/club/data/ClubCent.class','modifyCent'],
  71=>['ww/chess/room/RoomRole.class','setHostingCount'],
  75=>['ww/chess/room/RoomHandler.class','presetChat'],
  100=>['ww/chess/pdk/PDKRoom.class','settelment'],
  101=>['ww/chess/pdk/PDKRoom.class','settelment'],
  102=>['ww/chess/pdk/PDKRoom.class','deal'],
  103=>['ww/chess/pdk/PDKRoom.class','competeDealer'],
  104=>['ww/chess/pdk/PDKRoom.class','deal'],
  105=>['ww/chess/pdk/PDKRoom.class','settelment'],
  106=>['ww/chess/pdk/PDKRoom.class','checkSpeCardType'],
  107=>['ww/chess/pdk/PDKRoom.class','checkSpeCardType'],
  108=>['ww/chess/pdk/PDKRoom.class','checkSpeCardType'],
  109=>['ww/chess/pdk/PDKRoom.class','checkSpeCardType'],
  110=>['ww/chess/pdk/PDKRoom.class','checkSpeCardType'],
  111=>['ww/chess/pdk/PDKRoom.class','checkSpeCardType'],
  112=>['ww/chess/pdk/PDKRoom.class','checkSpeCardType'],
  113=>['ww/chess/pdk/PDKRoom.class','checkSpeCardType'],
  114=>['ww/chess/pdk/PDKRoom.class','checkSpeCardType'],
  115=>['ww/chess/pdk/PDKRoom.class','checkSpeCardType'],
  116=>['ww/chess/JinHua.class','getMaxJinHua'],
  117=>['ww/chess/pdk/PDKRoom.class','play'],
  118=>['ww/chess/pdk/PDKRoom.class','deal'],
  119=>['ww/chess/pdk/PDKRoom.class','checkBankerPoker'],
  120=>['ww/chess/pdk/PDKPlayRule.class','rule'],
  121=>['ww/chess/pdk/PDKRoom.class','getHandScore'],
  122=>['ww/chess/pdk/PDKRoom.class','checkNextOneOrTwo'],
  123=>['ww/chess/pdk/PDKRoom.class','checkNextOneOrTwo'],
  150=>['ww/chess/pdk/PDKPlayRule.class','rule'],
  151=>['ww/chess/pdk/PDKPlayRule.class','rule'],
  152=>['ww/chess/pdk/PDKPlayRule.class','rule'],
  153=>['ww/chess/pdk/PDKPlayRule.class','rule'],
  154=>['ww/chess/pdk/PDKPlayRule.class','rule'],
  155=>['ww/chess/pdk/PDKPlayRule.class','rule'],
  156=>['ww/chess/pdk/PDKPlayRule.class','rule'],
  157=>['ww/chess/pdk/PDKPlayRule.class','rule'],
  158=>['ww/chess/pdk/PDKPlayRule.class','rule'],
  159=>['ww/chess/pdk/PDKPlayRule.class','rule'],
  160=>['ww/chess/pdk/PDKRoom.class','settelment'],
  161=>['ww/chess/pdk/PDKRoom.class','checkDirectWinCardType'],
  162=>['ww/chess/pdk/PDKRoom.class','checkDirectWinCardType'],
  171=>['ww/chess/pdk/PDKPlayRule.class','rule'],
  181=>['ww/chess/pdk/PDKRoom.class','deal'],
  182=>['ww/chess/pdk/PDKRoom.class','checkCardType'],
  183=>['ww/chess/pdk/PDKRoom.class','toRoom'],
  184=>['ww/chess/pdk/PDKRoom.class','settelment']
}.freeze

def audit_server_binary(source_root)
  commit, commit_status = Open3.capture2('git', '-C', source_root, 'rev-parse', 'HEAD')
  abort('无法定位XQP Client Git提交') unless commit_status.success?
  binary, stderr, status = Open3.capture3('git', '-C', source_root, 'show',
          'HEAD:服务器更新/bin.zip')
  abort("XQP Git中的服务器更新/bin.zip不可读: #{stderr}") unless status.success?
  required_classes = SERVER_TYPE_METHODS.values.map(&:first).uniq
  evidence = {}
  Dir.mktmpdir('xqp-pdk-server-audit') do |directory|
    zip = File.join(directory, 'server-bin.zip')
    File.binwrite(zip, binary.b)
    entries, list_status = Open3.capture2('unzip', '-Z1', zip)
    abort('无法列出XQP服务端二进制') unless list_status.success?
    required_classes.each do |relative|
      entry = "bin/#{relative}"
      abort("XQP服务端二进制缺少#{entry}") unless entries.lines.map(&:strip).include?(entry)
      bytes, extract_status = Open3.capture2('unzip', '-p', zip, entry)
      abort("无法提取#{entry}") unless extract_status.success? && !bytes.empty?
      class_path = File.join(directory, File.basename(relative))
      File.binwrite(class_path, bytes.b)
      methods, javap_status = Open3.capture2('javap', '-p', class_path)
      abort("无法检查#{entry}") unless javap_status.success?
      SERVER_TYPE_METHODS.select { |_type, pair| pair.first == relative }.each do |type, pair|
        method = pair.last
        unless method == '<init>' || methods.include?(method)
          abort("#{entry}缺少预期方法#{method}")
        end
        evidence[type] = "XQP Client Git #{commit.strip}:服务器更新/bin.zip!/bin/#{relative}##{method}"
      end
    end
  end
  {
    'gitCommit'=>commit.strip,
    'gitObject'=>'服务器更新/bin.zip',
    'sha256'=>Digest::SHA256.hexdigest(binary.b),
    'classesVerified'=>required_classes,
    'evidenceByType'=>evidence
  }
end

MIGRATED_TYPES = ([8,10,28,51,52,54,60,61,62,63,64,65,71,74,75] + (100..123).to_a +
        (150..162).to_a + [171,181,182,183,184]).freeze
MIGRATED_SIDS = [500012].freeze
BLOCKED_SIDS = {
  500011=>'阻塞：Aoo当前持久化创房Saga只保留房主扣费；按入座人逐个事务扣费与失败补偿未闭环，禁止伪装已实现',
  500074=>'阻塞：XQP所给服务端二进制没有参数2的完整扣费分支，无法等价迁移',
  500081=>'阻塞：已审计XQP客户端俱乐部代码及历史服务端ClubCent；未找到type=68倍数到enterNum/cent_carry的权威换算与账务时机，禁止猜测'
}.freeze

def category(type)
  return '房间与流程' if [8,10,28,50,51,52,54,60,61,62,63,64,65,68,71,74,75].include?(type)
  return '牌型与出牌' if [117,120,122,123,150,151,152,153,154,155,156,157,158,159,171,181,182,183].include?(type)
  return '炸弹与特殊牌型' if [106,107,118,119,160,161,162].include?(type)
  return '计分与结算' if (100..116).cover?(type) || [121,184].include?(type)
  '待分类'
end

def prefab_evidence(prefab_root)
  evidence = Hash.new { |hash, key| hash[key] = [] }
  defaults = Hash.new { |hash, key| hash[key] = [] }
  Dir[File.join(prefab_root, '**/*.prefab')].sort.each do |path|
    objects = JSON.parse(File.read(path, encoding: 'UTF-8'))
    relative = path.delete_prefix(File.dirname(prefab_root) + '/')
    region = path.split('/RoomCreate/', 2).last.split('/', 2).first
    objects.each_with_index do |object, index|
      next unless object.is_a?(Hash) && object['__type__'] == 'cc.Node'
      ids = object['_name'].to_s.scan(/500\d{3}/).map(&:to_i)
      next if ids.empty?
      toggle = Array(object['_components']).map { |ref| objects.dig(ref['__id__']) }
              .find { |component| component.is_a?(Hash) && component['__type__'] == 'cc.Toggle' }
      ids.each do |sid|
        evidence[sid] << "#{relative}#node=#{object['_name']}"
        defaults[sid] << "#{region}=#{toggle && toggle['_isChecked'] ? '默认选中' : '默认未选'}"
      end
    end
  end
  [evidence.transform_values(&:uniq), defaults.transform_values(&:uniq)]
end

prefab_refs, prefab_defaults = prefab_evidence(prefab_root)
script_refs = Hash.new { |hash, key| hash[key] = [] }
code_files.each do |path|
  relative = path.delete_prefix(source_root + '/')
  File.readlines(path, encoding: 'UTF-8').each_with_index do |line, index|
    line.scan(/500\d{3}/).map(&:to_i).uniq.each { |sid| script_refs[sid] << "#{relative}:#{index + 1}" }
  end
end

exported_rows = CSV.read(source_table, encoding: 'UTF-8')
workbook_audit = audit_source_workbook(source_excel, exported_rows)
server_audit = audit_server_binary(source_root)
rows = exported_rows.each_with_index.map do |row, index|
  sid_text, key, type_text, array, memo, group_order = row
  sid = sid_text.to_i
  type = Integer(type_text, 10)
  dependencies = array.to_s.split('|').select { |value| value.match?(/\A500\d{3}\z/) }.map(&:to_i)
  semantic_evidence = Array(TYPE_LINES[type]).map { |path, line| "#{path}:#{line}" }
  direct_evidence = Array(script_refs[sid])
  configuration_evidence = Array(prefab_refs[sid])
  server_evidence = Array(server_audit.fetch('evidenceByType')[type])
  all_evidence = (semantic_evidence + direct_evidence + server_evidence).uniq
  proven = SEMANTICS.key?(type)
  status = if BLOCKED_SIDS.key?(sid)
      BLOCKED_SIDS.fetch(sid)
    elsif proven && (MIGRATED_SIDS.include?(sid))
      '已迁移：房主支付由Aoo持久化RoomCreateSaga与权威账本扣费实现'
    elsif proven && type == 60
      '已迁移：源服务端未消费该声明；Aoo保留不可变审计值且不伪造超时行为'
    elsif proven && [64,74].include?(type)
      '已迁移：已进入Aoo权威快照并作为客户端呈现元数据下发'
    elsif proven && MIGRATED_TYPES.include?(type)
      '已迁移：XQP服务端语义已进入Aoo权威策略，待地区玩法显式装配'
    elsif proven
      '已取证待迁移：Aoo尚未完整实现该语义'
    elsif configuration_evidence.empty? && direct_evidence.empty?
      '阻塞：XQP Client未找到业务判断或玩法组合证据'
    else
      '阻塞：只有创建组合/展示证据，缺少服务端业务判断'
    end
  default_text = prefab_defaults[sid].to_a.empty? ?
          '源总规则是样本库，不声明默认；未在地区Prefab发现该sid' :
          "源总规则不声明默认；地区Prefab证据：#{prefab_defaults[sid].join('｜')}"
  {
    'sourceRuleId'=>sid, 'sourceKey'=>key.to_i, 'sourceType'=>type,
    'sourceEnum'=>enum_names[type], 'sourceArray'=>array.to_s, 'sourceName'=>memo.to_s,
    'sourceGroupOrder'=>group_order.to_s,
    'sourceExcelLocation'=>"chessRule_50!A#{index + 4}:F#{index + 4}",
    'sourceCodeEvidence'=>all_evidence,
    'sourceConfigurationEvidence'=>configuration_evidence,
    'sourceDefaultEvidence'=>prefab_defaults[sid].to_a,
    'businessSemantics'=>SEMANTICS[type] || '源表只给出文案和参数；Client仅能证明选择/展示，服务端判定与结算源码不在给定源项目中，禁止推断。',
    'applicablePlayers'=> type == 10 ? '数组第1项明确声明' : '由引用该sid的地区玩法组合确定',
    'defaultValue'=>default_text,
    'dependencies'=>dependencies,
    'mutualExclusion'=>"同type=#{type}的不同sid只能由地区创建组合证明互斥，不能仅按type猜测",
    'targetLayer'=>category(type),
    'targetFieldKey'=>"xqp.pdk.type#{type}.sid#{sid}",
    'targetBackendStrategy'=>if sid == 500012 then 'JdbcRoomSagaBillingPort/JdbcBillingService'; else case category(type); when '牌型与出牌','炸弹与特殊牌型' then 'PdkCardPatternPolicy/PaoDeKuaiRuleSet'; when '计分与结算' then 'PdkScoringPolicy/PdkSettlementContext'; else 'PdkVariantPolicy/房间权威配置'; end; end,
    'testPlan'=>'启用、关闭、参数边界、非法组合、依赖缺失；适用时覆盖2/3/4人、恢复重连和结算',
    'status'=>status
  }
end

abort("源规则数量异常: #{rows.length}") unless rows.length == 135
ids = rows.map { |row| row['sourceRuleId'] }
abort('源规则编号重复或不连续') unless ids == (500001..500135).to_a
result = {
  'sourceExcel'=>source_excel, 'sourceSheet'=>'chessRule_50',
  'sourceWorkbookAudit'=>workbook_audit,
  'sourceServerBinaryAudit'=>server_audit.reject { |key, _| key == 'evidenceByType' },
  'sourceHeaders'=>%w[唯一标识 索引 类型 参数 描述 组序列],
  'totalRules'=>rows.length,
  'rulesWithCodeEvidence'=>rows.count { |row| row['status'].start_with?('已取证') || row['status'].start_with?('已迁移') || row['status'].start_with?('阻塞') },
  'migratedRules'=>rows.count { |row| row['status'].start_with?('已迁移') },
  'rulesWithConfigurationEvidence'=>rows.count { |row| !row['sourceConfigurationEvidence'].empty? },
  'blockedRules'=>rows.count { |row| row['status'].start_with?('阻塞') },
  'ruleTypes'=>rows.map { |row| row['sourceType'] }.uniq.sort,
  'rules'=>rows
}
FileUtils.mkdir_p(File.dirname(output))
File.write(output, JSON.pretty_generate(result) + "\n", encoding: 'UTF-8')
puts JSON.generate(result.select { |key, _| %w[totalRules rulesWithCodeEvidence migratedRules rulesWithConfigurationEvidence blockedRules ruleTypes].include?(key) })
