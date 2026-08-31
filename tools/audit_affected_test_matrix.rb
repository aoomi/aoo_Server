#!/usr/bin/env ruby
# frozen_string_literal: true

require 'fileutils'
require 'json'
require 'time'

ROOT = File.expand_path('..', __dir__)
SERVER = File.join(ROOT, 'server')
CLIENT = File.expand_path('../Client', ROOT)
OUTPUT = File.join(ROOT, 'work', 'audit', 'affected-test-matrix.json')

modules = Dir.children(SERVER).sort.map do |name|
  root = File.join(SERVER, name)
  next unless File.directory?(root)
  sources = Dir.glob(File.join(root, '**', '*.java')).reject { |path| path.include?('/test/') || path.include?('/target/') }
  next if sources.empty?
  reports = Dir.glob(File.join(root, '**', 'target', 'surefire-reports', '*.txt'))
  newest_source = sources.map { |path| File.mtime(path) }.max
  newest_report = reports.map { |path| File.mtime(path) }.max
  failures = reports.count do |path|
    File.read(path, encoding: 'UTF-8').match?(/Failures:\s*[1-9]|Errors:\s*[1-9]/)
  rescue ArgumentError
    true
  end
  {
    module: name,
    productionSources: sources.length,
    testReports: reports.length,
    reportFailures: failures,
    sourceChangedAfterLastReport: newest_report.nil? || newest_source > newest_report,
    covered: !reports.empty? && failures.zero? && newest_source <= newest_report
  }
end.compact

client_tests = Dir.glob(File.join(CLIENT, 'assets', '**', '*.{test,spec}.ts'))
client_ui = Dir.glob(File.join(CLIENT, 'assets', '**', '*.ts')).count do |path|
  File.read(path, encoding: 'UTF-8').match?(/Button|click|touch|UITransform|Label|Sprite/)
rescue ArgumentError
  false
end
uncovered = modules.reject { |entry| entry[:covered] }
report = {
  generatedAt: Time.now.utc.iso8601,
  invariant: 'Every affected server module has a newer passing report and client UI has executable interaction tests.',
  serverModuleCount: modules.length,
  coveredServerModules: modules.length - uncovered.length,
  uncoveredServerModules: uncovered.length,
  clientUiSourceCount: client_ui,
  clientInteractionTestCount: client_tests.length,
  passed: uncovered.empty? && client_ui.positive? && client_tests.any?,
  serverModules: modules,
  clientTests: client_tests.map { |path| path.delete_prefix(File.dirname(ROOT) + '/') }
}
FileUtils.mkdir_p(File.dirname(OUTPUT))
File.write(OUTPUT, JSON.pretty_generate(report) + "\n")
puts JSON.generate(serverModules: modules.length, covered: modules.length - uncovered.length,
                   uncovered: uncovered.length, clientUiSources: client_ui,
                   clientInteractionTests: client_tests.length, passed: report[:passed])
exit(report[:passed] ? 0 : 1)
