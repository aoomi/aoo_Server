#!/usr/bin/env ruby
require 'fileutils'
require 'json'
require 'minitest/autorun'
require 'open3'
require 'pathname'
require 'rbconfig'
require 'tmpdir'

class AuditUnused20FrontendSdksTest < Minitest::Test
  SCRIPT = Pathname(__dir__).join('../../audit/script-inputs/audit-unused20-frontend-sdks.rb').expand_path

  def test_internal_comment_words_and_identifiers_are_not_sdk_sources
    sources = {
      'Games/Common/Code/Runtime/GameRuntimeEntry.ts' => <<~'TS',
        /** Optional gameplay-only layout/render adjustment. */
        export interface CommonRoomMountedView {
          /** gameplay may adjust its real layout/render differences. */
          mounted?: () => void;
        }
      TS
      'Login/Code/Bootstrap/LoadingEnvironmentPolicy.ts' => <<~'TS'
        export class LoadingEnvironmentPolicy {
          private entrySceneClearIntent = false;
          public hasEntrySceneClearIntent(): boolean {
            return this.entrySceneClearIntent;
          }
        }
      TS
    }

    _stdout, stderr, status, report = run_audit(sources: sources)
    assert status.success?, stderr
    assert_equal [], report.fetch('thirdPartySdkSources')
    assert_equal [], report.fetch('productionSdkPackages')
  end

  def test_real_third_party_source_and_package_fail_closed
    sources = {
      'ThirdParty/SentryBridge.ts' => <<~'TS'
        import * as Sentry from '@sentry/browser';
        Sentry.init({});
      TS
    }
    packages = { '@sentry/browser' => '^8.0.0' }

    _stdout, stderr, status, report = run_audit(sources: sources, packages: packages)
    refute status.success?
    assert_includes stderr, 'third-party SDK sources remain'
    assert_includes stderr, 'production SDK packages remain'
    assert_equal ['assets/ThirdParty/SentryBridge.ts'], report.fetch('thirdPartySdkSources')
    assert_equal '@sentry/browser', report.fetch('productionSdkPackages').first.fetch('name')
  end

  def test_untrusted_source_and_package_location_with_known_token_still_fail_closed
    sources = {
      'Internal/UnknownVendorBridge.ts' => <<~'TS'
        const sdkName = 'sentry-client';
        export const bridge = sdkName;
      TS
    }
    packages = { '@unknown/sentry-client' => '^1.0.0' }

    _stdout, stderr, status, report = run_audit(sources: sources, packages: packages)
    refute status.success?
    assert_includes stderr, 'third-party SDK sources remain'
    assert_includes stderr, 'production SDK packages remain'
    assert_equal ['assets/Internal/UnknownVendorBridge.ts'], report.fetch('thirdPartySdkSources')
    assert_equal '@unknown/sentry-client', report.fetch('productionSdkPackages').first.fetch('name')
  end

  def test_identifier_suffixes_do_not_match_but_package_separators_do
    sources = {
      'Internal/AdjustmentPolicy.ts' => <<~'TS'
        export const adjustmentPolicy = 'adjustment';
      TS
    }
    packages = { 'firebase_core' => '^3.0.0' }

    _stdout, stderr, status, report = run_audit(sources: sources, packages: packages)
    refute status.success?
    assert_includes stderr, 'production SDK packages remain'
    assert_equal [], report.fetch('thirdPartySdkSources')
    assert_equal 'firebase_core', report.fetch('productionSdkPackages').first.fetch('name')
  end

  private

  def run_audit(sources:, packages: {})
    Dir.mktmpdir('unused20-contract-') do |directory|
      root = Pathname(directory)
      server = root.join('Server')
      client = root.join('Client')
      script = server.join('engineering/audit/script-inputs/audit-unused20-frontend-sdks.rb')
      FileUtils.mkdir_p(script.dirname)
      FileUtils.cp(SCRIPT, script)

      compatibility = client.join('assets/Common/Code/Runtime/CompatibilityApp/platform')
      runtime = client.join('assets/Common/Code/Runtime/platform/LegacyPlatformRuntime.ts')
      FileUtils.mkdir_p(compatibility)
      FileUtils.mkdir_p(runtime.dirname)
      File.write(compatibility.join('LegacyAnalyticsService.ts'), 'export class LegacyAnalyticsService {}\n')
      File.write(compatibility.join('LegacyVoiceService.ts'), 'export class LegacyVoiceService {}\n')
      File.write(runtime, "LegacyAnalyticsService\nLegacyVoiceService\n")

      sources.each do |relative, contents|
        path = client.join('assets').join(relative)
        FileUtils.mkdir_p(path.dirname)
        File.write(path, contents)
      end
      FileUtils.mkdir_p(client)
      File.write(client.join('package.json'), JSON.pretty_generate('dependencies' => packages) + "\n")

      stdout, stderr, status = Open3.capture3(RbConfig.ruby, script.to_s, chdir: server.to_s)
      report_path = server.join('build-only/audit/unused20-frontend-sdks.json')
      report = JSON.parse(File.read(report_path))
      return stdout, stderr, status, report
    end
  end
end
