#!/usr/bin/env ruby
# frozen_string_literal: true

require 'fileutils'
require 'json'
require 'time'

ROOT = File.expand_path('..', __dir__)
CLIENT = File.expand_path('../Client', ROOT)
OUTPUT = File.join(ROOT, 'work/audit/v07-all-games.json')
SERVICE = 'src/main/resources/META-INF/services/com.aoo.bcg.gamespi.GameProvider'
STAGES = {
  create: /roomFactory\s*\(|RoomCreationContext/,
  join: /\bjoin\s*\(|EnterRoom|GetRoomInfo/,
  ready: /\bready\b|ReadyRoom/i,
  start: /\bstart\s*\(|StartGame/,
  operation: /\boperate\s*\(|OpCard|CommandHandler/,
  turn: /operatorSeat|currentSeat|turn/i,
  reconnect: /reconnect|viewFor/i,
  settlement: /settlement/i,
  replay: /\breplay\b|eventReplay|restore/i
}.freeze

service_files = Dir.glob(File.join(ROOT, 'server/**', SERVICE)).sort
providers = service_files.flat_map do |service_file|
  module_root = service_file.split('/src/main/resources/').first
  File.readlines(service_file, chomp: true, encoding: 'UTF-8').map(&:strip)
      .reject { |line| line.empty? || line.start_with?('#') }.map do |class_name|
    simple = class_name.split('.').last
    source = Dir.glob(File.join(module_root, '**', "#{simple}.java")).reject { |p| p.include?('/test/') }.first
    tests = Dir.glob(File.join(module_root, '**', '*Test.java')).reject { |p| p.include?('/target/') }
    source_text = source && File.file?(source) ? File.read(source, encoding: 'UTF-8') : ''
    module_source_text = Dir.glob(File.join(module_root, '**/*.java'))
                            .reject { |p| p.include?('/target/') || p.include?('/test/') }
                            .map { |p| File.read(p, encoding: 'UTF-8') }.join("\n")
    test_text = tests.map { |p| File.read(p, encoding: 'UTF-8') }.join("\n")
    evidence = module_source_text + "\n" + test_text
    stages = STAGES.transform_values { |pattern| evidence.match?(pattern) }
    reports = Dir.glob(File.join(module_root, 'target/surefire-reports/TEST-*.xml'))
    failures = reports.sum { |p| File.read(p, encoding: 'UTF-8')[/failures="(\d+)"/, 1].to_i }
    errors = reports.sum { |p| File.read(p, encoding: 'UTF-8')[/errors="(\d+)"/, 1].to_i }
    code = source_text[/new\s+GameDescriptor\s*\([^;]*?"([a-z0-9_]+)"/m, 1] || simple.sub(/GameProvider$/, '').downcase
    {
      code: code, className: class_name, source: source&.delete_prefix(ROOT + '/'),
      tests: tests.map { |p| p.delete_prefix(ROOT + '/') }, stages: stages,
      missingStages: stages.select { |_stage, ok| !ok }.keys,
      testReports: reports.map { |p| p.delete_prefix(ROOT + '/') },
      testsPassed: !reports.empty? && failures.zero? && errors.zero?
    }
  end
end

catalog = File.join(ROOT, 'server/Bootstrap/src/main/resources/game-catalog-528.tsv')
catalog_rows = File.file?(catalog) ? File.readlines(catalog, chomp: true, encoding: 'UTF-8').drop(1).reject { |l| l.empty? || l.start_with?('#') } : []
bridge_source = File.join(ROOT, 'server/Bootstrap/src/main/java/com/aoo/bcg/bootstrap/CatalogGameProvider.java')
catalog_codes = catalog_rows.map { |row| row.split("\t", -1)[1].to_s.downcase }.reject(&:empty?)
native_codes = providers.map { |provider| provider[:code].downcase }
metadata_only_codes = File.file?(bridge_source) ? catalog_codes.reject { |code| native_codes.include?(code) } : []
bridge_count = metadata_only_codes.length

adapter_files = Dir.glob(File.join(CLIENT, 'assets/**/*NetworkAdapter.ts')).sort.select do |path|
  # Compatibility re-exports are aliases, not a second adapter implementation.
  File.read(path, encoding: 'UTF-8').match?(/\bclass\s+\w+NetworkAdapter\b/)
end
adapters = adapter_files.map { |p| p.delete_prefix(CLIENT + '/') }
adapter_codes = adapters.map { |p| File.basename(p, 'NetworkAdapter.ts').downcase }
duplicates = adapter_codes.each_with_object(Hash.new(0)) { |code, counts| counts[code] += 1 }
                          .select { |_code, count| count > 1 }

report = {
  schemaVersion: 1, generatedAt: Time.now.utc.iso8601,
  invariant: 'A game is accepted only with executable production source and evidence for create, join, ready, start, operation, turn, reconnect, settlement and replay. Catalog metadata is never executable-game evidence.',
  providers: providers,
  summary: {
    registeredNativeProviders: providers.length,
    providersWithAllStageEvidence: providers.count { |p| p[:missingStages].empty? },
    providersWithPassingTestReports: providers.count { |p| p[:testsPassed] },
    catalogRows: catalog_rows.length,
    metadataOnlyCatalogEntries: bridge_count,
    clientNetworkAdapters: adapters.length,
    duplicateClientAdapterCodes: duplicates
  },
  metadataOnlyCatalogCodes: metadata_only_codes,
  clientAdapters: adapters,
  blockers: [
    ("#{bridge_count} catalog entries are metadata-only and have no registered native provider" if bridge_count.positive?),
    ('one or more native providers lack lifecycle-stage evidence' if providers.any? { |p| !p[:missingStages].empty? }),
    ('one or more native providers lack a current passing Surefire report' if providers.any? { |p| !p[:testsPassed] }),
    ('duplicate client network adapters exist for the same game code' unless duplicates.empty?)
  ].compact,
  passed: bridge_count.zero? && providers.all? { |p| p[:missingStages].empty? && p[:testsPassed] } && duplicates.empty?
}
FileUtils.mkdir_p(File.dirname(OUTPUT))
File.write(OUTPUT, JSON.pretty_generate(report) + "\n")
puts JSON.generate(report[:summary].merge(passed: report[:passed], blockers: report[:blockers]))
exit(report[:passed] ? 0 : 1)
