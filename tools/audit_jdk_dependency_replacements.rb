#!/usr/bin/env ruby
# frozen_string_literal: true

require 'json'
require 'time'
require 'fileutils'

root = File.expand_path('..', __dir__)
output = File.join(root, 'work/audit/jdk-dependency-replacement.json')
rules = [
  {id: 'legacy-kernel', dependency: /com\.aoo\.legacy|kernel\.jar/, usage: /com\.aoo\.legacy/, replacement: 'GameSPI/GameCommon and native providers'},
  {id: 'joda-time', dependency: /joda-time/, usage: /org\.joda\.time/, replacement: 'java.time'},
  {id: 'commons-codec', dependency: /commons-codec/, usage: /org\.apache\.commons\.codec/, replacement: 'java.util.Base64, HexFormat and MessageDigest'},
  {id: 'commons-io', dependency: /commons-io/, usage: /org\.apache\.commons\.io/, replacement: 'java.nio.file and InputStream APIs'},
  {id: 'reflections', dependency: /org\.reflections|<artifactId>reflections/, usage: /org\.reflections/, replacement: 'ServiceLoader and generated indexes'},
  {id: 'gson', dependency: /<artifactId>gson/, usage: /com\.google\.gson/, replacement: 'project-standard Jackson codec'},
  {id: 'mina', dependency: /mina-core/, usage: /org\.apache\.mina/, replacement: 'Netty production transport'}
].freeze
files = Dir.glob(File.join(root, 'server/**/*')).select do |path|
  File.file?(path) && %w[.java .kt .xml].include?(File.extname(path)) && !path.include?('/target/') && !path.include?('/build/')
end
texts = files.to_h do |path|
  [path, File.read(path, encoding: 'UTF-8', invalid: :replace, undef: :replace).encode('UTF-8', invalid: :replace, undef: :replace, replace: '')]
end
rows = rules.map do |rule|
  dependencies = texts.select { |_path, text| text.match?(rule[:dependency]) }.keys.map { |path| path.delete_prefix(root + '/') }
  usages = texts.select { |_path, text| text.match?(rule[:usage]) }.keys.map { |path| path.delete_prefix(root + '/') }
  {id: rule[:id], replacement: rule[:replacement], dependencyFiles: dependencies, usageFiles: usages,
   productionReachable: dependencies.any? || usages.any?}
end
summary = {replacementRules: rows.size, productionReachable: rows.count { |row| row[:productionReachable] },
           cleared: rows.count { |row| !row[:productionReachable] },
           passed: rows.none? { |row| row[:productionReachable] }}
report = {schemaVersion: 1, generatedAt: Time.now.utc.iso8601,
          invariant: 'Production has no legacy kernel or utility dependency where the JDK or standard framework provides the required capability.',
          summary: summary, replacements: rows,
          policy: 'Preserved 2.22 reference libraries may remain outside the production dependency graph.'}
FileUtils.mkdir_p(File.dirname(output))
File.write(output, JSON.pretty_generate(report) + "\n", mode: 'w:UTF-8')
puts JSON.generate(summary)
exit(summary[:passed] ? 0 : 2) if ENV['AOO_ENFORCE_JDK_REPLACEMENT_GATE'] == '1'
