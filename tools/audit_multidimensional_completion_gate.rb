#!/usr/bin/env ruby
# frozen_string_literal: true

require 'fileutils'
require 'json'
require 'time'

ROOT = File.expand_path('..', __dir__)
OUTPUT = File.join(ROOT, 'work', 'audit', 'multidimensional-completion-gate.json')
DIMENSIONS = {
  code: %w[authoritative-game-provider-coverage.json continuous-legacy-isolation.json],
  data: %w[database-single-writer-audit.json data-lifecycle-policy-audit.json],
  interface: %w[compatibility-path-isolation.json protocol-coverage-audit.json],
  ui: %w[store-ui-subscription-graph.json operation-ui-invariant.json],
  test: %w[affected-test-matrix.json computed-regression-scope.json],
  documentation: %w[issue-registry.json dependency-lifecycle-governance.json],
  legacyIsolation: %w[legacy-production-reachability.json continuous-legacy-isolation.json]
}.freeze

def passed?(document)
  return document['passed'] if document.key?('passed')
  return document.dig('summary', 'passed') if document.dig('summary')&.key?('passed')
  false
end

dimensions = DIMENSIONS.map do |name, filenames|
  evidence = filenames.map do |filename|
    path = File.join(ROOT, 'work', 'audit', filename)
    if !File.file?(path)
      { path: "work/audit/#{filename}", present: false, passed: false }
    else
      begin
        document = JSON.parse(File.read(path, encoding: 'UTF-8'))
        { path: "work/audit/#{filename}", present: true, passed: passed?(document) }
      rescue JSON::ParserError
        { path: "work/audit/#{filename}", present: true, passed: false, invalidJson: true }
      end
    end
  end
  { dimension: name, passed: evidence.all? { |item| item[:present] && item[:passed] }, evidence: evidence }
end

report = {
  generatedAt: Time.now.utc.iso8601,
  invariant: 'Completion requires code, data, interface, UI, test, documentation and legacy-isolation dimensions to pass independently.',
  dimensions: dimensions,
  passedDimensions: dimensions.count { |dimension| dimension[:passed] },
  failedDimensions: dimensions.count { |dimension| !dimension[:passed] },
  passed: dimensions.all? { |dimension| dimension[:passed] }
}
FileUtils.mkdir_p(File.dirname(OUTPUT))
File.write(OUTPUT, JSON.pretty_generate(report) + "\n")
puts JSON.generate(dimensions: dimensions.length, passed: report[:passedDimensions],
                   failed: report[:failedDimensions], completionAllowed: report[:passed])
exit(report[:passed] ? 0 : 1)
