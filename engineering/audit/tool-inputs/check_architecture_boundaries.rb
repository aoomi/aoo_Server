#!/usr/bin/env ruby
# frozen_string_literal: true

require 'pathname'
require 'rexml/document'
require 'json'

root = Pathname.new(ARGV[0] || File.expand_path('../../..', __dir__)).realpath
root_pom = REXML::Document.new(File.read(root.join('pom.xml'), encoding: 'UTF-8'))
pom_paths = REXML::XPath.match(root_pom, '/*[local-name()="project"]/*[local-name()="modules"]/*[local-name()="module"]').map do |entry|
  root.join(entry.text.strip, 'pom.xml').to_s
end.sort
modules = {}
errors = []
growth = JSON.parse(File.read(root.join('config/architecture-directory-baseline.json')))
extensions = growth.fetch('countedExtensions')
maximum_files = growth.fetch('maximumFiles')
scope_mode = growth.fetch('scopeMode', 'recursive')
unless %w[recursive deepest-registered].include?(scope_mode)
  errors << "unsupported directory baseline scopeMode: #{scope_mode}"
end

# A Games category may contain nested concrete gameplay modules.  In the
# deepest-registered mode each file belongs to the deepest configured baseline
# directory; otherwise a category would count the same source once at the
# category level and again at every nested gameplay module.  Unregistered
# subdirectories intentionally remain owned by their nearest registered parent
# so that omissions fail closed instead of disappearing from the gate.
scope_roots = {}
maximum_files.each_key do |relative|
  unless relative.is_a?(String)
    errors << "invalid directory baseline path: #{relative.inspect}"
    next
  end
  candidate = Pathname.new(relative)
  if candidate.absolute? || candidate.each_filename.any? { |part| part == '..' }
    errors << "directory baseline path escapes root: #{relative}"
    next
  end
  normalized = candidate.cleanpath.to_s
  if scope_roots.key?(normalized) && scope_roots[normalized] != relative
    errors << "duplicate directory baseline path: #{scope_roots[normalized]} and #{relative}"
    next
  end
  scope_path = root.join(normalized).cleanpath
  unless scope_path.directory?
    errors << "directory baseline path missing: #{relative}"
    next
  end
  scope_roots[normalized] = relative
end

maximum_files.each do |relative, maximum|
  normalized_relative = relative.is_a?(String) ? Pathname.new(relative).cleanpath.to_s : nil
  next unless normalized_relative && scope_roots[normalized_relative] == relative
  unless maximum.is_a?(Integer) && maximum >= 0
    errors << "invalid directory baseline maximum: #{relative}=#{maximum.inspect}"
    next
  end
  relative_scope = normalized_relative
  count = Dir.glob(root.join(relative_scope, '**/*')).count do |path|
    next false unless File.file?(path) && extensions.include?(File.extname(path)) && !path.include?('/target/')
    if scope_mode == 'deepest-registered'
      nested = scope_roots.keys.any? do |nested_scope|
        nested_scope != relative_scope &&
          nested_scope.start_with?("#{relative_scope}/") && path.start_with?("#{root.join(nested_scope).cleanpath}/")
      end
      next false if nested
    end
    true
  end
  errors << "directory growth exceeds baseline: #{relative}=#{count}>#{maximum}" if count > maximum
end
concrete_games = %w[cdxzmj njpdk scjymj xcpdk zjh zypk]
game_categories = {
  'cdxzmj' => 'game-category-mahjong',
  'scjymj' => 'game-category-mahjong',
  'njpdk' => 'game-category-poker',
  'xcpdk' => 'game-category-poker',
  'zjh' => 'game-category-poker',
  'zypk' => 'game-category-poker'
}

pom_paths.each do |path|
  document = REXML::Document.new(File.read(path, encoding: 'UTF-8'))
  artifact = REXML::XPath.first(document, '/*[local-name()="project"]/*[local-name()="artifactId"]')&.text
  dependencies = REXML::XPath.match(document, '//*[local-name()="dependency"]/*[local-name()="artifactId"]').map(&:text)
  modules[artifact] = { path: path, dependencies: dependencies }
end

real_paths = Hash.new { |hash, key| hash[key] = [] }
pom_paths.each { |path| real_paths[Pathname.new(File.dirname(path)).realpath.to_s] << path }
real_paths.each_value { |paths| errors << "duplicate physical module path: #{paths.join(', ')}" if paths.size > 1 }

hall = modules['game-hall']
if hall
  forbidden = hall[:dependencies] & (concrete_games + %w[games])
  errors << "game-hall depends on concrete games: #{forbidden.join(', ')}" unless forbidden.empty?
end

bootstrap = modules['game-bootstrap']
# Bootstrap is the composition root.  Direct dependencies on concrete games are
# intentional: they put the provider JARs on the production classpath so the
# ServiceLoader call in Bootstrap can discover them.

# Enforce the inward dependency direction on the architectural core.  The
# composition root is deliberately excluded; every other module is forbidden
# from acquiring a concrete game implementation.
modules.each do |artifact, info|
  next if artifact == 'game-bootstrap' || concrete_games.include?(artifact)
  forbidden = info[:dependencies] & concrete_games
  errors << "#{artifact} depends on concrete games: #{forbidden.join(', ')}" unless forbidden.empty?
end

allowed_core_dependencies = {
  'game-spi' => [],
  'game-common' => %w[game-spi],
  'game-category-mahjong' => %w[game-spi game-common],
  'game-category-poker' => %w[game-spi game-common],
  'game-category-long-card' => %w[game-spi game-common],
  'game-category-word-card' => %w[game-spi game-common],
  'game-families' => %w[game-spi game-common game-category-mahjong game-category-poker game-category-long-card game-category-word-card]
}
allowed_core_dependencies.each do |artifact, allowed|
  next unless modules[artifact]
  internal = modules[artifact][:dependencies].select { |dependency| modules.key?(dependency) }
  reverse = internal - allowed
  errors << "#{artifact} has reverse/outward dependencies: #{reverse.join(', ')}" unless reverse.empty?
end

categories = %w[game-category-mahjong game-category-poker game-category-long-card game-category-word-card]
categories.each do |artifact|
  next unless modules[artifact]
  forbidden = modules[artifact][:dependencies] & (categories - [artifact] + %w[game-families games game-hall] + concrete_games)
  errors << "#{artifact} has forbidden dependencies: #{forbidden.join(', ')}" unless forbidden.empty?
end

concrete_games.each do |artifact|
  next unless modules[artifact]
  forbidden = modules[artifact][:dependencies] & (concrete_games - [artifact])
  errors << "#{artifact} depends on another concrete game: #{forbidden.join(', ')}" unless forbidden.empty?
end

game_categories.each do |artifact, category|
  next unless modules[artifact]
  dependencies = modules[artifact][:dependencies]
  errors << "#{artifact} must depend on game-common" unless dependencies.include?('game-common')
  errors << "#{artifact} must depend on #{category}" unless dependencies.include?(category)
end

concrete_games.each do |artifact|
  module_info = modules[artifact]
  next unless module_info
  module_root = Pathname.new(File.dirname(module_info[:path]))
  provider_file = module_root.join('src/main/resources/META-INF/services/com.aoo.bcg.gamespi.GameProvider')
  errors << "#{artifact} has no GameProvider ServiceLoader registration" unless provider_file.file? && !provider_file.read.strip.empty?
end

authoritative_sources = %w[Games/Poker/PDK Games/Poker/ZJH].flat_map do |directory|
  Dir.glob(root.join("server/#{directory}/src/business/global/**/*.java"))
end
authoritative_sources.concat(Dir.glob(root.join('server/Games/Poker/ZYPK/src/business/global/pk/zypk/ZYPKTable.java')))
authoritative_sources.each do |path|
    source = File.read(path, encoding: 'UTF-8')
    if source.match?(/\b(?:new\s+Random\s*\(|Math\.random\s*\(|Collections\.shuffle\s*\()/)
      errors << "authoritative game code bypasses GameRandomSource: #{path}"
    end
    if source.match?(/public\s+(?:final\s+)?(?:ArrayList|List)<Integer>\s+privateCards\b/)
      errors << "authoritative private cards are publicly mutable: #{path}"
    end
end

concrete_dirs = {
  'CDXZMJ' => { path: 'Games/Mahjong/CDXZMJ', handler: 'chengdu1001' },
  'SCJYMJ' => { path: 'Games/Mahjong/SCJYMJ', handler: 'scjymj' },
  'XCPDK' => { path: 'Games/Poker/PDK', handler: 'xcpdk' }
}
concrete_dirs.each do |module_name, info|
  Dir.glob(root.join("server/#{info[:path]}/src/core/network/client2game/handler/#{info[:handler]}/*.java")).each do |path|
    next unless File.basename(path).match?(/(?:DissolveRoom(?:Agree|Refuse)?|UnReadyRoom|ReadyRoom|ContinueEnterRoom|EnterRoom|ExitRoom)\.java\z/)
    source = File.read(path, encoding: 'UTF-8')
    errors << "duplicated lifecycle implementation: #{path}" unless source.match?(/extends\s+CBase(?:DissolveRoom(?:Agree|Refuse)?|UnReadyRoom|ReadyRoom|ContinueEnterRoom|EnterRoom|GameExitRoom)\b/)
  end
end

social_handlers = /(?:Chat|Voice|SendGift|RoomInvitationList|RoomInvitationOperation|ChatMessageList)\.java\z/
social_bases = /extends\s+CBase(?:Chat|Voice|SendGift|RoomInvitationList|RoomInvitationOperation|ChatMessageList)\b/
concrete_dirs.each do |module_name, info|
  Dir.glob(root.join("server/#{info[:path]}/src/core/network/client2game/handler/#{info[:handler]}/*.java")).each do |path|
    next unless File.basename(path).match?(social_handlers)
    source = File.read(path, encoding: 'UTF-8')
    errors << "duplicated room social implementation: #{path}" unless source.match?(social_bases)
  end
end

mq_consumer_types = %w[EnterRoom ExitRoom ChangeRoom ContinueRoom]
concrete_dirs.each_key do |module_name|
  mq_consumer_types.each do |type|
      Dir.glob(root.join("server/#{concrete_dirs.fetch(module_name)[:path]}/src/business/rocketmq/**/#{module_name}#{type}Consumer.java")).each do |path|
      source = File.read(path, encoding: 'UTF-8')
      unless source.match?(/extends\s+Base#{type}Consumer\b/)
        errors << "room MQ consumer bypasses shared base: #{path}"
        next
      end
      delegates = source.scan(/super\.action\s*\(/).size
      statements = source.scan(/^\s*(?!package\b|import\b|public\s+class\b|public\s+void\b|@|\*|\/\*|\*\/|\{|\}|\/\/)([^\s].*;)\s*$/).flatten
      errors << "room MQ registration shell contains business logic: #{path}" unless delegates == 1 && statements.size == 1
    end
  end
end

Dir.glob(root.join('server/Bootstrap/src/**/*.java')).sort.each do |path|
  imports = File.read(path, encoding: 'UTF-8').scan(/^import\s+([^;]+);/).flatten
  forbidden = imports.select do |name|
    name.match?(%r{\Acore\.server\.(?:cdxzmj|njpdk|scjymj|xcpdk|zjh|zypk)\.}) ||
      name.match?(%r{\Abusiness\.global\.(?:mj|pk)\.(?:cdxzmj|njpdk|scjymj|xcpdk|zjh|zypk)\.})
  end
  errors << "Bootstrap imports concrete games in #{path}: #{forbidden.join(', ')}" unless forbidden.empty?
end

if errors.empty?
  puts "architecture-boundaries: passed (#{modules.size} modules)"
else
  warn errors.join("\n")
  exit 1
end
