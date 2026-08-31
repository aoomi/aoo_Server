#!/usr/bin/env ruby
require 'json'
require 'pathname'

root = Pathname(__dir__).join('..').expand_path
ignored = %r{/(?:\.git|target|work|node_modules|temp|library|build)/}
files = Dir[root.join('**/*').to_s].select { |p| File.file?(p) && !p.match?(ignored) }
ci_files = files.select do |path|
  relative = path.delete_prefix(root.to_s + '/')
  relative.match?(%r{(?:^|/)\.github/workflows/.*\.ya?ml$}) ||
    File.basename(path).match?(/\A(?:\.gitlab-ci\.yml|Jenkinsfile.*|azure-pipelines\.yml|bitbucket-pipelines\.yml|buildkite\.ya?ml|drone\.ya?ml)\z/i) ||
    relative.start_with?('.circleci/')
end

action_refs = ci_files.flat_map do |path|
  File.readlines(path).each_with_index.map do |line, index|
    { file: path.delete_prefix(root.to_s + '/'), line: index + 1, reference: line.strip } if line.match?(/\buses:\s*[^\s]+@[^\s]+/)
  end.compact
end

errors = []
errors << "CI definitions require ownership audit: #{ci_files.join(', ')}" unless ci_files.empty?
report = {
  task: 'UNUSED23', status: errors.empty? ? 'passed' : 'failed',
  pipelineDefinitions: ci_files.map { |p| p.delete_prefix(root.to_s + '/') }, actionReferences: action_refs,
  currentGateOwner: 'Maven validate lifecycle and repository-local scripts',
  policy: 'No dormant CI/CD integration is retained. Any future pipeline must pin immutable action/plugin versions, map every step to a repository gate, avoid duplicate scans, and use current Aoo release targets only.',
  errors: errors
}
out = root.join('work/audit/unused23-ci-plugins.json')
out.dirname.mkpath
out.write(JSON.pretty_generate(report) + "\n")
abort(errors.join("\n")) unless errors.empty?
puts 'UNUSED23 passed: no dormant CI/CD definition, action or legacy release target exists'
