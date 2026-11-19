#!/usr/bin/env ruby
# frozen_string_literal: true

require 'json'
require 'fileutils'
require 'rexml/document'

module Unused08PluginExecutions
  module_function

  DEFAULT_PHASES = { ['maven-enforcer-plugin', 'enforce'] => 'validate' }.freeze
  BOOTSTRAP_PROPERTY = 'aoo.unused02.bootstrap.skip'
  BOOTSTRAP_EXECUTION = 'enforce-no-accidental-transitives'
  BOOTSTRAP_PLUGIN = 'exec-maven-plugin'

  def inside_profile?(node)
    parent = node.respond_to?(:parent) ? node.parent : nil
    while parent
      return true if parent.respond_to?(:name) && parent.name == 'profile'
      parent = parent.respond_to?(:parent) ? parent.parent : nil
    end
    false
  end

  def audit(root:)
    root = File.expand_path(root)
    root_pom = File.join(root, 'pom.xml')
    root_document = REXML::Document.new(File.read(root_pom))
    property_nodes = REXML::XPath.match(
      root_document,
      '/*[local-name()="project"]/*[local-name()="properties"]/*'
    ).select { |node| node.name == BOOTSTRAP_PROPERTY }
    property_default = property_nodes.one? ? property_nodes.first.text.to_s.strip : nil
    property_contract = property_nodes.size == 1 && property_default == 'false'

    ledger = []
    violations = []
    bootstrap_exceptions = []

    Dir.glob(File.join(root, '**/pom.xml')).reject { |path| path.include?('/target/') }.sort.each do |path|
      document = REXML::Document.new(File.read(path))
      REXML::XPath.each(document, '//*[local-name()="plugin"]') do |plugin|
        artifact = REXML::XPath.first(plugin, '*[local-name()="artifactId"]')&.text
        REXML::XPath.each(plugin, '*[local-name()="executions"]/*[local-name()="execution"]') do |execution|
          id = REXML::XPath.first(execution, '*[local-name()="id"]')&.text
          goals = REXML::XPath.match(execution, '*[local-name()="goals"]/*[local-name()="goal"]').map(&:text)
          configured_phase = REXML::XPath.first(execution, '*[local-name()="phase"]')&.text
          phase = configured_phase || DEFAULT_PHASES[[artifact, goals.first]]
          configuration = REXML::XPath.first(execution, '*[local-name()="configuration"]')
          config_text = configuration ? configuration.to_s : ''
          skip_node = configuration && REXML::XPath.first(configuration, '*[local-name()="skip"]')
          skip_value = skip_node&.text&.strip
          relative_pom = path.delete_prefix(root + '/')
          root_execution = relative_pom == 'pom.xml'
          profile_execution = inside_profile?(execution)
          bootstrap_candidate = skip_value == '${' + BOOTSTRAP_PROPERTY + '}'
          bootstrap_allowed = bootstrap_candidate &&
                              root_execution &&
                              artifact == BOOTSTRAP_PLUGIN &&
                              id == BOOTSTRAP_EXECUTION &&
                              goals == ['exec'] &&
                              configured_phase == 'validate' &&
                              !profile_execution &&
                              property_contract
          bootstrap_exception = if bootstrap_allowed
                                  {
                                    property: BOOTSTRAP_PROPERTY,
                                    defaultValue: property_default,
                                    wrapperOnly: true,
                                    pom: relative_pom,
                                    plugin: artifact,
                                    id: id,
                                    phase: configured_phase,
                                    goals: goals
                                  }
                                end

          non_skip_suppressor = config_text.match?(
            /<(?:ignoreExitValue|failOnError)>\s*(?:true|false)\s*<\//
          )
          dynamic_skip = skip_value&.match?(/\A\$\{[^}]+\}\z/)
          skip_violation = if skip_value.nil?
                             nil
                           elsif bootstrap_exception
                             nil
                           elsif bootstrap_candidate
                             'bootstrap skip interpolation is outside the exact root execution contract'
                           elsif dynamic_skip
                             'dynamic skip is not allowlisted'
                           else
                             'execution can suppress failure'
                           end

          failure_impact = if bootstrap_exception
                             'scoped bootstrap deferral; audited exception'
                           elsif non_skip_suppressor || skip_violation
                             'non-failing override present'
                           else
                             'fail build'
                           end
          item = {
            pom: relative_pom, plugin: artifact, id: id, phase: phase,
            goals: goals,
            input: config_text.empty? ? 'project model/defaults' : 'declared configuration',
            output: case goals.first
                    when 'copy-dependencies' then 'target/lib'
                    when 'copy-resources' then 'target/resources'
                    when 'add-source' then 'reactor compile source roots'
                    when 'enforce' then 'build admission decision'
                    when 'exec' then 'build-only/audit evidence or build admission decision'
                    else 'Maven lifecycle artifact'
                    end,
            cache: 'deterministic Maven lifecycle; no hidden remote cache',
            failureImpact: failure_impact,
            bootstrapException: bootstrap_exception
          }
          ledger << item
          bootstrap_exceptions << bootstrap_exception if bootstrap_exception
          violations << item.merge(reason: 'missing execution id') if id.to_s.empty?
          violations << item.merge(reason: 'missing goal') if goals.empty?
          violations << item.merge(reason: 'phase not explicit and no documented goal default') if phase.to_s.empty?
          violations << item.merge(reason: skip_violation) if skip_violation
          violations << item.merge(reason: 'execution can suppress failure') if non_skip_suppressor
        end
      end
    end

    duplicate_ids = ledger.group_by { |item| [item[:pom], item[:id]] }.select { |_, entries| entries.size > 1 }.keys
    bootstrap_contract = {
      rootPropertyPresentOnce: property_nodes.size == 1,
      rootPropertyDefaultFalse: property_default == 'false',
      singleScopedException: bootstrap_exceptions.size == 1,
      noInvalidBootstrapShape: bootstrap_exceptions.all? { |item| item[:wrapperOnly] == true }
    }
    checks = {
      all_executions_catalogued: !ledger.empty?,
      complete_contracts: violations.empty?,
      unique_ids_per_pom: duplicate_ids.empty?,
      bootstrap_contract: bootstrap_contract.values.all?
    }
    result = {
      task: 'UNUSED08', passed: checks.values.all?, checks: checks,
      executionCount: ledger.size, duplicateIds: duplicate_ids,
      violations: violations, bootstrapContract: bootstrap_contract,
      bootstrapProperty: { name: BOOTSTRAP_PROPERTY, occurrences: property_nodes.size, default: property_default },
      bootstrapExceptions: bootstrap_exceptions, ledger: ledger
    }
    audit_dir = File.join(root, 'build-only/audit')
    FileUtils.mkdir_p(audit_dir)
    File.write(File.join(audit_dir, 'unused08-plugin-executions.json'), JSON.pretty_generate(result))
    result
  end
end

if __FILE__ == $PROGRAM_NAME
  root = File.expand_path('../../..', __dir__)
  result = Unused08PluginExecutions.audit(root: root)
  puts JSON.generate(result.reject { |key, _| key == :ledger })
  abort('UNUSED08 failed') unless result[:passed]
end
