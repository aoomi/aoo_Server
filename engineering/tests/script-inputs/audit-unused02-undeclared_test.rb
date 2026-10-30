#!/usr/bin/env ruby
# frozen_string_literal: true

require 'digest'
require 'fileutils'
require 'json'
require 'minitest/autorun'
require 'tmpdir'
require 'time'
require_relative '../../audit/script-inputs/audit-unused02-undeclared'

class Unused02UndeclaredTest < Minitest::Test
  NOW = Time.iso8601('2026-10-01T04:00:00Z')

  def with_root
    Dir.mktmpdir('unused02-') do |root|
      FileUtils.mkdir_p(File.join(root, 'build-only/audit'))
      yield root
    end
  end

  def write_valid_proof(root)
    log = "[INFO] --- dependency:3.11.0:analyze ---\n[INFO] BUILD SUCCESS\n"
    log_path = File.join(root, Unused02Undeclared::LOG_PATH)
    File.binwrite(log_path, log)
    File.utime(NOW - 1, NOW - 1, log_path)
    manifest = { 'schemaVersion' => 2, 'runId' => 'run-1', 'startedAt' => (NOW - 2).iso8601(6), 'endedAt' => (NOW - 1).iso8601(6), 'command' => Unused01DirectDependencies::COMMAND, 'exitCode' => 0, 'logPath' => log_path, 'logSha256' => Digest::SHA256.hexdigest(log) }
    File.write(File.join(root, Unused02Undeclared::MANIFEST_PATH), JSON.generate(manifest))
    direct = { 'schemaVersion' => 2, 'task' => 'UNUSED01', 'passed' => true, 'checks' => Unused02Undeclared::REQUIRED_CHECKS.to_h { |key| [key, true] }, 'usedUndeclaredDeferredToUnused02' => [], 'log' => Unused02Undeclared::LOG_PATH, 'manifest' => Unused02Undeclared::MANIFEST_PATH, 'runId' => 'run-1' }
    File.write(File.join(root, Unused02Undeclared::DIRECT_PATH), JSON.generate(direct))
    [direct, manifest]
  end

  def test_current_schema_v2_proof_passes
    with_root do |root|
      write_valid_proof(root)
      assert Unused02Undeclared.audit(root: root, now: NOW)[:passed]
    end
  end

  def test_missing_malformed_and_direct_proof_permutations_fail_closed
    with_root do |root|
      assert_raises(Errno::ENOENT) { Unused02Undeclared.audit(root: root, now: NOW) }
      mutations = [
        ->(direct, _manifest) { direct['schemaVersion'] = 1 }, ->(direct, _manifest) { direct['passed'] = false }, ->(direct, _manifest) { direct['checks'].delete('same_wrapper_run') },
        ->(direct, _manifest) { direct['checks']['same_wrapper_run'] = false },
        ->(direct, _manifest) { direct['runId'] = 'other' }, ->(direct, _manifest) { direct['log'] = 'wrong' }, ->(direct, _manifest) { direct['manifest'] = 'wrong' },
        ->(_direct, manifest) { manifest['command'] = ['wrong'] }, ->(_direct, manifest) { manifest['exitCode'] = 1 }, ->(_direct, manifest) { manifest['logSha256'] = '0' * 64 },
        ->(_direct, manifest) { manifest['startedAt'] = NOW.iso8601(6); manifest['endedAt'] = (NOW - 1).iso8601(6) },
        ->(_direct, manifest) { manifest['endedAt'] = (NOW + 6).iso8601(6) }, ->(_direct, manifest) { manifest['endedAt'] = (NOW - 301).iso8601(6) }
      ]
      mutations.each do |mutation|
        direct, manifest = write_valid_proof(root)
        mutation.call(direct, manifest)
        File.write(File.join(root, Unused02Undeclared::DIRECT_PATH), JSON.generate(direct))
        File.write(File.join(root, Unused02Undeclared::MANIFEST_PATH), JSON.generate(manifest))
        assert_raises(RuntimeError) { Unused02Undeclared.audit(root: root, now: NOW) }
      end
      write_valid_proof(root)
      File.utime(NOW - 10, NOW - 10, File.join(root, Unused02Undeclared::LOG_PATH))
      manifest = JSON.parse(File.read(File.join(root, Unused02Undeclared::MANIFEST_PATH)))
      manifest['startedAt'] = (NOW - 2).iso8601(6)
      File.write(File.join(root, Unused02Undeclared::MANIFEST_PATH), JSON.generate(manifest))
      assert_raises(RuntimeError) { Unused02Undeclared.audit(root: root, now: NOW) }
      [
        '[INFO] --- dependency:3.11.0:list ---\n[INFO] BUILD SUCCESS\n',
        '[INFO] --- dependency:3.11.0:analyze ---\n[INFO] BUILD FAILURE\n'
      ].each do |log|
        _direct, manifest = write_valid_proof(root)
        log_path = File.join(root, Unused02Undeclared::LOG_PATH)
        File.binwrite(log_path, log)
        File.utime(NOW - 1, NOW - 1, log_path)
        manifest['logSha256'] = Digest::SHA256.hexdigest(log)
        File.write(File.join(root, Unused02Undeclared::MANIFEST_PATH), JSON.generate(manifest))
        assert_raises(RuntimeError) { Unused02Undeclared.audit(root: root, now: NOW) }
      end
      File.write(File.join(root, Unused02Undeclared::DIRECT_PATH), '{')
      assert_raises(JSON::ParserError) { Unused02Undeclared.audit(root: root, now: NOW) }
    end
  end
end
