#!/usr/bin/env ruby
# frozen_string_literal: true

require 'fileutils'
require 'json'
require 'minitest/autorun'
require 'open3'
require 'rbconfig'
require 'tmpdir'

class Unused19BuildTemplatesTest < Minitest::Test
  SCRIPT = File.expand_path('../../audit/script-inputs/audit-unused19-build-templates.rb', __dir__)
  OUTPUT = 'build-only/audit/unused19-creator-build-templates.json'
  CREATOR_MARKERS = [
    'desktop and mobile share the same project-bundle registration boundary',
    'bundle-script-bootstrap.js',
    'src/settings.json',
    'Promise.all'
  ].freeze

  def run_fixture(builder_platforms: %w[web-desktop web-mobile], policy_platforms: builder_platforms,
                  templates: %w[web-desktop web-mobile native oppo-mini-game vivo-mini-game],
                  versions: nil, creator_version: '3.8.8', focused_test: CREATOR_MARKERS.join("\n"),
                  extra_web_desktop_script: nil)
    Dir.mktmpdir('unused19-') do |root|
      server = File.join(root, 'Server')
      client = File.join(root, 'Client')
      %w[scripts config build-only/audit].each { |directory| FileUtils.mkdir_p(File.join(server, directory)) }
      %w[build-templates profiles/v2/packages tests/build assets].each { |directory| FileUtils.mkdir_p(File.join(client, directory)) }

      task_map = builder_platforms.each_with_index.to_h do |platform, index|
        [index.to_s, { 'options' => { 'platform' => platform } }]
      end
      File.write(File.join(client, 'profiles/v2/packages/builder.json'), JSON.generate('BuildTaskManager' => { 'taskMap' => task_map }, 'common' => { 'polyfills' => { 'asyncFunctions' => true } }))
      File.write(File.join(client, 'package.json'), JSON.generate('creator' => { 'version' => creator_version }))
      File.write(File.join(server, 'config/client-profile-allowlist.json'), JSON.generate('creatorVersion' => '3.8.8', 'allowedPlatforms' => policy_platforms))
      File.write(File.join(client, 'tests/build/aoo0070-web-desktop-bundle-bootstrap.test.mjs'), focused_test)

      templates.each do |template|
        template_dir = File.join(client, 'build-templates', template)
        FileUtils.mkdir_p(template_dir)
        if %w[web-desktop web-mobile].include?(template)
          refs = <<~HTML
            <script src="src/polyfills.bundle.js"></script>
            <script src="src/system.bundle.js"></script>
            <script src="src/import-map.json"></script>
            <script src="bundle-script-bootstrap.js"></script>
          HTML
          refs += "<script src=\"#{extra_web_desktop_script}\"></script>\n" if template == 'web-desktop' && extra_web_desktop_script
          File.write(File.join(template_dir, 'index.html'), refs)
        else
          File.write(File.join(template_dir, 'template.ejs'), "<%# #{template} deprecated fixture %>\n")
        end
      end
      versions ||= templates.each_with_object({}) { |template, result| result[template] = '1.0.0' }
      File.write(File.join(client, 'build-templates/templates-version.json'), JSON.pretty_generate(versions))
      FileUtils.mkdir_p(File.join(server, 'engineering/audit/script-inputs'))
      FileUtils.cp(SCRIPT, File.join(server, 'engineering/audit/script-inputs/audit-unused19-build-templates.rb'))

      stdout, stderr, status = Open3.capture3(RbConfig.ruby, File.join(server, 'engineering/audit/script-inputs/audit-unused19-build-templates.rb'), chdir: server)
      output_path = File.join(server, OUTPUT)
      flunk("audit did not write #{OUTPUT}; exit=#{status.exitstatus}; stdout=#{stdout}; stderr=#{stderr}") unless File.file?(output_path)
      result = JSON.parse(File.read(output_path))
      yield result, stdout, stderr, status
    end
  end

  def test_exact_project_bootstrap_scripts_are_separate_from_creator_outputs
    run_fixture do |result, _stdout, stderr, status|
      assert status.success?, stderr
      assert_equal true, result['passed']
      assert_equal [], result['unknownScriptReferences']
      assert_equal %w[web-desktop/bundle-script-bootstrap.js web-mobile/bundle-script-bootstrap.js], result['allowedProjectScriptReferences']
      assert_equal %w[native oppo-mini-game vivo-mini-game], result['deprecatedTemplates'].map { |row| row['name'] }
      assert_equal true, result['checks']['project_scripts_have_focused_contract']
    end
  end

  def test_unknown_script_remains_fail_closed
    run_fixture(extra_web_desktop_script: 'custom-project-loader.js') do |result, _stdout, _stderr, status|
      refute status.success?
      assert_equal false, result['passed']
      assert_includes result['unknownScriptReferences'], 'web-desktop/custom-project-loader.js'
      assert_equal false, result['checks']['template_scripts_are_creator_3_8_8_outputs']
    end
  end

  def test_builder_and_client_policy_platform_conflict_remains_fail_closed
    run_fixture(policy_platforms: ['web-desktop']) do |result, _stdout, _stderr, status|
      refute status.success?
      assert_equal false, result['passed']
      assert_equal ['web-mobile'], result['platformAuthorityConflicts']['builderOnly']
      assert_equal false, result['checks']['builder_client_platform_authority_matches']
    end
  end

  def test_stale_templates_version_remains_fail_closed
    run_fixture(versions: { 'native' => '1.0.0', 'oppo-mini-game' => '1.0.0', 'vivo-mini-game' => '1.0.0' }) do |result, _stdout, _stderr, status|
      refute status.success?
      assert_equal false, result['passed']
      assert_equal true, result['templatesVersion']['stale']
      assert_equal false, result['checks']['templates_version_current']
    end
  end

  def test_unrecognized_template_platform_and_missing_focused_contract_fail_closed
    run_fixture(templates: %w[web-desktop web-mobile native oppo-mini-game vivo-mini-game unknown-platform], focused_test: '') do |result, _stdout, _stderr, status|
      refute status.success?
      assert_equal false, result['passed']
      assert_includes result['templatePlatformMismatches'], 'unknown-platform'
      assert_equal false, result['checks']['project_scripts_have_focused_contract']
    end
  end
end
