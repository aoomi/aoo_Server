#!/usr/bin/env ruby
# frozen_string_literal: true

require 'digest'
require 'fileutils'
require 'json'
require 'rbconfig'
require 'securerandom'
require 'tempfile'
require 'time'
require_relative 'audit-unused01-direct-dependencies'

module Cd299ParentWithUnused01Proof
  module_function

  BOOTSTRAP_PROPERTY = '-Daoo.unused02.bootstrap.skip=true'
  BOOTSTRAP_COMMAND = ([Unused01DirectDependencies::COMMAND.first, BOOTSTRAP_PROPERTY] + Unused01DirectDependencies::COMMAND.drop(1)).freeze

  def run(root:, clock: -> { Time.now }, command_runner: method(:run_command),
          audit_runner: method(:run_audit), unused02_runner: method(:run_unused02),
          failure_publisher: method(:publish_failure))
    audit_dir = File.join(root, 'build-only/audit')
    FileUtils.mkdir_p(audit_dir)
    baseline_path = File.join(audit_dir, 'unused01-analysis-baseline.log')
    manifest_path = File.join(audit_dir, 'unused01-analysis-baseline.json')
    failure_log_path = File.join(audit_dir, 'unused01-analysis-failure.log')
    failure_manifest_path = File.join(audit_dir, 'unused01-analysis-failure.json')
    FileUtils.rm_f(baseline_path)
    FileUtils.rm_f(manifest_path)
    FileUtils.rm_f(failure_log_path)
    FileUtils.rm_f(failure_manifest_path)
    FileUtils.rm_f(File.join(audit_dir, 'unused01-direct-dependencies.json'))

    run_id = SecureRandom.uuid
    started = clock.call
    temporary = Tempfile.new(['unused01-', '.log'], audit_dir)
    temporary.close
    exit_code = command_runner.call(BOOTSTRAP_COMMAND, temporary.path, root)
    ended = clock.call
    unless exit_code == 0
      failure_publisher.call(
        temporary.path, failure_log_path, failure_manifest_path,
        failure_manifest(run_id, started, ended, exit_code, failure_log_path, temporary.path)
      )
      raise "UNUSED01 parent Maven failed: exit=#{exit_code} runId=#{run_id} " \
            "failureLog=#{failure_log_path} failureManifest=#{failure_manifest_path}"
    end

    log = File.binread(temporary.path)
    raise 'UNUSED01 parent Maven log empty' if log.empty?
    raise 'UNUSED01 dependency analyze proof missing' unless log.include?('dependency:3.11.0:analyze')
    raise 'UNUSED01 BUILD SUCCESS proof missing' unless log.match?(/^\[INFO\]\s+BUILD SUCCESS\s*$/)

    File.rename(temporary.path, baseline_path)
    manifest = {
      'schemaVersion' => 2, 'runId' => run_id,
      'startedAt' => started.iso8601(6), 'endedAt' => ended.iso8601(6),
      'command' => Unused01DirectDependencies::COMMAND,
      'exitCode' => exit_code, 'logPath' => baseline_path,
      'logSha256' => Digest::SHA256.hexdigest(log)
    }
    atomic_json(manifest_path, manifest)
    audit_runner.call(root, run_id)
    unused02_runner.call(root)
    0
  ensure
    FileUtils.rm_f(temporary.path) if defined?(temporary) && temporary
  end

  def failure_manifest(run_id, started, ended, exit_code, log_path, temporary_path)
    log = File.binread(temporary_path)
    {
      'schemaVersion' => 2, 'runId' => run_id, 'stage' => 'parent_maven',
      'startedAt' => started.iso8601(6), 'endedAt' => ended.iso8601(6),
      'command' => Unused01DirectDependencies::COMMAND,
      'exitCode' => exit_code, 'logPath' => log_path,
      'logSha256' => Digest::SHA256.hexdigest(log)
    }
  end

  def publish_failure(temporary_log_path, failure_log_path, failure_manifest_path, manifest)
    File.rename(temporary_log_path, failure_log_path)
    atomic_json(failure_manifest_path, manifest)
  end

  def run_command(command, log_path, root)
    pid = Process.spawn(*command, chdir: root, out: log_path, err: [:child, :out])
    Process.wait(pid)
    $CHILD_STATUS ? $CHILD_STATUS.exitstatus : $?.exitstatus
  end

  def run_audit(root, run_id)
    script = File.join(root, 'engineering/audit/script-inputs/audit-unused01-direct-dependencies.rb')
    system(RbConfig.ruby, script, '--run-id', run_id, chdir: root)
    raise 'UNUSED01 post-build audit failed' unless $?.success?
  end

  def run_unused02(root)
    script = File.join(root, 'engineering/audit/script-inputs/audit-unused02-undeclared.rb')
    system(RbConfig.ruby, script, chdir: root)
    raise 'UNUSED02 post-bootstrap audit failed' unless $?.success?
  end

  def atomic_json(path, value)
    temporary = "#{path}.tmp-#{Process.pid}-#{SecureRandom.hex(4)}"
    File.open(temporary, 'wb') do |file|
      file.write(JSON.pretty_generate(value) + "\n")
      file.flush
      file.fsync
    end
    File.rename(temporary, path)
  ensure
    FileUtils.rm_f(temporary) if defined?(temporary) && temporary
  end
end

if __FILE__ == $PROGRAM_NAME
  root = File.expand_path('../../..', __dir__)
  begin
    exit Cd299ParentWithUnused01Proof.run(root: root)
  rescue StandardError => error
    warn error.message
    exit 1
  end
end
