#!/usr/bin/env ruby
require 'digest'
require 'fileutils'
require 'json'
require 'open3'
require 'rbconfig'

SERVER_ROOT = File.expand_path('../../..', __dir__)
PROJECT_ROOT = File.expand_path('..', SERVER_ROOT)
IDENTITY_REGISTRY = File.join(SERVER_ROOT, 'release-input/identity/room-rule-play-identities.json')
PUBLISH = ENV.fetch('AOO_ROOM_RULE_PUBLISHER', File.join(SERVER_ROOT, 'engineering/release/tool-inputs/publish-room-rules.sh'))
POLL_SECONDS = Float(ENV.fetch('AOO_ROOM_RULE_POLL_SECONDS', '1'))
STABLE_SECONDS = Float(ENV.fetch('AOO_ROOM_RULE_STABLE_SECONDS', '0.6'))
LOGGED_ONCE = {}

def log_once(key, message)
  return if LOGGED_ONCE[key]
  LOGGED_ONCE[key] = true
  log(message)
end

# 用户维护的正式规则表根目录；每个 Excel 的身份由表内 sourceIdentity 三元组读取，
# gameId/gameCode/playVersion 仍只从稳定身份注册表精确匹配，不从目录或文件名推测。
OFFICIAL_RULE_ROOT = File.join(PROJECT_ROOT, 'Client/docs/开房规则表')
PUBLISHER_SCRIPT = File.join(SERVER_ROOT, 'engineering/release/tool-inputs/room-rules-publisher.rb')
IDENTITY_SCAN_CACHE = {}

def load_registered_plays
  payload = JSON.parse(File.read(IDENTITY_REGISTRY, encoding: 'UTF-8'))
  payload.fetch('plays')
end

def registered_workbook_path(play)
  relative = play.fetch('workbookPath')
  candidate = File.expand_path(relative, PROJECT_ROOT)
  unless candidate.start_with?(PROJECT_ROOT + File::SEPARATOR)
    raise "room-rules workbookPath 越出项目目录: #{relative}"
  end
  candidate
end

# 稳定身份：注册表条目的 workbook 是地区表的稳定文件名，workbookPath 只是它当前所在的位置。
# 早期条目还没有稳定身份字段时按同一语义回退到 workbookPath 的文件名部分，并留下可见告警，
# 避免「注册表少了字段」被静默当成「地区表不存在」。
def stable_workbook_name(play, relative)
  play.fetch('workbook') do
    log_once("identity-fallback:#{relative}",
             "room-rules 注册表条目缺少稳定身份 workbook，按 workbookPath 文件名回退: #{relative}")
    File.basename(relative)
  end
end

def registered_workbooks
  plays = load_registered_plays.select do |play|
    play.fetch('publishingEnabled', true) && play.fetch('automaticPublishingEnabled', true)
  end
  registered = plays.map do |play|
    relative = play.fetch('workbookPath')
    path = registered_workbook_path(play)
    name = stable_workbook_name(play, relative)
    # 稳定身份和登记路径必须指向同一张表，否则地区身份已经和文件漂移。
    unless File.basename(path) == name
      raise "room-rules 稳定身份与登记路径不一致: workbook=#{name} workbookPath=#{relative}"
    end
    [name, path]
  end
  raise 'room-rules workbookPath 不得重复登记' unless registered.map { |(_, path)| path }.uniq.length == registered.length
  names = registered.map { |(name, _)| name }
  duplicated = names.each_with_object(Hash.new(0)) { |name, counts| counts[name] += 1 }
                    .select { |_, count| count > 1 }.keys
  raise "room-rules 稳定身份不得重复登记: #{duplicated.join('、')}" unless duplicated.empty?
  registered.to_h
end

def source_identity_for_workbook(path)
  stdout, stderr, status = Open3.capture3(
    { 'AOO_ROOM_RULE_WORKBOOK'=>path }, RbConfig.ruby, PUBLISHER_SCRIPT, 'source-identity', chdir: SERVER_ROOT
  )
  unless status.success?
    raise "正式 room-rules Excel 身份读取失败 #{path}: #{utf8(stderr.empty? ? stdout : stderr)}"
  end
  JSON.parse(stdout)
rescue JSON::ParserError => error
  raise "正式 room-rules Excel 身份输出无效 #{path}: #{error.message}"
end

# 全玩法目录按内容身份分类，而不是用玩法文件名后缀猜测。没有身份三元组的汇总/说明
# 工作簿不发布；存在完整身份的工作簿必须精确且唯一匹配身份注册表，路径也必须一致。
# 缓存只在工作簿清单、size/mtime 或身份注册表变化时重扫，避免常驻 watcher 每秒重复
# 启动 Ruby 子进程；变更文件或新增 workbook 会使签名失效并立即重验。
def verify_official_rule_directory!(all_plays)
  raise "正式 room-rules 根目录不存在: #{OFFICIAL_RULE_ROOT}" unless File.directory?(OFFICIAL_RULE_ROOT)
  paths = Dir.glob(File.join(OFFICIAL_RULE_ROOT, '**', '*.xlsx')).sort
  registry_stat = File.stat(IDENTITY_REGISTRY)
  signature = [registry_stat.size, registry_stat.mtime.to_f,
    paths.map { |path| stat = File.stat(path); [path, stat.size, stat.mtime.to_f] }]
  return IDENTITY_SCAN_CACHE[:unregistered] if IDENTITY_SCAN_CACHE[:signature] == signature

  plays_by_identity = all_plays.group_by { |play| play.fetch('sourceIdentity') }
  unregistered = []
  paths.each do |path|
    identity = source_identity_for_workbook(path)
    next if identity.nil?
    matches = plays_by_identity.fetch(identity, [])
    if matches.empty?
      unregistered << { 'path'=>path, 'sourceIdentity'=>identity }
      log_once("unregistered-source-identity:#{path}",
        "正式目录发现未登记玩法规则表，暂不发布且不猜 gameCode/gameId/playVersion: #{path} sourceIdentity=#{identity.inspect}")
      next
    end
    raise "room-rules sourceIdentity 必须唯一登记: #{path}" unless matches.length == 1
    registered_path = registered_workbook_path(matches.first)
    unless registered_path == path
      raise "room-rules sourceIdentity 与登记路径不一致: #{path} registered=#{registered_path}"
    end
  end
  IDENTITY_SCAN_CACHE[:signature] = signature
  IDENTITY_SCAN_CACHE[:unregistered] = unregistered
  unregistered
end

def resolve_workbooks
  override = ENV['AOO_ROOM_RULE_WORKBOOK'].to_s
  if override.empty?
    all_plays = load_registered_plays
    registered = registered_workbooks
    verify_official_rule_directory!(all_plays)
    absent = registered.select { |_, path| !File.file?(path) }.keys
    absent.each do |name|
      log_once("absent:#{name}",
               "room-rules 注册表登记的地区表文件不存在，本轮不监听: #{name}")
    end
    return registered.values.uniq.sort
  end
  # 正式服务自动发现目录中的地区表；只有测试可改为临时副本。
  # 只有测试用例可以把 watcher 指向临时副本，以验证损坏文件 fail-closed，
  # 避免生产/预览环境被旧路径或临时表格环境变量悄悄劫持。
  return [File.expand_path(override)] if ENV['AOO_ROOM_RULE_TEST_MODE'] == '1'
  raise "正式 room-rules watcher 禁止覆盖规则表路径: #{override}"
end

def workbooks
  resolve_workbooks
end

def log(message)
  puts("#{Time.now.strftime('%Y-%m-%d %H:%M:%S')} #{utf8(message)}")
  $stdout.flush
end

def utf8(value)
  # 子进程输出由 Open3 以 ASCII-8BIT 返回。必须先规范化再与中文运维文案拼接，
  # 否则失败日志本身会让常驻 watcher 崩溃，Excel 保存后的下一轮同步就会中断。
  value.to_s.encode(Encoding::UTF_8, invalid: :replace, undef: :replace)
end

def workbook_snapshot(workbook)
  return nil unless File.file?(workbook)
  first = File.stat(workbook)
  first_hash = Digest::SHA256.file(workbook).hexdigest
  sleep(STABLE_SECONDS)
  second = File.stat(workbook)
  second_hash = Digest::SHA256.file(workbook).hexdigest
  return nil unless first.size == second.size && first.mtime == second.mtime && first_hash == second_hash
  # xlsx 是 zip 容器；完整性校验避免在 Excel 写入窗口读取半文件。
  return nil unless system('unzip', '-tqq', workbook, out: File::NULL, err: File::NULL)
  { 'sourceHash' => second_hash, 'size' => second.size, 'mtime' => second.mtime.to_f }
rescue Errno::ENOENT, IOError
  nil
end

def generated_path(workbook)
  override = ENV['AOO_ROOM_RULE_GENERATED'].to_s
  return File.expand_path(override) if ENV['AOO_ROOM_RULE_TEST_MODE'] == '1' && !override.empty?
  File.join(SERVER_ROOT, "release-input/room-rules/#{File.basename(workbook, '.xlsx')}.generated.json")
end
def state_path(workbook)
  override = ENV['AOO_ROOM_RULE_STATE'].to_s
  return File.expand_path(override) if ENV['AOO_ROOM_RULE_TEST_MODE'] == '1' && !override.empty?
  File.join(SERVER_ROOT, "work/local-runtime/room-rules-#{File.basename(workbook, '.xlsx')}.json")
end
def current_published_hash(workbook)
  generated = generated_path(workbook)
  return nil unless File.file?(generated)
  JSON.parse(File.read(generated)).fetch('sourceHash')
rescue JSON::ParserError, KeyError
  nil
end

def last_confirmed_hash(workbook)
  state = state_path(workbook)
  return nil unless File.file?(state)
  JSON.parse(File.read(state)).fetch('sourceHash')
rescue JSON::ParserError, KeyError
  nil
end

def write_state(workbook, payload)
  state = state_path(workbook)
  FileUtils.mkdir_p(File.dirname(state))
  temporary = "#{state}.tmp-#{Process.pid}"
  File.write(temporary, JSON.pretty_generate(payload) + "\n")
  # 状态包含本机发布路径与时间戳；原子替换前先收紧权限，避免 rename
  # 把进程 umask 产生的 0644 文件暴露给同机其他账号。
  File.chmod(0o600, temporary)
  File.rename(temporary, state)
ensure
  File.delete(temporary) if defined?(temporary) && File.exist?(temporary)
end

def synchronize(workbook, snapshot)
  env = {'AOO_ROOM_RULE_WORKBOOK'=>workbook, 'AOO_ROOM_RULE_GENERATED'=>generated_path(workbook)}
  stdout, stderr, status = Open3.capture3(env, PUBLISH, 'local', chdir: SERVER_ROOT)
  unless status.success?
    detail = utf8(stderr.empty? ? stdout : stderr)
    log("#{File.basename(workbook)} 同步失败，继续使用上一发布版本 sourceHash=#{last_confirmed_hash(workbook) || 'none'}: #{detail}")
    return false
  end
  after = workbook_snapshot(workbook)
  unless after && after['sourceHash'] == snapshot['sourceHash']
    log('发布期间 Excel 再次变化，本轮结果不记为成功并立即重新同步')
    return false
  end
  unless current_published_hash(workbook) == snapshot['sourceHash']
    log('生成物哈希与稳定 Excel 不一致，拒绝确认发布')
    return false
  end
  write_state(workbook, snapshot.merge('publishedAt' => Time.now.utc.iso8601))
  log("#{File.basename(workbook)} 同步成功 sourceHash=#{snapshot['sourceHash']}")
  true
end

require 'time'
once = ARGV.include?('--once')
last_attempted_hashes = {}
if ARGV.include?('--check-identities')
  all_plays = load_registered_plays
  unregistered = verify_official_rule_directory!(all_plays)
  active = registered_workbooks
  puts JSON.generate({ 'watchableWorkbookCount'=>active.length, 'unregisteredRuleWorkbooks'=>unregistered })
  exit 0
end
workbooks.each { |workbook| log("监听启动 workbook=#{workbook} generated=#{generated_path(workbook)} confirmedSourceHash=#{last_confirmed_hash(workbook) || 'none'}") }
loop do
  workbooks.each do |workbook|
    snapshot = workbook_snapshot(workbook)
    if snapshot && snapshot['sourceHash'] != last_confirmed_hash(workbook) && snapshot['sourceHash'] != last_attempted_hashes[workbook]
      last_attempted_hashes[workbook] = snapshot['sourceHash']
      last_attempted_hashes.delete(workbook) unless synchronize(workbook, snapshot)
    end
  end
  break if once
  sleep(POLL_SECONDS)
end
