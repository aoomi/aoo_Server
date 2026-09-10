#!/usr/bin/env ruby

server_root = File.expand_path('..', __dir__)
project_root = File.expand_path('..', server_root)
document_dir = File.join(project_root, 'Client/docs/开房规则表/跑得快')
allowed_extensions = %w[.md .markdown .xlsx .png .jpg .jpeg .webp].freeze
forbidden = Dir.children(document_dir).sort.filter do |name|
  path = File.join(document_dir, name)
  next false if File.directory?(path)
  extension = File.extname(name).downcase
  !allowed_extensions.include?(extension) && !name.casecmp('README').zero?
end
abort("跑得快人工文档目录混入机器产物: #{forbidden.join('、')}") unless forbidden.empty?

publisher = File.read(File.join(server_root, 'tools/room-rules-publisher.rb'), encoding: 'UTF-8')
watcher = File.read(File.join(server_root, 'tools/watch-room-rules.rb'), encoding: 'UTF-8')
official_workbook = 'Client/docs/开房规则表/跑得快/成都跑得快.xlsx'
stale_workbook = ['玩法文档', '跑得快', '成都跑得快开房规则表.xlsx'].join('/')
abort('规则发布器没有读取当前正式成都跑得快七列权威规则表') unless publisher.include?(official_workbook)
abort('规则监听器没有读取当前正式成都跑得快七列权威规则表') unless watcher.include?(official_workbook)
abort('房间规则工具仍引用已废弃的成都规则表旧路径') if [publisher, watcher].any? { |content| content.include?(stale_workbook) }
client_gitignore = File.read(File.join(project_root, 'Client/.gitignore'), encoding: 'UTF-8')
abort('Creator 预览缓存 Client/temp 未从源码交付范围排除') unless client_gitignore.lines.map(&:strip).include?('/temp/')
builder = File.read(File.join(project_root, 'work/spreadsheet-authoring/build_create_room_rules_workbook.mjs'), encoding: 'UTF-8')
authority_path = '/Client/docs/开房规则表/跑得快/跑得快创建房间规则表.xlsx'
abort('创建房间规则表示例生成器仍会覆盖用户文字权威源') if builder.include?(authority_path)
abort('创建房间规则表示例必须写入Server/work生成目录') unless builder.include?('/Server/work/generated/room-rules/跑得快创建房间规则表.template.xlsx')
old_generated_path = ['跑得快玩法规则', '.generated.json'].join
abort('规则发布器仍向人工文档目录写入 generated JSON') if publisher.match?(/RULES_DIR[^\n]*#{Regexp.escape(old_generated_path)}/)
abort('规则发布器生成物未迁入 Server/work/generated/room-rules') unless publisher.include?('work/generated/room-rules')
puts "跑得快人工文档目录卫生校验通过: #{Dir.children(document_dir).length} 个人工文件"
