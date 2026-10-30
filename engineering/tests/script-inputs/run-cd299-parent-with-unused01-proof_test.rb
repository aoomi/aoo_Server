#!/usr/bin/env ruby
# frozen_string_literal: true

require 'digest'
require 'fileutils'
require 'json'
require 'minitest/autorun'
require 'tmpdir'
require 'time'
require_relative '../../audit/script-inputs/run-cd299-parent-with-unused01-proof'

class Cd299ParentWithUnused01ProofTest < Minitest::Test

  def test_current_module_paths_and_dynamic_evidence
    assert_equal 'modules/gameplay/games/poker/cx', Unused01DirectDependencies::COMMAND[2]
    Dir.mktmpdir do |root|
      path = File.join(root, 'modules/example/src/main/resources/application.yml')
      FileUtils.mkdir_p(File.dirname(path))
      File.write(path, 'driver-class-name: example.Driver')
      assert_includes Unused01DirectDependencies.dynamic_evidence(root), 'modules/example/src/main/resources/application.yml'
    end
  end
  NOW = Time.iso8601('2026-10-01T04:00:00Z')

  def with_root
    Dir.mktmpdir('unused01-') do |root|
      FileUtils.mkdir_p(File.join(root, 'build-only/audit'))
      yield root
    end
  end

  def valid_log(unused = 'ch.qos.logback:logback-classic:jar:1.6.3:compile')
    <<~LOG
      [INFO] Building Gateway 1.0 [1/1]
      [INFO] --- dependency:3.11.0:analyze (default-cli) @ Gateway ---
      [WARNING] Unused declared dependencies found:
      [WARNING]    #{unused}
      [INFO] BUILD SUCCESS
    LOG
  end

  def write_proof(root, log: valid_log, run_id: 'run-1', started: NOW - 2, ended: NOW - 1, digest: nil)
    log_path = File.join(root, 'build-only/audit/unused01-analysis-baseline.log')
    File.binwrite(log_path, log)
    File.utime(ended, ended, log_path)
    manifest = {
      'schemaVersion' => 2, 'runId' => run_id,
      'startedAt' => started.iso8601(6), 'endedAt' => ended.iso8601(6),
      'command' => Unused01DirectDependencies::COMMAND, 'exitCode' => 0,
      'logPath' => log_path, 'logSha256' => digest || Digest::SHA256.hexdigest(log)
    }
    File.write(File.join(root, 'build-only/audit/unused01-analysis-baseline.json'), JSON.generate(manifest))
  end

  def test_valid_fresh_proof_passes_and_keeps_unused_classification
    with_root do |root|
      write_proof(root)
      result = Unused01DirectDependencies.audit(root: root, expected_run_id: 'run-1', now: NOW)
      assert result[:passed]
      assert_equal 'SLF4J runtime provider', result[:classifiedDynamicOrAssembly][0][:reason]
    end
  end

  def test_missing_markers_empty_stale_and_digest_drift_fail_closed
    with_root do |root|
      write_proof(root, log: valid_log.sub("[INFO] BUILD SUCCESS\n", ''))
      assert_raises(RuntimeError) { Unused01DirectDependencies.audit(root: root, expected_run_id: 'run-1', now: NOW) }
      write_proof(root, log: valid_log.sub('dependency:3.11.0:analyze', 'dependency:3.11.0:list'))
      assert_raises(RuntimeError) { Unused01DirectDependencies.audit(root: root, expected_run_id: 'run-1', now: NOW) }
      write_proof(root, log: '')
      assert_raises(RuntimeError) { Unused01DirectDependencies.audit(root: root, expected_run_id: 'run-1', now: NOW) }
      write_proof(root, started: NOW - 700, ended: NOW - 600)
      assert_raises(RuntimeError) { Unused01DirectDependencies.audit(root: root, expected_run_id: 'run-1', now: NOW) }
      write_proof(root, digest: '0' * 64)
      assert_raises(RuntimeError) { Unused01DirectDependencies.audit(root: root, expected_run_id: 'run-1', now: NOW) }
    end
  end

  def test_run_id_and_unclassified_unused_fail_closed
    with_root do |root|
      write_proof(root)
      assert_raises(RuntimeError) { Unused01DirectDependencies.audit(root: root, expected_run_id: 'other', now: NOW) }
      write_proof(root, log: valid_log('example:unknown:jar:1.0:compile'))
      assert_raises(RuntimeError) { Unused01DirectDependencies.audit(root: root, expected_run_id: 'run-1', now: NOW) }
    end
  end

  def test_wrapper_parent_nonzero_does_not_publish_or_run_audit
    with_root do |root|
      audit_called = false
      runner = lambda do |_command, path, _root|
        File.write(path, '[INFO] BUILD FAILURE')
        7
      end
      error = assert_raises(RuntimeError) do
        Cd299ParentWithUnused01Proof.run(
          root: root, clock: -> { NOW }, command_runner: runner,
          audit_runner: ->(_root, _run_id) { audit_called = true }
        )
      end
      refute audit_called
      refute File.exist?(File.join(root, 'build-only/audit/unused01-analysis-baseline.log'))
      refute File.exist?(File.join(root, 'build-only/audit/unused01-analysis-baseline.json'))
      failure_log = File.join(root, 'build-only/audit/unused01-analysis-failure.log')
      failure_manifest = File.join(root, 'build-only/audit/unused01-analysis-failure.json')
      assert File.file?(failure_log)
      manifest = JSON.parse(File.read(failure_manifest))
      assert_equal 'parent_maven', manifest['stage']
      assert_equal 7, manifest['exitCode']
      assert_equal Unused01DirectDependencies::COMMAND, manifest['command']
      assert_equal failure_log, manifest['logPath']
      assert_equal Digest::SHA256.file(failure_log).hexdigest, manifest['logSha256']
      assert_includes error.message, "runId=#{manifest['runId']}"
      assert_includes error.message, "failureLog=#{failure_log}"
      assert_includes error.message, "failureManifest=#{failure_manifest}"
    end
  end

  def test_failure_evidence_publish_error_fails_closed_without_audit
    with_root do |root|
      audit_called = false
      runner = lambda do |_command, path, _root|
        File.write(path, '[INFO] BUILD FAILURE')
        9
      end
      publisher = ->(*_arguments) { raise 'failure evidence publish failed' }
      error = assert_raises(RuntimeError) do
        Cd299ParentWithUnused01Proof.run(
          root: root, clock: -> { NOW }, command_runner: runner,
          audit_runner: ->(_root, _run_id) { audit_called = true },
          failure_publisher: publisher
        )
      end
      assert_equal 'failure evidence publish failed', error.message
      refute audit_called
      refute File.exist?(File.join(root, 'build-only/audit/unused01-analysis-baseline.log'))
      refute File.exist?(File.join(root, 'build-only/audit/unused01-analysis-baseline.json'))
    end
  end

  def test_wrapper_publishes_current_atomic_proof_then_audits
    with_root do |root|
      observed = nil
      runner = lambda do |command, path, command_root|
        assert_equal Cd299ParentWithUnused01Proof::BOOTSTRAP_COMMAND, command
        assert_equal root, command_root
        File.write(path, valid_log)
        0
      end
      unused02_called = false
      auditor = lambda do |audit_root, run_id|
        observed = Unused01DirectDependencies.audit(root: audit_root, expected_run_id: run_id, now: NOW)
      end
      assert_equal 0, Cd299ParentWithUnused01Proof.run(
        root: root, clock: -> { NOW }, command_runner: runner, audit_runner: auditor,
        unused02_runner: ->(audit_root) { unused02_called = File.file?(File.join(audit_root, 'build-only/audit/unused01-direct-dependencies.json')) }
      )
      assert observed[:passed]
      assert unused02_called
      manifest = JSON.parse(File.read(File.join(root, 'build-only/audit/unused01-analysis-baseline.json')))
      assert_equal 2, manifest['schemaVersion']
      assert_equal Unused01DirectDependencies::COMMAND, manifest['command']
      assert_equal 0, manifest['exitCode']
    end
  end

  def test_wrapper_removes_preseeded_direct_json_before_bootstrap
    with_root do |root|
      direct = File.join(root, 'build-only/audit/unused01-direct-dependencies.json')
      File.write(direct, '{"stale":true}')
      observed_missing = false
      runner = lambda do |command, path, _root|
        assert_equal Cd299ParentWithUnused01Proof::BOOTSTRAP_COMMAND, command
        observed_missing = !File.exist?(direct)
        File.write(path, valid_log)
        0
      end
      Cd299ParentWithUnused01Proof.run(
        root: root, clock: -> { NOW }, command_runner: runner,
        audit_runner: ->(_audit_root, _run_id) { File.write(direct, '{}') },
        unused02_runner: ->(_audit_root) {}
      )
      assert observed_missing
    end
  end

  def test_bootstrap_marker_is_default_off_and_scoped_to_the_existing_execution
    pom = File.read(File.expand_path('../../../pom.xml', __dir__))
    assert_includes pom, '<aoo.unused02.bootstrap.skip>false</aoo.unused02.bootstrap.skip>'
    refute_includes pom, '<aoo.unused02.bootstrap.skip>true</aoo.unused02.bootstrap.skip>'
    assert_includes pom, '<skip>${aoo.unused02.bootstrap.skip}</skip>'
    refute_match(/<profiles>.*aoo\.unused02\.bootstrap\.skip/m, pom)
    assert_equal 1, Cd299ParentWithUnused01Proof::BOOTSTRAP_COMMAND.count { |argument| argument == '-Daoo.unused02.bootstrap.skip=true' }
    refute_includes Cd299ParentWithUnused01Proof::BOOTSTRAP_COMMAND, '-Dexec.skip=true'
  end
end
