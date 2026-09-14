#!/usr/bin/env ruby
require 'json'
require 'fileutils'

root = File.expand_path('..', __dir__)
manifest = File.read(File.join(root, 'deploy/production-services.yaml'))
scripts = %w[tools/start-hall-local.sh tools/start-njpdk-local.sh tools/start-cdxzmj-local.sh].to_h { |path| [path, File.read(File.join(root, path))] }
expected = { 'hall' => [9998, 9888], 'njpdk' => [9996, 9886], 'cdxzmj' => [19996, 19886] }
checks = {
  servicesDeclared: %w[account hall njpdk cdxzmj].all? { |name| manifest.include?("name: #{name}") },
  portsMatchRuntimeScripts: expected.all? { |name, ports| ports.all? { |port| manifest.include?("containerPort: #{port}") && scripts.fetch("tools/start-#{name}-local.sh").include?(port.to_s) } },
  healthOrReadiness: manifest.scan(/readiness:/).length == 4,
  resourceLimits: manifest.scan(/memoryLimit:/).length == 4 && manifest.scan(/cpuLimit:/).length == 4,
  externalFacilities: %w[mysql redis rocketmq mongodb].all? { |name| manifest.include?(name) }
}
report = { schemaVersion: 1, manifest: 'deploy/production-services.yaml', services: expected.keys + ['account'], checks: checks }
out = File.join(root, 'docs/generated/drift07-deployment-reconciliation.json')
FileUtils.mkdir_p(File.dirname(out)); File.write(out, JSON.pretty_generate(report) + "\n")
abort "DRIFT07 failed: #{checks}" unless checks.values.all?
puts 'DRIFT07 PASS: deployment manifest reconciled'
