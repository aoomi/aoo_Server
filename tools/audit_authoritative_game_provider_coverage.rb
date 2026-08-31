#!/usr/bin/env ruby
# frozen_string_literal: true

require 'fileutils'
require 'json'
require 'time'

ROOT = File.expand_path('..', __dir__)
SERVER = File.join(ROOT, 'server')
OUTPUT = File.join(ROOT, 'work', 'audit', 'authoritative-game-provider-coverage.json')

# Only concrete providers registered for production discovery are in scope.  The
# previous filename glob also counted GameProvider itself and category mixins,
# while missing inherited implementations (for example PokerGameProvider's
# command handler).  That made the gate report adapter gaps which did not exist.
service_files = Dir.glob(File.join(SERVER, '**', 'src/main/resources/META-INF/services/com.aoo.bcg.gamespi.GameProvider'))
registered_declarations = service_files.flat_map do |path|
  File.readlines(path, chomp: true, encoding: 'UTF-8')
      .map(&:strip).reject { |line| line.empty? || line.start_with?('#') }
end
registered_classes = registered_declarations.uniq
duplicate_registrations = registered_declarations.each_with_object(Hash.new(0)) { |name, counts| counts[name] += 1 }
                                                .select { |_name, count| count > 1 }
provider_files = registered_classes.map do |class_name|
  simple_name = class_name.split('.').last
  Dir.glob(File.join(SERVER, '**', "#{simple_name}.java")).reject { |path| path.include?('/test/') }.first
end.compact
requirements = {
  providerInterface: /implements\s+\w*GameProvider/,
  commandHandler: /commandHandler\s*\(/,
  createAuthority: /roomFactory\s*\(/,
  restoreAuthority: /restoreAuthoritativeSession\s*\(|StateRestorer/,
  reconnectPerspective: /reconnectViewProvider\s*\(/,
  settlement: /settlementProvider\s*\(/
}

providers = provider_files.map do |path|
  text = File.read(path, encoding: 'UTF-8').encode('UTF-8', invalid: :replace, undef: :replace, replace: '')
  inherited = []
  if text.match?(/implements\s+PokerGameProvider/)
    inherited << File.join(SERVER, 'Poker/src/main/java/com/aoo/bcg/poker/PokerGameProvider.java')
  end
  if text.match?(/implements\s+MahjongGameProvider/)
    inherited << File.join(SERVER, 'Mahjong/src/main/java/com/aoo/bcg/mahjong/MahjongGameProvider.java')
  end
  module_root = path.split('/src/').first
  module_sources = Dir.glob(File.join(module_root, '**/*.java'))
                      .reject { |candidate| candidate.include?('/target/') || candidate.include?('/test/') }
  effective_text = (module_sources + inherited).uniq.select { |candidate| File.file?(candidate) }
                                     .map { |candidate| File.read(candidate, encoding: 'UTF-8') }.join("\n")
  checks = requirements.to_h { |name, pattern| [name, name == :providerInterface || effective_text.match?(pattern)] }
  {
    path: path.delete_prefix(ROOT + '/'),
    checks: checks,
    authoritativeComplete: checks.values.all?,
    missing: checks.select { |_name, present| !present }.keys
  }
end

complete = providers.count { |provider| provider[:authoritativeComplete] }
report = {
  generatedAt: Time.now.utc.iso8601,
  invariant: 'Every production game provider owns command validation, authority creation/restoration, perspective reconnect and settlement.',
  providerCount: providers.length,
  authoritativeCompleteCount: complete,
  incompleteCount: providers.length - complete,
  registeredClasses: registered_classes,
  duplicateServiceLoaderDeclarations: duplicate_registrations,
  unresolvedRegisteredClasses: registered_classes.length - provider_files.length,
  providers: providers,
  passed: !providers.empty? && complete == providers.length && duplicate_registrations.empty? &&
          registered_classes.length == provider_files.length
}
FileUtils.mkdir_p(File.dirname(OUTPUT))
File.write(OUTPUT, JSON.pretty_generate(report) + "\n")
puts JSON.generate(providerCount: providers.length, complete: complete,
                   incomplete: providers.length - complete,
                   duplicateServiceLoaderDeclarations: duplicate_registrations, passed: report[:passed])
exit(report[:passed] ? 0 : 1)
