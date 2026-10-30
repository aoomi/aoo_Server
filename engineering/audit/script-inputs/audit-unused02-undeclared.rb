#!/usr/bin/env ruby
# frozen_string_literal: true

require 'json'
require 'fileutils'
require_relative 'audit-unused01-direct-dependencies'

module Unused02Undeclared
  module_function

  REQUIRED_CHECKS = %w[same_wrapper_run fresh_nonempty_sha_matched reactor_build_success source_and_bytecode_analysis reflection_config_spi_scan all_unused_classified].freeze
  DIRECT_PATH = 'build-only/audit/unused01-direct-dependencies.json'
  LOG_PATH = 'build-only/audit/unused01-analysis-baseline.log'
  MANIFEST_PATH = 'build-only/audit/unused01-analysis-baseline.json'
  ALLOWED = {
    /org\.junit\.jupiter:junit-jupiter-api:.*:test/ => 'JUnit aggregate intentionally exports API',
    /ch\.qos\.logback:logback-core/ => 'logback-classic provider aggregate',
    /org\.apache\.rocketmq:rocketmq-(?:common|remoting)/ => 'RocketMQ client SDK aggregate',
    /com\.fasterxml\.jackson\.core:jackson-core/ => 'Jackson databind public API aggregate',
    /^(?:org\.springframework|org\.springframework\.boot|org\.springframework\.data|org\.hibernate\.orm|jakarta\.)/ => 'Spring Boot starter managed API surface',
    /^(?:org\.mongodb):(?!mongodb-driver-sync)/ => 'MongoDB starter/driver aggregate',
    /org\.aspectj:aspectjweaver/ => 'Spring AOP runtime',
    /org\.apache\.tomcat\.embed:tomcat-embed-core/ => 'Spring Web embedded runtime',
    /org\.slf4j:slf4j-api/ => 'Spring logging facade',
    /org\.redisson:redisson/ => 'configured Redisson Spring adapter'
  }.freeze

  def audit(root:, now: Time.now)
    audit_dir = File.join(root, 'build-only/audit')
    direct = JSON.parse(File.read(File.join(root, DIRECT_PATH), encoding: 'UTF-8'))
    validate_direct!(direct)
    log_path = File.join(root, LOG_PATH)
    log = File.binread(log_path)
    manifest = JSON.parse(File.read(File.join(root, MANIFEST_PATH), encoding: 'UTF-8'))
    Unused01DirectDependencies.validate_manifest!(manifest, direct.fetch('runId'), log_path, log, now)
    items = direct.fetch('usedUndeclaredDeferredToUnused02')
    raise 'UNUSED02 direct dependency list invalid' unless items.is_a?(Array)
    classified = items.map do |item|
      coordinate = item.fetch('coordinate')
      reason = ALLOWED.find { |pattern, _| coordinate.match?(pattern) }&.last
      item.merge('reason' => reason)
    end
    unproved = classified.select { |item| item['reason'].nil? }
    checks = { dependency_analysis: true, aggregate_exports_classified: true, no_accidental_transitive: unproved.empty? }
    output = { task: 'UNUSED02', passed: checks.values.all?, checks: checks, classifiedAggregateExports: classified.reject { |item| item['reason'].nil? }, accidentalTransitives: unproved }
    raise 'UNUSED02 failed' unless output[:passed]
    FileUtils.mkdir_p(audit_dir)
    File.write(File.join(audit_dir, 'unused02-undeclared.json'), JSON.pretty_generate(output) + "\n")
    output
  end

  def validate_direct!(direct)
    raise 'UNUSED02 schema mismatch' unless direct['schemaVersion'] == 2
    raise 'UNUSED02 task mismatch' unless direct['task'] == 'UNUSED01'
    raise 'UNUSED02 proof not passed' unless direct['passed'] == true
    raise 'UNUSED02 run id missing' unless direct['runId'].is_a?(String) && !direct['runId'].empty?
    raise 'UNUSED02 log path mismatch' unless direct['log'] == LOG_PATH
    raise 'UNUSED02 manifest path mismatch' unless direct['manifest'] == MANIFEST_PATH
    checks = direct['checks']
    raise 'UNUSED02 checks missing' unless checks.is_a?(Hash)
    REQUIRED_CHECKS.each { |check| raise "UNUSED02 required check failed: #{check}" unless checks[check] == true }
  end
end

if __FILE__ == $PROGRAM_NAME
  root = File.expand_path('../../..', __dir__)
  begin
    puts JSON.generate(Unused02Undeclared.audit(root: root))
  rescue StandardError => error
    warn error.message
    exit 1
  end
end
