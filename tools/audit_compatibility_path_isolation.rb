#!/usr/bin/env ruby
# frozen_string_literal: true

require 'json'
require 'time'
require 'fileutils'

root = File.expand_path('..', __dir__)
output = File.join(root, 'work/audit/compatibility-path-isolation.json')
excluded = %w[/target/ /build/ /.git/ /work/audit/].freeze
patterns = {
  legacyProtocolMode: /V1_COMPATIBILITY/,
  legacyEndpoint: /(?:JavaServerPack|127\.0\.0\.1:(?:904|9998))/, 
  compatibilityAlias: /(?:legacyEvent|legacyHandlers|compatibility requests)/,
  replayBypass: /HttpRequireFreshness/
}.freeze
findings = []
Dir.glob(File.join(root, 'server/**/*'), File::FNM_DOTMATCH).each do |path|
  next unless File.file?(path) && %w[.java .kt .properties .yml .yaml .xml].include?(File.extname(path))
  next if excluded.any? { |part| path.include?(part) }
  relative = path.delete_prefix(root + '/')
  File.foreach(path, encoding: 'UTF-8', invalid: :replace, undef: :replace).with_index(1) do |raw, number|
    line = raw.encode('UTF-8', invalid: :replace, undef: :replace, replace: '')
    patterns.each do |kind, pattern|
      findings << {kind: kind, path: relative, line: number, text: line.strip[0, 220]} if line.match?(pattern)
    end
  end
end
counts = patterns.keys.to_h { |kind| [kind, findings.count { |row| row[:kind] == kind }] }
summary = counts.merge(violations: findings.size, passed: findings.empty?)
report = {schemaVersion: 1, generatedAt: Time.now.utc.iso8601,
          invariant: 'Production has no compatibility endpoint, V1 fallback, replay bypass or legacy message alias.',
          summary: summary, findings: findings}
FileUtils.mkdir_p(File.dirname(output))
File.write(output, JSON.pretty_generate(report) + "\n", mode: 'w:UTF-8')
puts JSON.generate(summary)
exit(summary[:passed] ? 0 : 2) if ENV['AOO_ENFORCE_COMPATIBILITY_GATE'] == '1'
