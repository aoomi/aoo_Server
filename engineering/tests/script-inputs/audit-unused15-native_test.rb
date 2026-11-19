#!/usr/bin/env ruby
# frozen_string_literal: true

require 'fileutils'
require 'json'
require 'minitest/autorun'
require 'open3'
require 'rbconfig'
require 'tmpdir'

class Unused15NativeTest < Minitest::Test
  SCRIPT = File.expand_path('../../audit/script-inputs/audit-unused15-native.rb', __dir__)
  SCAN_INPUT_ROOTS = File.expand_path('../../scan-input-roots.json', __dir__)
  OUTPUT = 'build-only/audit/unused15-native-dependencies.json'

  def run_fixture(files:, pom: minimal_pom)
    Dir.mktmpdir('unused15-') do |root|
      %w[scripts tools modules build-only/audit engineering/audit/script-inputs engineering/local-dev/tool-inputs].each { |directory| FileUtils.mkdir_p(File.join(root, directory)) }
      File.write(File.join(root, 'pom.xml'), pom)
      FileUtils.cp(SCAN_INPUT_ROOTS, File.join(root, 'engineering/scan-input-roots.json'))
      File.write(File.join(root, 'engineering/local-dev/tool-inputs/runtime-java26.sh'), "#!/bin/sh\n")
      files.each do |path, content|
        absolute = File.join(root, path)
        FileUtils.mkdir_p(File.dirname(absolute))
        File.write(absolute, content)
      end
      script = File.join(root, 'engineering/audit/script-inputs/audit-unused15-native.rb')
      FileUtils.cp(SCRIPT, script)
      stdout, stderr, status = Open3.capture3(RbConfig.ruby, script, chdir: root)
      output_path = File.join(root, OUTPUT)
      flunk("audit did not write #{OUTPUT}; exit=#{status.exitstatus}; stdout=#{stdout}; stderr=#{stderr}") unless File.file?(output_path)
      result = JSON.parse(File.read(output_path))
      yield result, stdout, stderr, status
    end
  end

  def minimal_pom
    <<~XML
      <project xmlns="http://maven.apache.org/POM/4.0.0">
        <modelVersion>4.0.0</modelVersion>
        <groupId>fixture</groupId><artifactId>fixture</artifactId><version>1.0</version>
      </project>
    XML
  end

  def test_task_only_archive_inspection_and_rules_fixture_are_not_native_unpack
    run_fixture(
      files: {
        'engineering/local-dev/task-only-runtime/launcher-r20.sh' => <<~SH,
          require_command unzip
          unzip -Z1 "$jar"
          unzip -p "$jar" "$entry"
        SH
        'engineering/tests/tool-inputs/test-room-rule-schema-evolution.rb' =>
          "abort('unzip failed') unless system('unzip', '-qq', workbook, '-d', directory)\n"
      }
    ) do |result, _stdout, stderr, status|
      assert status.success?, stderr
      assert_equal true, result['passed']
      assert_equal [], result['unpackScripts']
      assert_equal [
        'engineering/local-dev/task-only-runtime/launcher-r20.sh',
        'engineering/tests/tool-inputs/test-room-rule-schema-evolution.rb'
      ].sort, result['safeArchiveOperations'].map { |entry| entry['path'] }.uniq.sort
    end
  end

  def test_native_unpack_and_other_native_contracts_remain_fail_closed
    run_fixture(
      files: {
        'tools/native-extract.sh' => "unzip -qq payload.zip -d target\n",
        'modules/Native.java' => "class Native { void load() { System.loadLibrary(\"x\"); } }\n"
      },
      pom: minimal_pom.sub(
        '</project>',
        '<dependencies><dependency><groupId>x</groupId><artifactId>zstd-jni</artifactId><classifier>native</classifier></dependency></dependencies></project>'
      )
    ) do |result, _stdout, _stderr, status|
      refute status.success?
      assert_equal false, result['passed']
      assert_includes result['unpackScripts'], 'tools/native-extract.sh'
      assert_includes result['manualLoads'], 'modules/Native.java'
      refute_empty result['classifiers']
      assert_equal false, result['checks']['no_native_unpack_script']
      assert_equal false, result['checks']['no_manual_jni_load']
      assert_equal false, result['checks']['no_direct_native_or_classifier_dependency']
    end
  end

  def test_near_miss_rule_fixture_command_is_not_allowlisted
    run_fixture(
      files: {
        'engineering/tests/tool-inputs/test-room-rule-schema-evolution.rb' =>
          "system('unzip', '-qq', workbook, '-d', other_directory)\n"
      }
    ) do |result, _stdout, _stderr, status|
      refute status.success?
      assert_equal false, result['passed']
      assert_includes result['unpackScripts'], 'engineering/tests/tool-inputs/test-room-rule-schema-evolution.rb'
    end
  end
end
