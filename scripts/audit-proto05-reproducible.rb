require 'json'
require 'fileutils'

root = File.expand_path('..', __dir__)
result = JSON.parse(`node #{File.join(root, 'tools/protocol/verify-reproducible.mjs')}`)
abort('PROTO05 reproducibility command failed') unless $?.success?
checks = {
  node_runtime_pinned: result.fetch('node') == File.read(File.join(root, '.node-version')).strip,
  generator_dependency_free: !File.read(File.join(root, 'tools/protocol/generate.mjs')).include?('node_modules'),
  deterministic_outputs: result.fetch('outputs').length == 3 && result.fetch('outputs').all? { |output| output.fetch('sha256').match?(/\A[0-9a-f]{64}\z/) }
}
checks[:passed] = checks.values.all?
out = File.join(root, 'docs/generated/proto05-reproducible.json')
FileUtils.mkdir_p(File.dirname(out))
File.write(out, JSON.pretty_generate(checks.merge(runtime: result.fetch('node'), outputs: result.fetch('outputs'))) + "\n")
abort('PROTO05 reproducibility audit failed') unless checks[:passed]
