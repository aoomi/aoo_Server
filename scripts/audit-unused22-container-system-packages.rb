#!/usr/bin/env ruby
require 'json'
require 'pathname'

root = Pathname(__dir__).join('..').expand_path
container_names = /(?:\ADockerfile|\AContainerfile|\.dockerfile\z|(?:docker-)?compose.*\.ya?ml\z)/i
ignored = %r{/(?:\.git|target|work|node_modules|temp|library|build)/}
files = Dir[root.join('**/*').to_s].select { |p| File.file?(p) && !p.match?(ignored) }
container_files = files.select { |p| File.basename(p).match?(container_names) }
shell_files = files.select { |p| p.end_with?('.sh', '.bash', '.zsh') }
install_pattern = /\b(?:apt(?:-get)?\s+(?:update|install)|apk\s+add|yum\s+install|dnf\s+install|microdnf\s+install|brew\s+install)\b/i
runtime_installs = shell_files.map do |path|
  hits = File.readlines(path).each_with_index.map { |line, index| { line: index + 1, text: line.strip } if line.match?(install_pattern) }.compact
  { file: path.delete_prefix(root.to_s + '/'), hits: hits } unless hits.empty?
end.compact

errors = []
errors << "runtime scripts install system packages: #{runtime_installs.map { |x| x[:file] }.join(', ')}" unless runtime_installs.empty?
container_audits = container_files.map do |path|
  body = File.read(path)
  checks = {
    pinned_jre_runtime: body.match?(/FROM\s+eclipse-temurin:25-jre\b/),
    no_package_installer: !body.match?(install_pattern),
    non_root_user: body.match?(/^USER\s+(?!0\b|root\b)\S+/i),
    owned_runtime_copy: body.match?(/COPY\s+--chown=/),
    bounded_jvm_memory: body.include?('MaxRAMPercentage')
  }
  errors << "container minimal-runtime audit failed: #{path}" unless checks.values.all?
  { file: path.delete_prefix(root.to_s + '/'), checks: checks }
end
report = {
  task: 'UNUSED22', status: errors.empty? ? 'passed' : 'failed',
  containerDefinitions: container_files.map { |p| p.delete_prefix(root.to_s + '/') },
  containerAudits: container_audits,
  runtimePackageInstalls: runtime_installs,
  deploymentModel: container_files.empty? ? 'JDK/runtime dependencies are externally provisioned.' : 'Minimal JRE runtime image with application artifacts assembled outside the runtime stage.',
  policy: 'Runtime images must use the pinned JRE, install no system packages, run non-root, copy owned runtime artifacts and declare a JVM memory bound.',
  errors: errors
}
out = root.join('work/audit/unused22-container-system-packages.json')
out.dirname.mkpath
out.write(JSON.pretty_generate(report) + "\n")
abort(errors.join("\n")) unless errors.empty?
puts "UNUSED22 passed: #{container_audits.length} container definition(s) audited; no runtime system-package installer exists"
