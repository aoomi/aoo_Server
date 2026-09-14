#!/usr/bin/env ruby
require 'json'
require 'pathname'

root = Pathname(__dir__).join('..').expand_path
pom = root.join('pom.xml').read
required_executions = %w[
  enforce-direct-dependency-proof
  enforce-effective-build-plugins
  enforce-plugin-execution-ledger
  enforce-unique-production-implementations
]
evidence = %w[
  work/audit/unused01-direct-dependencies.json
  work/audit/unused07-build-plugins.json
  work/audit/unused08-plugin-executions.json
]
parsed = evidence.to_h do |relative|
  path = root.join(relative)
  [relative, path.exist? ? JSON.parse(path.read) : {}]
end
checks = {
  mavenValidateIsCiEntry: required_executions.all? { |id| pom.include?("<id>#{id}</id>") && pom.include?('<phase>validate</phase>') },
  dependencyConvergence: pom.include?('<dependencyConvergence/>'),
  unusedDependenciesGreen: parsed[evidence[0]]['passed'] == true && Array(parsed[evidence[0]]['unprovedUnused']).empty?,
  unusedPluginsGreen: parsed[evidence[1]]['passed'] == true && Array(parsed[evidence[1]]['ineffective']).empty?,
  pluginExecutionsGreen: parsed[evidence[2]]['passed'] == true && Array(parsed[evidence[2]]['violations']).empty? && Array(parsed[evidence[2]]['duplicateIds']).empty?,
  duplicateImplementationGate: pom.include?('<id>enforce-unique-production-implementations</id>')
}
errors = checks.reject { |_key, value| value }.keys
report = { task: 'UNUSED30', status: errors.empty? ? 'passed' : 'failed', checks: checks,
  ciCommand: './mvnw verify', admissionPhase: 'validate', requiredExecutions: required_executions,
  policy: 'Any CI runner invoking the standard Maven lifecycle fails before compilation on unproved dependencies, ineffective/unexecuted plugins, duplicate production implementations or convergence conflicts.', errors: errors }
out = root.join('work/audit/unused30-continuous-gate.json'); out.write(JSON.pretty_generate(report) + "\n")
abort("UNUSED30 failed: #{errors.join(', ')}") unless errors.empty?
puts 'UNUSED30 passed: Maven validate continuously enforces dependency/plugin/duplicate/convergence policy'
