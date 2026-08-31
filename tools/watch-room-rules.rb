#!/usr/bin/env ruby
require 'digest'
require 'fileutils'
require 'json'
require 'open3'

SERVER_ROOT = File.expand_path('..', __dir__)
PROJECT_ROOT = File.expand_path('..', SERVER_ROOT)
WORKBOOK = ENV.fetch('AOO_ROOM_RULE_WORKBOOK', File.join(PROJECT_ROOT, '玩法文档/跑得快/成都跑得快开房规则表.xlsx'))
GENERATED = ENV.fetch('AOO_ROOM_RULE_GENERATED', File.join(SERVER_ROOT, 'work/generated/room-rules/成都跑得快.generated.json'))
STATE = ENV.fetch('AOO_ROOM_RULE_STATE', File.join(SERVER_ROOT, 'work/local-runtime/room-rules-watcher.json'))
PUBLISH = ENV.fetch('AOO_ROOM_RULE_PUBLISHER', File.join(SERVER_ROOT, 'tools/publish-room-rules.sh'))
POLL_SECONDS = Float(ENV.fetch('AOO_ROOM_RULE_POLL_SECONDS', '1'))
STABLE_SECONDS = Float(ENV.fetch('AOO_ROOM_RULE_STABLE_SECONDS', '0.6'))

def log(message)
  # 子进程输出由 Open3 以 ASCII-8BIT 返回；发布器包含中文错误时，直接与
  # UTF-8 运维文案拼接会让常驻 watcher 崩溃，导致 Excel 保存后永远不发布。
  puts("#{Time.now.strftime('%Y-%m-%d %H:%M:%S')} #{message.to_s.encode(Encoding::UTF_8, invalid: :replace, undef: :replace)}")
  $stdout.flush
end

def workbook_snapshot
  return nil unless File.file?(WORKBOOK)
  first = File.stat(WORKBOOK)
  first_hash = Digest::SHA256.file(WORKBOOK).hexdigest
  sleep(STABLE_SECONDS)
  second = File.stat(WORKBOOK)
  second_hash = Digest::SHA256.file(WORKBOOK).hexdigest
  return nil unless first.size == second.size && first.mtime == second.mtime && first_hash == second_hash
  # xlsx 是 zip 容器；完整性校验避免在 Excel 写入窗口读取半文件。
  return nil unless system('unzip', '-tqq', WORKBOOK, out: File::NULL, err: File::NULL)
  { 'sourceHash' => second_hash, 'size' => second.size, 'mtime' => second.mtime.to_f }
rescue Errno::ENOENT, IOError
  nil
end

def current_published_hash
  return nil unless File.file?(GENERATED)
  JSON.parse(File.read(GENERATED)).fetch('sourceHash')
rescue JSON::ParserError, KeyError
  nil
end

def write_state(payload)
  FileUtils.mkdir_p(File.dirname(STATE))
  temporary = "#{STATE}.tmp-#{Process.pid}"
  File.write(temporary, JSON.pretty_generate(payload) + "\n")
  # 状态包含本机发布路径与时间戳；原子替换前先收紧权限，避免 rename
  # 把进程 umask 产生的 0644 文件暴露给同机其他账号。
  File.chmod(0o600, temporary)
  File.rename(temporary, STATE)
ensure
  File.delete(temporary) if defined?(temporary) && File.exist?(temporary)
end

def synchronize(snapshot)
  stdout, stderr, status = Open3.capture3(PUBLISH, 'local', chdir: SERVER_ROOT)
  unless status.success?
    detail = stderr.empty? ? stdout : stderr
    log("同步失败，继续使用上一发布版本 sourceHash=#{current_published_hash || 'none'}: #{detail}")
    return false
  end
  after = workbook_snapshot
  unless after && after['sourceHash'] == snapshot['sourceHash']
    log('发布期间 Excel 再次变化，本轮结果不记为成功并立即重新同步')
    return false
  end
  unless current_published_hash == snapshot['sourceHash']
    log('生成物哈希与稳定 Excel 不一致，拒绝确认发布')
    return false
  end
  write_state(snapshot.merge('publishedAt' => Time.now.utc.iso8601))
  log("同步成功 sourceHash=#{snapshot['sourceHash']}")
  true
end

require 'time'
once = ARGV.include?('--once')
last_attempted_hash = nil
loop do
  snapshot = workbook_snapshot
  if snapshot && snapshot['sourceHash'] != current_published_hash && snapshot['sourceHash'] != last_attempted_hash
    last_attempted_hash = snapshot['sourceHash']
    last_attempted_hash = nil unless synchronize(snapshot)
  end
  break if once
  sleep(POLL_SECONDS)
end
