require 'json'
require 'fileutils'

root = File.expand_path('../../..', __dir__)
client = File.expand_path('../Client', root)
template_root = File.join(client, 'build-templates')
templates = Dir.exist?(template_root) ? Dir.children(template_root).select { |name| File.directory?(File.join(template_root, name)) }.sort : []
builder_profile = JSON.parse(File.read(File.join(client, 'profiles/v2/packages/builder.json')))
task_map = builder_profile.dig('BuildTaskManager', 'taskMap') || {}
configured_platforms = task_map.values.map { |task| task.dig('options', 'platform') }.compact.uniq.sort
client_policy = JSON.parse(File.read(File.join(root, 'config/client-profile-allowlist.json')))
policy_platforms = Array(client_policy['allowedPlatforms']).map(&:to_s).sort
template_files = Dir.glob(File.join(template_root, '**/*')).select { |path| File.file?(path) }
template_text = template_files.map { |path| File.read(path, encoding: 'UTF-8') }.join("\n")
script_refs = template_text.scan(/<script[^>]+src=["']([^"']+)/).flatten
creator_platforms = %w[web-desktop web-mobile]
deprecated_template_candidates = %w[native oppo-mini-game vivo-mini-game]
known_creator_outputs = %w[src/polyfills.bundle.js src/system.bundle.js src/import-map.json]
allowed_project_scripts = {
  'web-desktop' => %w[bundle-script-bootstrap.js],
  'web-mobile' => %w[bundle-script-bootstrap.js]
}.freeze
script_references_by_platform = creator_platforms.each_with_object({}) do |platform, result|
  html_files = Dir.glob(File.join(template_root, platform, '**/*')).select { |path| File.file?(path) }
  result[platform] = html_files.flat_map do |path|
    File.read(path, encoding: 'UTF-8').scan(/<script[^>]+src=["']([^"']+)/).flatten
  end.uniq.sort
end
creator_script_references = script_references_by_platform.values.flatten.uniq.sort
creator_output_references = creator_script_references & known_creator_outputs
allowed_project_script_references = script_references_by_platform.each_with_object([]) do |(platform, refs), result|
  result.concat(refs.select { |ref| allowed_project_scripts.fetch(platform, []).include?(ref) }.map { |ref| "#{platform}/#{ref}" })
end.sort
required_project_script_references = (configured_platforms & creator_platforms).flat_map do |platform|
  allowed_project_scripts.fetch(platform, []).map { |ref| "#{platform}/#{ref}" }
end.sort
unknown_script_references = script_references_by_platform.each_with_object([]) do |(platform, refs), result|
  allowed = known_creator_outputs + allowed_project_scripts.fetch(platform, [])
  refs.reject { |ref| allowed.include?(ref) }.each { |ref| result << "#{platform}/#{ref}" }
end.sort
focused_test_path = File.join(client, 'tests/build/aoo0070-web-desktop-bundle-bootstrap.test.mjs')
focused_test_markers = [
  'desktop and mobile share the same project-bundle registration boundary',
  'bundle-script-bootstrap.js',
  'src/settings.json',
  'Promise.all'
]
focused_test_text = File.file?(focused_test_path) ? File.read(focused_test_path, encoding: 'UTF-8') : ''
focused_test_contract = {
  path: focused_test_path.delete_prefix(client + '/'),
  present: File.file?(focused_test_path),
  markers: focused_test_markers.each_with_object({}) { |marker, result| result[marker] = focused_test_text.include?(marker) }
}
focused_test_passed = focused_test_contract[:present] && focused_test_contract[:markers].values.all?
templates_version_path = File.join(template_root, 'templates-version.json')
templates_version = File.file?(templates_version_path) ? JSON.parse(File.read(templates_version_path)) : {}
templates_version_keys = templates_version.keys.sort
templates_version_stale = templates_version_keys != templates.sort || templates_version.values.any? { |version| version.to_s.strip.empty? }
template_platform_mismatches = (templates - creator_platforms - deprecated_template_candidates).sort
deprecated_templates = (templates & deprecated_template_candidates).sort
missing_creator_templates = (configured_platforms & creator_platforms) - templates
unknown_configured_platforms = configured_platforms - creator_platforms
builder_only_platforms = configured_platforms - policy_platforms
policy_only_platforms = policy_platforms - configured_platforms
platform_authority_conflicts = {
  builderOnly: builder_only_platforms,
  policyOnly: policy_only_platforms
}
production_ts = Dir.glob(File.join(client, 'assets/**/*.ts')).reject { |path| path.include?('/Legacy/') }
bridge_sources = production_ts.select do |path|
  text = File.binread(path).force_encoding('UTF-8').scrub
  text.match?(/window\.jsb\.|jsb\.reflection|require\(['"]jsb-adapter/) && !text.match?(/import\s*\{[^}]*native[^}]*\}\s*from\s*['"]cc['"]/)
end.map { |path| path.delete_prefix(client + '/') }
creator_version = JSON.parse(File.read(File.join(client, 'package.json'))).dig('creator', 'version')
polyfill_configured = builder_profile.dig('common', 'polyfills', 'asyncFunctions') == true
checks = { templates_only_for_built_platforms: (templates - creator_platforms - deprecated_template_candidates).empty? && missing_creator_templates.empty? && unknown_configured_platforms.empty?,
           template_scripts_are_creator_3_8_8_outputs: unknown_script_references.empty? && creator_version == '3.8.8',
           project_scripts_have_focused_contract: focused_test_passed && allowed_project_script_references.sort == required_project_script_references,
           builder_client_platform_authority_matches: platform_authority_conflicts.values.all?(&:empty?),
           templates_version_current: !templates_version_stale,
           generated_polyfill_has_explicit_current_build_reason: !script_refs.include?('src/polyfills.bundle.js') || polyfill_configured,
           no_deprecated_native_bridge_source: bridge_sources.empty? }
result = { task: 'UNUSED19', passed: checks.values.all?, checks: checks, templates: templates,
           configuredPlatforms: configured_platforms, policyPlatforms: policy_platforms,
           creatorPlatforms: creator_platforms, deprecatedTemplateCandidates: deprecated_template_candidates,
           deprecatedTemplates: deprecated_templates.map { |name| { name: name, status: 'deprecated/isolated-candidate' } },
           templatePlatformMismatches: template_platform_mismatches, missingCreatorTemplates: missing_creator_templates,
           unknownConfiguredPlatforms: unknown_configured_platforms,
           platformAuthorityConflicts: platform_authority_conflicts,
           templatesVersion: { path: 'build-templates/templates-version.json', keys: templates_version_keys, stale: templates_version_stale },
           scriptReferences: script_refs, creatorOutputReferences: creator_output_references,
           requiredProjectScriptReferences: required_project_script_references,
           allowedProjectScriptReferences: allowed_project_script_references, unknownScriptReferences: unknown_script_references,
           focusedTestContract: focused_test_contract,
           nativeBridgeSources: bridge_sources, polyfill: { enabled: polyfill_configured, owner: 'Creator 3.8.8 async-function transform' } }
FileUtils.mkdir_p(File.join(root, 'build-only/audit'))
File.write(File.join(root, 'build-only/audit/unused19-creator-build-templates.json'), JSON.pretty_generate(result))
puts JSON.generate(result)
abort('UNUSED19 failed') unless result[:passed]
