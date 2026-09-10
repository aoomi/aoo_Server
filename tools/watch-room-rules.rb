#!/usr/bin/env ruby
require 'digest'
require 'fileutils'
require 'json'
require 'open3'

SERVER_ROOT = File.expand_path('..', __dir__)
PROJECT_ROOT = File.expand_path('..', SERVER_ROOT)
WORKBOOK_DIRECTORY = File.join(PROJECT_ROOT, 'Client/docs/开房规则表/跑得快')
IDENTITY_REGISTRY = File.join(SERVER_ROOT, 'tools/room-rule-play-identities.json')
PUBLISH = ENV.fetch('AOO_ROOM_RULE_PUBLISHER', File.join(SERVER_ROOT, 'tools/publish-room-rules.sh'))
POLL_SECONDS = Float(ENV.fetch('AOO_ROOM_RULE_POLL_SECONDS', '1'))
STABLE_SECONDS = Float(ENV.fetch('AOO_ROOM_RULE_STABLE_SECONDS', '0.6'))

def resolve_workbooks
  override = ENV['AOO_ROOM_RULE_WORKBOOK'].to_s
  if override.empty?
    payload = JSON.parse(File.read(IDENTITY_REGISTRY, encoding: 'UTF-8'))
    return payload.fetch('plays').map { |play| File.join(WORKBOOK_DIRECTORY, play.fetch('workbook')) }.sort
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
  File.join(SERVER_ROOT, "work/generated/room-rules/#{File.basename(workbook, '.xlsx')}.generated.json")
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
