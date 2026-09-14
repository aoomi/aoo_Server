require 'json'
require 'fileutils'

root = File.expand_path('..', __dir__)
client = File.expand_path('../Client', root)
template_root = File.join(client, 'build-templates')
templates = Dir.exist?(template_root) ? Dir.children(template_root).select { |name| File.directory?(File.join(template_root, name)) }.sort : []
builder_profile = JSON.parse(File.read(File.join(client, 'profiles/v2/packages/builder.json')))
task_map = builder_profile.dig('BuildTaskManager', 'taskMap') || {}
configured_platforms = task_map.values.map { |task| task.dig('options', 'platform') }.compact.uniq.sort
template_files = Dir.glob(File.join(template_root, '**/*')).select { |path| File.file?(path) }
template_text = template_files.map { |path| File.read(path) }.join("\n")
script_refs = template_text.scan(/<script[^>]+src=["']([^"']+)/).flatten
known_creator_outputs = %w[src/polyfills.bundle.js src/system.bundle.js src/import-map.json]
production_ts = Dir.glob(File.join(client, 'assets/**/*.ts')).reject { |path| path.include?('/Legacy/') }
bridge_sources = production_ts.select do |path|
  text = File.binread(path).force_encoding('UTF-8').scrub
  text.match?(/window\.jsb\.|jsb\.reflection|require\(['"]jsb-adapter/) && !text.match?(/import\s*\{[^}]*native[^}]*\}\s*from\s*['"]cc['"]/)
end.map { |path| path.delete_prefix(client + '/') }
creator_version = JSON.parse(File.read(File.join(client, 'package.json'))).dig('creator', 'version')
polyfill_configured = builder_profile.dig('common', 'polyfills', 'asyncFunctions') == true
checks = { templates_only_for_built_platforms: (templates - configured_platforms).empty?,
           template_scripts_are_creator_3_8_8_outputs: (script_refs - known_creator_outputs).empty? && creator_version == '3.8.8',
           generated_polyfill_has_explicit_current_build_reason: !script_refs.include?('src/polyfills.bundle.js') || polyfill_configured,
           no_deprecated_native_bridge_source: bridge_sources.empty? }
result = { task: 'UNUSED19', passed: checks.values.all?, checks: checks, templates: templates,
           configuredPlatforms: configured_platforms, scriptReferences: script_refs,
           nativeBridgeSources: bridge_sources, polyfill: { enabled: polyfill_configured, owner: 'Creator 3.8.8 async-function transform' } }
FileUtils.mkdir_p(File.join(root, 'work/audit'))
File.write(File.join(root, 'work/audit/unused19-creator-build-templates.json'), JSON.pretty_generate(result))
puts JSON.generate(result)
abort('UNUSED19 failed') unless result[:passed]
