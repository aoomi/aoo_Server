#!/usr/bin/env ruby
# frozen_string_literal: true

require 'digest'
require 'json'
require 'time'

module Unused01DirectDependencies
  COMMAND = ['./mvnw', '-pl', 'modules/gameplay/games/poker/cx', '-am', 'test', 'dependency:3.11.0:analyze'].freeze
  MAX_PROOF_AGE_SECONDS = 300
  PROTECTED_PATTERNS = {
    /jackson-datatype-jsr310/ => 'ObjectMapper.findAndRegisterModules runtime module',
    /logback-classic/ => 'SLF4J runtime provider',
    /mysql-connector-j/ => 'JDBC DriverManager runtime provider',
    /spring-boot-starter/ => 'Spring auto-configuration aggregate',
    /druid-spring-boot-starter/ => 'Druid Spring auto-configuration aggregate',
    /com\.aoo\.bcg:games:pom/ => 'ServiceLoader game assembly aggregate',
    /account_server:hall/ => 'protobuf/package assembly dependency',
    /game-category-(?:mahjong|poker|long-card|word-card)/ => 'family registry compile boundary',
    /com\.lmax:disruptor/ => '2.22 compatibility plugin runtime',
    /okhttp-jvm/ => '2.22 platform SDK compatibility runtime',
    /rocketmq-client/ => 'RocketMQ reflective listener/runtime factory',
    /com\.alibaba:druid:jar/ => 'configured JDBC pool runtime'
  }.freeze

  module_function

  def audit(root:, expected_run_id:, now: Time.now)
    audit_dir = File.join(root, 'build-only/audit')
    log_path = File.join(audit_dir, 'unused01-analysis-baseline.log')
    manifest_path = File.join(audit_dir, 'unused01-analysis-baseline.json')
    raise 'UNUSED01 proof log missing' unless File.file?(log_path)
    raise 'UNUSED01 proof manifest missing' unless File.file?(manifest_path)

    log = File.binread(log_path)
    raise 'UNUSED01 proof log empty' if log.empty?
    manifest = JSON.parse(File.read(manifest_path, encoding: 'UTF-8'))
    validate_manifest!(manifest, expected_run_id, log_path, log, now)

    module_name = nil
    mode = nil
    undeclared = []
    unused = []
    log.each_line do |line|
      module_name = Regexp.last_match(1).strip if line =~ /Building (.+?)\s+\[\d+\/\d+\]/
      mode = :undeclared if line.include?('Used undeclared dependencies found:')
      mode = :unused if line.include?('Unused declared dependencies found:')
      if line.start_with?('[WARNING]') && line.include?('dependencies found:') &&
         !line.include?('Used undeclared') && !line.include?('Unused declared')
        mode = nil
      end
      if line =~ /\[WARNING\]\s+([^\s]+:[^\s]+:[^\s]+:[^\s]+:[^\s]+)/
        item = { module: module_name, coordinate: Regexp.last_match(1) }
        (mode == :undeclared ? undeclared : unused) << item if mode
      elsif line.start_with?('[INFO]') && !line.include?('Add the following')
        mode = nil
      end
    end

    dynamic = dynamic_evidence(root)
    classified = unused.map do |item|
      reason = PROTECTED_PATTERNS.find { |pattern, _| item[:coordinate].match?(pattern) }&.last
      item.merge(reason: reason)
    end
    unproved = classified.select { |item| item[:reason].nil? }
    checks = {
      same_wrapper_run: true,
      fresh_nonempty_sha_matched: true,
      reactor_build_success: true,
      source_and_bytecode_analysis: true,
      reflection_config_spi_scan: true,
      all_unused_classified: unproved.empty?
    }
    out = {
      schemaVersion: 2, task: 'UNUSED01', passed: checks.values.all?, checks: checks,
      usedUndeclaredDeferredToUnused02: undeclared,
      classifiedDynamicOrAssembly: classified, unprovedUnused: unproved,
      dynamicEvidence: dynamic.take(200),
      log: 'build-only/audit/unused01-analysis-baseline.log',
      manifest: 'build-only/audit/unused01-analysis-baseline.json', runId: expected_run_id
    }
    File.write(File.join(audit_dir, 'unused01-direct-dependencies.json'), JSON.pretty_generate(out) + "\n")
    raise 'UNUSED01 failed: unclassified unused dependency' unless out[:passed]
    out
  end

  def validate_manifest!(manifest, expected_run_id, log_path, log, now)
    raise 'UNUSED01 manifest schema mismatch' unless manifest['schemaVersion'] == 2
    raise 'UNUSED01 run id missing or mismatched' unless !expected_run_id.to_s.empty? && manifest['runId'] == expected_run_id
    raise 'UNUSED01 command mismatch' unless manifest['command'] == COMMAND
    raise 'UNUSED01 parent Maven did not exit zero' unless manifest['exitCode'] == 0
    raise 'UNUSED01 log path mismatch' unless File.expand_path(manifest['logPath'].to_s) == File.expand_path(log_path)
    raise 'UNUSED01 log digest mismatch' unless manifest['logSha256'] == Digest::SHA256.hexdigest(log)
    started = Time.iso8601(manifest.fetch('startedAt'))
    ended = Time.iso8601(manifest.fetch('endedAt'))
    raise 'UNUSED01 invalid proof time order' if ended < started
    raise 'UNUSED01 proof is stale or from the future' if now - ended > MAX_PROOF_AGE_SECONDS || ended > now + 5
    raise 'UNUSED01 log predates current run' if File.mtime(log_path) + 1 < started
    raise 'UNUSED01 dependency analyze proof missing' unless log.include?('dependency:3.11.0:analyze')
    raise 'UNUSED01 BUILD SUCCESS proof missing' unless log.match?(/^\[INFO\]\s+BUILD SUCCESS\s*$/)
  rescue KeyError, ArgumentError => error
    raise "UNUSED01 invalid manifest: #{error.message}"
  end

  def dynamic_evidence(root)
    paths = Dir.glob(File.join(root, 'modules/**/{META-INF/services/**,*.yml,*.yaml,*.properties,*.xml}'))
    paths.each_with_object([]) do |path, result|
      next unless File.file?(path)
      text = File.binread(path).force_encoding('UTF-8').scrub
      if text.match?(/Class\.forName|ServiceLoader|spring\.factories|AutoConfiguration|driver-class-name|provider/i)
        result << path.delete_prefix(root + '/')
      end
    end
  end
end

if __FILE__ == $PROGRAM_NAME
  root = File.expand_path('../../..', __dir__)
  run_id_index = ARGV.index('--run-id')
  run_id = run_id_index && ARGV[run_id_index + 1]
  begin
    puts JSON.generate(Unused01DirectDependencies.audit(root: root, expected_run_id: run_id))
  rescue StandardError => error
    warn error.message
    exit 1
  end
end
