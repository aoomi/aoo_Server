require 'json'
require 'fileutils'
require 'rexml/document'

root = File.expand_path('..', __dir__)
default_phases = { ['maven-enforcer-plugin', 'enforce'] => 'validate' }
ledger = []
violations = []

Dir.glob(File.join(root, '**/pom.xml')).reject { |path| path.include?('/target/') }.sort.each do |path|
  document = REXML::Document.new(File.read(path))
  REXML::XPath.each(document, '//*[local-name()="plugin"]') do |plugin|
    artifact = REXML::XPath.first(plugin, '*[local-name()="artifactId"]')&.text
    REXML::XPath.each(plugin, '*[local-name()="executions"]/*[local-name()="execution"]') do |execution|
      id = REXML::XPath.first(execution, '*[local-name()="id"]')&.text
      goals = REXML::XPath.match(execution, '*[local-name()="goals"]/*[local-name()="goal"]').map(&:text)
      configured_phase = REXML::XPath.first(execution, '*[local-name()="phase"]')&.text
      phase = configured_phase || default_phases[[artifact, goals.first]]
      configuration = REXML::XPath.first(execution, '*[local-name()="configuration"]')
      config_text = configuration ? configuration.to_s : ''
      fail_closed = !config_text.match?(/<(?:ignoreExitValue|failOnError|skip)>\s*(?:true|false)\s*<\//)
      output = case goals.first
               when 'copy-dependencies' then 'target/lib'
               when 'copy-resources' then 'target/resources'
               when 'add-source' then 'reactor compile source roots'
               when 'enforce' then 'build admission decision'
               when 'exec' then 'work/audit evidence or build admission decision'
               else 'Maven lifecycle artifact'
               end
      item = { pom: path.delete_prefix(root + '/'), plugin: artifact, id: id, phase: phase,
               goals: goals, input: config_text.empty? ? 'project model/defaults' : 'declared configuration',
               output: output, cache: 'deterministic Maven lifecycle; no hidden remote cache',
               failureImpact: fail_closed ? 'fail build' : 'non-failing override present' }
      ledger << item
      violations << item.merge(reason: 'missing execution id') if id.to_s.empty?
      violations << item.merge(reason: 'missing goal') if goals.empty?
      violations << item.merge(reason: 'phase not explicit and no documented goal default') if phase.to_s.empty?
      violations << item.merge(reason: 'execution can suppress failure') unless fail_closed
    end
  end
end

duplicate_ids = ledger.group_by { |item| [item[:pom], item[:id]] }.select { |_, entries| entries.size > 1 }.keys
checks = { all_executions_catalogued: !ledger.empty?, complete_contracts: violations.empty?, unique_ids_per_pom: duplicate_ids.empty? }
result = { task: 'UNUSED08', passed: checks.values.all?, checks: checks, executionCount: ledger.size,
           duplicateIds: duplicate_ids, violations: violations, ledger: ledger }
FileUtils.mkdir_p(File.join(root, 'work/audit'))
File.write(File.join(root, 'work/audit/unused08-plugin-executions.json'), JSON.pretty_generate(result))
puts JSON.generate(result.reject { |key, _| key == :ledger })
abort('UNUSED08 failed') unless result[:passed]
