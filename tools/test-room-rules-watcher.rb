#!/usr/bin/env ruby
require 'json'
require 'digest'
require 'fileutils'
require 'open3'
require 'tmpdir'

root = File.expand_path('..', __dir__)
project_root = File.expand_path('..', root)
watcher = File.read(File.join(root, 'tools/watch-room-rules.rb'))
publisher = File.read(File.join(root, 'tools/room-rules-publisher.rb'))
service = File.read(File.join(root, 'tools/local-dev-services.sh'))
publish = File.read(File.join(root, 'tools/publish-room-rules.sh'))
client_gitignore = File.read(File.join(project_root, 'Client/.gitignore'), encoding: 'UTF-8')
official_workbook = 'Client/docs/开房规则表/跑得快/成都跑得快.xlsx'
stale_workbook = ['玩法文档', '跑得快', '成都跑得快开房规则表.xlsx'].join('/')

checks = {
  'watcher discovers registered regional workbooks only' => watcher.include?("Client/docs/开房规则表/跑得快") &&
    watcher.include?('room-rule-play-identities.json') && watcher.include?("play.fetch('workbook')"),
  'publisher uses current official Chengdu workbook path' => publisher.include?(official_workbook),
  'stale Chengdu workbook path is not accepted by tools' => ![watcher, publisher, service, publish].any? { |content| content.include?(stale_workbook) },
  'formal watcher rejects workbook path overrides outside tests' => watcher.include?('AOO_ROOM_RULE_TEST_MODE') &&
    watcher.include?('正式 room-rules watcher 禁止覆盖规则表路径'),
  'watcher validates xlsx zip before publishing' => watcher.include?("system('unzip', '-tqq'"),
  'watcher requires two matching hashes' => watcher.include?("first_hash == second_hash"),
  'watcher retains last good version on failure' => watcher.include?('继续使用上一发布版本'),
  'watcher normalizes subprocess output to UTF-8' => watcher.include?('encode(Encoding::UTF_8'),
  'local lifecycle launches watcher under UTF-8 locale' => service.include?('export LANG=${LANG:-en_US.UTF-8}') &&
    service.include?('export LC_ALL=${LC_ALL:-en_US.UTF-8}'),
  'local lifecycle falls back when launchctl is unavailable' => service.include?('降级为 nohup 后台进程'),
  'launchd publisher resolves Docker outside interactive PATH' => publish.include?('resolve_docker') &&
    publish.include?('/Applications/Docker.app/Contents/Resources/bin/docker'),
  'watcher state is owner-only before atomic publish' => watcher.include?('File.chmod(0o600, temporary)'),
  'watcher is idempotent by confirmed source hash' => watcher.include?("snapshot['sourceHash'] != last_confirmed_hash"),
  'watcher retries generated but unconfirmed publications' => watcher.include?("snapshot['sourceHash'] != last_confirmed_hash"),
  'publisher writes generated output atomically' => publisher.include?('File.rename(temporary, OUTPUT)'),
  'publisher locates the unique current six-column header' => publisher.include?('一组六列表头') &&
    publisher.include?('托管次数') && publisher.include?('是否显示'),
  'publisher allocates persistent keys without rule-name code maps' => publisher.include?('AOO_ROOM_RULE_REGISTRY') &&
    publisher.include?('allocate_field_key!') && !publisher.include?('FIELD_KEYS_BY_CODE') &&
    !publisher.include?('FIELD_LABEL_ALIASES') && !publisher.include?('PLAY_IDENTITIES') &&
    publisher.include?('AOO_ROOM_RULE_IDENTITY_REGISTRY'),
  'local lifecycle starts supervised watcher' => service.include?('start_rule_watcher') && service.include?('<key>KeepAlive</key><true/>'),
  'supervised watcher refreshes its pid on every launch' => service.include?('echo $$ > %q'),
  'production remains an explicit release action' => File.read(File.join(root, 'tools/publish-room-rules.sh')).include?('生产发布必须显式指定'),
  'Creator preview cache is excluded from source handoff' => client_gitignore.lines.map(&:strip).include?('/temp/')
}
failed = checks.reject { |_, passed| passed }
abort("room-rule watcher checks failed: #{failed.keys.join(', ')}") unless failed.empty?

# 使用权威表的只读副本验证损坏文件 fail-closed，绝不改写用户 Excel。
Dir.mktmpdir('aoo-room-rule-watcher-test') do |directory|
  workbook = File.join(directory, 'rules.xlsx')
  generated = File.join(directory, 'generated.json')
  state = File.join(directory, 'state.json')
  fake_publisher = File.join(directory, 'publish.sh')
  source = File.expand_path('../Client/docs/开房规则表/跑得快/成都跑得快.xlsx', root)
  FileUtils.cp(source, workbook)
  expected_hash = Digest::SHA256.file(workbook).hexdigest
  File.write(fake_publisher, <<~SH)
    #!/usr/bin/env bash
    ruby -rdigest -rjson -e 'File.write(ENV.fetch("AOO_ROOM_RULE_GENERATED"), JSON.generate({sourceHash: Digest::SHA256.file(ENV.fetch("AOO_ROOM_RULE_WORKBOOK")).hexdigest}))'
  SH
  FileUtils.chmod(0o700, fake_publisher)
  environment = {
    'AOO_ROOM_RULE_WORKBOOK'=>workbook, 'AOO_ROOM_RULE_GENERATED'=>generated,
    'AOO_ROOM_RULE_STATE'=>state, 'AOO_ROOM_RULE_PUBLISHER'=>fake_publisher,
    'AOO_ROOM_RULE_STABLE_SECONDS'=>'0.01', 'AOO_ROOM_RULE_TEST_MODE'=>'1'
  }
  _, stderr, status = Open3.capture3(environment, 'ruby', File.join(root, 'tools/watch-room-rules.rb'), '--once')
  abort("stable workbook sync failed: #{stderr}") unless status.success? && JSON.parse(File.read(generated))['sourceHash'] == expected_hash
  last_good = File.binread(generated)
  File.binwrite(workbook, 'not-an-xlsx')
  _, stderr, status = Open3.capture3(environment, 'ruby', File.join(root, 'tools/watch-room-rules.rb'), '--once')
  abort("damaged workbook changed last good output: #{stderr}") unless status.success? && File.binread(generated) == last_good

  File.binwrite(workbook, File.binread(source))
  File.write(fake_publisher, <<~SH)
    #!/usr/bin/env bash
    printf '\\377发布失败\\n' >&2
    exit 1
  SH
  _, stderr, status = Open3.capture3(environment, 'ruby', File.join(root, 'tools/watch-room-rules.rb'), '--once')
  abort("non-UTF8 publisher output crashed watcher: #{stderr}") unless status.success? && File.binread(generated) == last_good
end
puts JSON.generate(checks)
