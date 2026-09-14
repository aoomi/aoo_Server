#!/usr/bin/env ruby
# frozen_string_literal: true

require 'json'
require 'time'
require 'fileutils'

root = File.expand_path('..', __dir__)
output = File.join(root, 'work/audit/continuous-legacy-isolation.json')
inputs = %w[
  legacy-production-reachability.json
  legacy-websocket-isolation.json
  credential-isolation-audit.json
  database-single-writer-audit.json
  legacy-build-isolation.json
  compatibility-path-isolation.json
  legacy-endpoint-negative-probe.json
  reference-source-isolation.json
].map { |name| File.join(root, 'work/audit', name) }

gates = inputs.map do |path|
  if File.file?(path)
    data = JSON.parse(File.read(path, encoding: 'UTF-8'))
    {artifact: path.delete_prefix(root + '/'), present: true, passed: data.dig('summary', 'passed') == true}
  else
    {artifact: path.delete_prefix(root + '/'), present: false, passed: false}
  end
end

image_manifests = Dir.glob(File.join(root, '**/{Dockerfile,compose*.yml,compose*.yaml,*image*.json}')).reject do |path|
  path.include?('/target/') || path.include?('/build/')
end
runtime_network_evidence = ENV['AOO_PROBE_ENV'] == 'production'
log_export = ENV['AOO_PRODUCTION_LOG_EXPORT']
log_evidence = !log_export.to_s.empty? && File.file?(log_export)

summary = {
  componentGates: gates.size,
  componentGatesPassed: gates.count { |gate| gate[:passed] },
  imageManifests: image_manifests.size,
  productionNetworkEvidence: runtime_network_evidence,
  productionLogEvidence: log_evidence,
  passed: gates.all? { |gate| gate[:passed] } && image_manifests.any? && runtime_network_evidence && log_evidence
}
report = {
  schemaVersion: 1,
  generatedAt: Time.now.utc.iso8601,
  invariant: 'Every release continuously proves images, configuration, runtime network and logs cannot reach a legacy target.',
  summary: summary,
  gates: gates,
  imageManifests: image_manifests.map { |path| path.delete_prefix(root + '/') },
  requiredCiInputs: ['built image SBOM', 'rendered production configuration', 'production network probe', 'production log export']
}
FileUtils.mkdir_p(File.dirname(output))
File.write(output, JSON.pretty_generate(report) + "\n", mode: 'w:UTF-8')
puts JSON.generate(summary)
exit(summary[:passed] ? 0 : 2) if ENV['AOO_ENFORCE_CONTINUOUS_ISOLATION_GATE'] == '1'
