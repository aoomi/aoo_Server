#!/usr/bin/env ruby
require 'json'
require 'fileutils'
require 'find'

root = File.expand_path('..', __dir__)
manifest_path = File.join(root, 'docs/generated/generated-artifact-manifest.json')
manifest = JSON.parse(File.read(manifest_path))
entries = manifest.fetch('artifacts')
listed = entries.map { |entry| entry.fetch('path') }

generated_docs = Dir.glob(File.join(root, 'docs/generated/*')).select { |path| File.file?(path) }
                    .map { |path| path.delete_prefix(root + '/') }
                    .reject { |path| path == 'docs/generated/generated-artifact-manifest.json' }

generated_java = []
Find.find(root) do |path|
  if File.directory?(path)
    Find.prune if path.match?(%r{/(target|build|work|reference|\.git)(/|$)})
    next
  end
  next unless path.end_with?('.java')
  first_line = File.open(path, &:readline) rescue ''
  generated_java << path.delete_prefix(root + '/') if first_line.match?(/Generated|generated|DO NOT EDIT|Do not edit/)
end

required_fields = %w[path kind authoritativeInputs generator rebuild owner editPolicy]
checks = {
  manifest_schema: manifest['schemaVersion'] == 1 && entries.is_a?(Array) && !entries.empty?,
  required_metadata: entries.all? { |entry| required_fields.all? { |field| entry.key?(field) && !entry[field].nil? } },
  generated_docs_covered: (generated_docs - listed).empty?,
  generated_java_covered: (generated_java - listed).empty?,
  outputs_exist: entries.all? { |entry| File.exist?(File.join(root, entry['path'])) },
  inputs_exist: entries.all? { |entry| entry['authoritativeInputs'].all? { |input|
    absolute = File.expand_path(input, root)
    wildcard = input.match?(/[\*?\[]/)
    File.exist?(absolute) || wildcard || input == 'third-party'
  } },
  generators_exist: entries.all? { |entry| File.file?(File.join(root, entry['generator'])) },
  immutable_policy: entries.all? { |entry| entry['editPolicy'] == 'generated-do-not-edit' },
  transient_release_isolation: %w[work/ **/target/ **/build/].all? { |rule| File.read(File.join(root, '.releaseignore')).include?(rule) }
}

result = {
  task: 'HYGIENE12',
  passed: checks.values.all?,
  checks: checks,
  manifest: 'docs/generated/generated-artifact-manifest.json',
  registeredArtifacts: listed.sort,
  generatedDocuments: generated_docs.sort,
  generatedJava: generated_java.sort
}
out = File.join(root, 'work/audit/hygiene12-generated-boundary.json')
FileUtils.mkdir_p(File.dirname(out))
File.write(out, JSON.pretty_generate(result) + "\n")
puts "HYGIENE12 #{result[:passed] ? 'passed' : 'failed'}: #{listed.length} generated artifacts registered"
exit(result[:passed] ? 0 : 1)
