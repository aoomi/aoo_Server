#!/usr/bin/env ruby
require 'json'; require 'digest'; require 'open3'; require 'fileutils'
root = File.expand_path('..', __dir__)
scripts = (1..9).map { |number| Dir.glob(File.join(root, "scripts/audit-drift#{format('%02d', number)}-*.rb")).first }.compact
artifacts = JSON.parse(File.read(File.join(root, 'docs/generated/generated-artifact-manifest.json'))).fetch('artifacts')
  .select { |entry| entry.fetch('generator').match?(/audit-drift0[1-9]-/) }
  .map { |entry| File.join(root, entry.fetch('path')) }
digest = -> { artifacts.to_h { |path| [path, File.exist?(path) ? Digest::SHA256.file(path).hexdigest : nil] } }
before = digest.call
results = scripts.map do |script|
  out, err, status = Open3.capture3('ruby', script, chdir: root)
  { script: script.delete_prefix(root + '/'), passed: status.success?, stdout: out.strip, stderr: err.strip }
end
after = digest.call
changed = before.keys.select { |path| before[path] != after[path] }.map { |path| path.delete_prefix(root + '/') }
checks = { allGeneratorsPassed: results.all? { |result| result[:passed] }, generatedArtifactsCurrent: changed.empty?, coversCodeSchemaRegistryPrefabDependencies: %w[drift01 drift02 drift03 drift04 drift06].all? { |name| scripts.any? { |script| File.basename(script).include?(name) } } }
report = { schemaVersion: 1, generators: results, changedArtifacts: changed, checks: checks }
out = File.join(root, 'docs/generated/drift10-continuous-drift.json'); FileUtils.mkdir_p(File.dirname(out)); File.write(out, JSON.pretty_generate(report) + "\n")
abort "DRIFT10 failed: #{checks}" unless checks.values.all?
puts 'DRIFT10 PASS: continuous drift gate is deterministic'
