#!/usr/bin/env ruby
# frozen_string_literal: true
require 'json'
require 'yaml'
root = File.expand_path('..', __dir__)
files = Dir.glob(File.join(root, '**/*'), File::FNM_DOTMATCH).select { |p| File.file?(p) }
              .reject { |p| p.match?(%r{/(?:target|build|work|database/original|reference|\.git)/}) }
text = files.to_h { |p| [p.delete_prefix(root + '/'), File.binread(p)] }
runtime = text.fetch('deploy/ops/kubernetes/mongodb.yaml')
pom = text.fetch('pom.xml')
bad = text.select { |path, body| path != 'tools/audit_mongodb_baseline.rb' && body.match?(/image:\s*["']?mongo:(?:8\.0|8\.2(?:\.12)?|8\.3(?:\.8)?|latest)(?:["'\s]|$)/) }
checks = {
  productionImagePinned: runtime.include?('image: mongo:8.0.29-noble'),
  javaDriverPinned: pom.include?('<mongodb.version>5.10.0</mongodb.version>'),
  noFloatingOrWrongMongoImage: bad.empty?,
  futureTargetNotProductionDependency: text.select { |_, body| body.include?('8.3.8') }.keys.all? { |p| p.start_with?('docs/') || %w[tools/audit_latest_stable_inventory.rb tools/audit_mongodb_baseline.rb].include?(p) }
}
report = {schemaVersion: 1, server: {image: 'mongo:8.0.29-noble', version: '8.0.29', role: 'production/default'}, javaDriver: {version: '5.10.0'}, futureCompatibilityTarget: {version: '8.3.8', enabledByDefault: false}, checks: checks, scannedFiles: text.size, violations: bad.keys}
out = File.join(root, 'docs/generated/mongodb-8.0.29-baseline.json')
File.write(out, JSON.pretty_generate(report) + "\n")
abort("MongoDB baseline audit failed: #{report}") unless checks.values.all?
puts "MongoDB 8.0.29 baseline PASS (#{text.size} source/config files scanned)"
