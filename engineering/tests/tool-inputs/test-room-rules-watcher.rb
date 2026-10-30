#!/usr/bin/env ruby
require 'json'
require 'digest'
require 'fileutils'
require 'open3'
require 'tmpdir'

root = File.expand_path('../../..', __dir__)
project_root = File.expand_path('..', root)
watcher = File.read(File.join(root, 'engineering/release/tool-inputs/watch-room-rules.rb'))
publisher = File.read(File.join(root, 'engineering/release/tool-inputs/room-rules-publisher.rb'))
service = File.read(File.join(root, 'engineering/local-dev/tool-inputs/local-dev-services.sh'))
publish = File.read(File.join(root, 'engineering/release/tool-inputs/publish-room-rules.sh'))
client_gitignore = File.read(File.join(project_root, 'Client/.gitignore'), encoding: 'UTF-8')
official_workbook = 'Client/docs/开房规则表/跑得快/成都跑得快.xlsx'
canonical_mahjong_workbook = 'Client/docs/开房规则表/麻将/成都血战麻将.xlsx'
stale_workbook = ['玩法文档', '跑得快', '成都跑得快开房规则表.xlsx'].join('/')

checks = {
  'watcher recursively scans the complete official rules tree' =>
    watcher.include?("Client/docs/开房规则表") && watcher.include?("'**', '*.xlsx'") &&
    !watcher.include?('region_table =') && watcher.include?('--check-identities'),
  'watcher resolves workbook identity from exact worksheet cells and the canonical registry' =>
    watcher.include?("'source-identity'") && watcher.include?('plays_by_identity.fetch(identity, [])') &&
    watcher.include?('不猜 gameCode/gameId/playVersion'),
  'publisher exposes a read-only source identity probe' =>
    publisher.include?("command == 'source-identity'") && publisher.include?('JSON.generate(workbook_source_identity)') &&
    publisher.include?('玩法身份字段不得为空'),
  'publisher enforces explicit published source labels and filters all other rows' =>
    publisher.include?('publishedSourceLabels 缺少平台必填字段') &&
    publisher.include?('publishedSourceLabels 对应的工作簿行缺失') &&
    publisher.include?('next if allowed_labels && !allowed_labels.key?(label)'),
  'watcher discovers enabled registered workbooks across project directories' =>
    watcher.include?('room-rule-play-identities.json') && watcher.include?("play.fetch('workbookPath')") &&
    watcher.include?("play.fetch('publishingEnabled', true)") && watcher.include?('workbookPath 不得重复登记'),
  'publisher uses current official Chengdu workbook path' => publisher.include?(official_workbook),
  'active Mahjong rule table uses the actual Chinese workbook path' =>
    publisher.include?(canonical_mahjong_workbook) ||
    File.read(File.join(root, 'release-input/identity/room-rule-play-identities.json'), encoding: 'UTF-8').include?(canonical_mahjong_workbook),
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
  'publisher locates the unique current five-column header' => publisher.include?('一组五列表头') &&
    publisher.include?('HEADERS = %w[界面显示文字 控件类型 可选项显示文字 默认勾选 是否显示]'),
  'publisher allocates persistent keys without rule-name code maps' => publisher.include?('AOO_ROOM_RULE_REGISTRY') &&
    publisher.include?('allocate_field_key!') && !publisher.include?('FIELD_KEYS_BY_CODE') &&
    !publisher.include?('FIELD_LABEL_ALIASES') && !publisher.include?('PLAY_IDENTITIES') &&
    publisher.include?('AOO_ROOM_RULE_IDENTITY_REGISTRY'),
  'local lifecycle starts supervised watcher' => service.include?('start_rule_watcher') && service.include?('<key>KeepAlive</key><true/>'),
  'supervised watcher refreshes its pid on every launch' => service.include?('echo $$ > %q'),
  'production remains an explicit release action' => File.read(File.join(root, 'engineering/release/tool-inputs/publish-room-rules.sh')).include?('生产发布必须显式指定'),
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
  _, stderr, status = Open3.capture3(environment, 'ruby', File.join(root, 'engineering/release/tool-inputs/watch-room-rules.rb'), '--once')
  abort("stable workbook sync failed: #{stderr}") unless status.success? && JSON.parse(File.read(generated))['sourceHash'] == expected_hash
  last_good = File.binread(generated)
  File.binwrite(workbook, 'not-an-xlsx')
  _, stderr, status = Open3.capture3(environment, 'ruby', File.join(root, 'engineering/release/tool-inputs/watch-room-rules.rb'), '--once')
  abort("damaged workbook changed last good output: #{stderr}") unless status.success? && File.binread(generated) == last_good

  File.binwrite(workbook, File.binread(source))
  File.write(fake_publisher, <<~SH)
    #!/usr/bin/env bash
    printf '\\377发布失败\\n' >&2
    exit 1
  SH
  _, stderr, status = Open3.capture3(environment, 'ruby', File.join(root, 'engineering/release/tool-inputs/watch-room-rules.rb'), '--once')
  abort("non-UTF8 publisher output crashed watcher: #{stderr}") unless status.success? && File.binread(generated) == last_good
end

Dir.mktmpdir('aoo-room-rule-identity-probe') do |directory|
  source = File.expand_path('../Client/docs/开房规则表/跑得快/成都跑得快.xlsx', root)
  copy = File.join(directory, 'renamed.xlsx')
  FileUtils.cp(source, copy)
  stdout, stderr, status = Open3.capture3({ 'AOO_ROOM_RULE_WORKBOOK'=>copy }, 'ruby',
    File.join(root, 'engineering/release/tool-inputs/room-rules-publisher.rb'), 'source-identity')
  actual = JSON.parse(stdout) if status.success?
  expected = ['四川', '成都', '成都跑得快']
  abort("exact workbook identity probe failed: #{stderr}") unless status.success? && actual == expected
end

Dir.mktmpdir('aoo-room-rule-chengdu1001-preview') do |directory|
  workbook = File.expand_path('../Client/docs/开房规则表/麻将/成都血战麻将.xlsx', root)
  identities = File.join(root, 'release-input/identity/room-rule-play-identities.json')
  publisher_path = File.join(root, 'engineering/release/tool-inputs/room-rules-publisher.rb')
  before_hash = Digest::SHA256.file(workbook).hexdigest
  preview_path = File.join(directory, 'chengdu1001-preview.json')
  environment = {
    'AOO_ROOM_RULE_WORKBOOK'=>workbook,
    'AOO_ROOM_RULE_GENERATED'=>preview_path,
    'AOO_ROOM_RULE_REGISTRY'=>File.join(directory, 'chengdu1001.keys.json'),
    'AOO_ROOM_RULE_IDENTITY_REGISTRY'=>identities
  }
  stdout, stderr, status = Open3.capture3(environment, 'ruby', publisher_path, 'preview')
  abort("chengdu1001 preview failed: #{stderr}") unless status.success?
  File.write(preview_path, stdout)
  preview = JSON.parse(File.read(preview_path, encoding: 'UTF-8'))
  fields = preview.fetch('fields')
  expected_keys = %w[setCount playerNum]
  actual_keys = fields.map { |field| field.fetch('key') }
  abort("chengdu1001 preview published unexpected rule keys: #{actual_keys.inspect}") unless actual_keys == expected_keys
  abort('chengdu1001 preview identity mismatch') unless preview.dig('identity', 'gameId') == 1001 &&
    preview.dig('identity', 'gameCode') == 'chengdu1001' &&
    preview.dig('identity', 'playVersion') == 'chengdu1001-v1.0.0'
  abort('chengdu1001 published option values mismatch') unless fields.map { |field| field.fetch('options').map { |option| option.fetch('value') } } ==
    [[8, 12, 16], [2, 3, 4]]

  incomplete_identity = File.join(directory, 'identities-missing-required-label.json')
  payload = JSON.parse(File.read(identities, encoding: 'UTF-8'))
  play = payload.fetch('plays').find { |entry| entry.fetch('gameCode') == 'chengdu1001' }
  play.fetch('publishedSourceLabels').delete('人数')
  File.write(incomplete_identity, JSON.pretty_generate(payload) + "\n")
  negative_environment = environment.merge(
    'AOO_ROOM_RULE_IDENTITY_REGISTRY'=>incomplete_identity,
    'AOO_ROOM_RULE_REGISTRY'=>File.join(directory, 'negative.keys.json')
  )
  _negative_stdout, negative_stderr, negative_status = Open3.capture3(negative_environment, 'ruby', publisher_path, 'preview')
  abort('chengdu1001 publisher accepted an incomplete source-label allowlist') if negative_status.success?
  abort("chengdu1001 publisher did not fail closed on a missing required label: #{negative_stderr}") unless
    negative_stderr.include?('publishedSourceLabels 缺少平台必填字段')
  abort('chengdu1001 workbook changed during preview') unless Digest::SHA256.file(workbook).hexdigest == before_hash

  audit_stdout, audit_stderr, audit_status = Open3.capture3('ruby', File.join(root, 'engineering/release/tool-inputs/watch-room-rules.rb'), '--check-identities')
  abort("chengdu1001 watcher identity audit failed: #{audit_stderr}") unless audit_status.success?
  audit = JSON.parse(audit_stdout.lines.last)
  abort("chengdu1001 watcher identity was not registered: #{audit.inspect}") unless
    audit.fetch('watchableWorkbookCount') == 31 && audit.fetch('unregisteredRuleWorkbooks').empty?
end
puts JSON.generate(checks)
