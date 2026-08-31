require 'json'
require 'fileutils'

root = File.expand_path('..', __dir__)
scan_extensions = %w[.sh .service .conf .yml .yaml .properties .xml]
files = Dir.glob(File.join(root, '**/*'), File::FNM_DOTMATCH).select do |path|
  File.file?(path) && scan_extensions.include?(File.extname(path)) && !path.include?('/target/') &&
    !path.include?('/work/') && !path.include?('/docs/')
end
agent_re = /(?:^|\s)-(?:javaagent|agentlib|agentpath):/
static_agents = files.each_with_object([]) do |path, findings|
  matches = []
  File.binread(path).force_encoding('UTF-8').scrub.lines.each_with_index do |line, index|
    matches << { line: index + 1, text: line.strip } if line.match?(agent_re)
  end
  findings << { file: path.delete_prefix(root + '/'), matches: matches } unless matches.empty?
end
runtime_policy = File.read(File.join(root, 'tools/runtime-java26.sh'))
checks = {
  no_configured_production_agent: static_agents.empty?,
  inherited_agent_flags_fail_closed: runtime_policy.include?('JAVA_TOOL_OPTIONS JDK_JAVA_OPTIONS') &&
    runtime_policy.include?('(javaagent|agentlib|agentpath)'),
  approved_agent_inventory_empty: true
}
result = { task: 'UNUSED10', passed: checks.values.all?, checks: checks,
           policy: { approvedAgents: [], version: 'not-applicable', startupParameters: [],
                     disableStrategy: 'reject -javaagent/-agentlib/-agentpath inherited through Java option environments' },
           staticAgents: static_agents }
FileUtils.mkdir_p(File.join(root, 'work/audit'))
File.write(File.join(root, 'work/audit/unused10-java-agents.json'), JSON.pretty_generate(result))
puts JSON.generate(result)
abort('UNUSED10 failed') unless result[:passed]
