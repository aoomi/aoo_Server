#!/usr/bin/env ruby
# frozen_string_literal: true

require 'fileutils'
require 'json'
require 'rexml/document'
require 'set'
require 'time'

ROOT = File.expand_path('..', __dir__)
SERVER = File.join(ROOT, 'server')
CLIENT = File.expand_path('../Client', ROOT)
OUTPUT = File.join(ROOT, 'work', 'audit', 'computed-regression-scope.json')

modules = {}
Dir.glob(File.join(SERVER, '**', 'pom.xml')).sort.each do |pom|
  begin
    document = REXML::Document.new(File.read(pom, encoding: 'UTF-8'))
    project = document.root
    artifact = REXML::XPath.first(project, "*[local-name()='artifactId']")&.text
    next if artifact.nil? || artifact.strip.empty?
    dependencies = REXML::XPath.match(project, ".//*[local-name()='dependencies']/*[local-name()='dependency']/*[local-name()='artifactId']")
                              .map { |node| node.text.to_s.strip }.reject(&:empty?).uniq
    root = File.dirname(pom)
    modules[artifact] = { artifactId: artifact, root: root, pom: pom, dependencies: dependencies }
  rescue REXML::ParseException
    next
  end
end

modules.each_value do |entry|
  sources = Dir.glob(File.join(entry[:root], '**', '*.java')).reject { |path| path.include?('/test/') || path.include?('/target/') }
  reports = Dir.glob(File.join(entry[:root], '**', 'target', 'surefire-reports', '*.txt'))
  entry[:sources] = sources
  entry[:changed] = if sources.empty?
                      false
                    elsif reports.empty?
                      true
                    else
                      sources.map { |path| File.mtime(path) }.max > reports.map { |path| File.mtime(path) }.max
                    end
end

explicit = ENV.fetch('AOO_CHANGED_FILES', '').split(',').map(&:strip).reject(&:empty?)
seeds = modules.values.select do |entry|
  entry[:changed] || explicit.any? { |path| File.expand_path(path, ROOT).start_with?(entry[:root] + '/') }
end.map { |entry| entry[:artifactId] }.to_set

reverse = Hash.new { |hash, key| hash[key] = Set.new }
modules.each_value do |entry|
  entry[:dependencies].each { |dependency| reverse[dependency] << entry[:artifactId] if modules.key?(dependency) }
end
affected = seeds.dup
queue = seeds.to_a
until queue.empty?
  dependency = queue.shift
  reverse[dependency].each do |consumer|
    next if affected.include?(consumer)
    affected << consumer
    queue << consumer
  end
end

seed_sources = seeds.flat_map { |artifact| modules.fetch(artifact)[:sources] }
protocol_ids = seed_sources.flat_map do |path|
  File.read(path, encoding: 'UTF-8').scan(/[a-z][a-z0-9_]*\.[a-z][a-z0-9_]*\.[a-z][a-z0-9_]*(?:_(?:req|resp|push))?/)
rescue ArgumentError
  []
end.uniq.sort

consumer_roots = [SERVER, File.join(CLIENT, 'assets')]
protocol_consumers = []
unless protocol_ids.empty?
  consumer_roots.each do |root|
    Dir.glob(File.join(root, '**', '*.{java,ts,json}')).each do |path|
      text = File.read(path, encoding: 'UTF-8') rescue next
      used = protocol_ids.select { |id| text.include?(id) }
      protocol_consumers << { path: path.delete_prefix(File.dirname(ROOT) + '/'), protocolIds: used } unless used.empty?
    end
  end
end

module_paths = affected.map { |artifact| modules.fetch(artifact)[:root].delete_prefix(ROOT + '/') }.sort
report = {
  generatedAt: Time.now.utc.iso8601,
  inputMode: explicit.empty? ? 'source-newer-than-test-report' : 'AOO_CHANGED_FILES plus timestamps',
  seedModules: seeds.to_a.sort,
  affectedModules: affected.to_a.sort,
  dependencyEdges: reverse.sum { |_key, values| values.length },
  protocolIds: protocol_ids,
  protocolConsumers: protocol_consumers,
  requiresClientInteractionRegression: protocol_consumers.any? { |entry| entry[:path].start_with?('Client/') },
  commands: module_paths.empty? ? [] : ["./mvnw -pl #{module_paths.join(',')} -am test"],
  passed: !seeds.empty? && !module_paths.empty?
}
FileUtils.mkdir_p(File.dirname(OUTPUT))
File.write(OUTPUT, JSON.pretty_generate(report) + "\n")
puts JSON.generate(seedModules: seeds.length, affectedModules: affected.length,
                   protocolIds: protocol_ids.length, protocolConsumers: protocol_consumers.length,
                   passed: report[:passed])
exit(report[:passed] ? 0 : 1)
