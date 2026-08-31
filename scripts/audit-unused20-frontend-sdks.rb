#!/usr/bin/env ruby
require 'json'
require 'pathname'

root = Pathname(__dir__).join('..').expand_path
client = root.join('../Client').expand_path
assets = client.join('assets')
sdk_names = /(?:firebase|sentry|bugly|umeng|talkingdata|jpush|agora|rongcloud|adjust|appsflyer)/i
source_files = Dir[assets.join('**/*.{ts,js}').to_s]
manifest_files = Dir[client.join('**/package.json').to_s].reject { |p| p.include?('/node_modules/') || p.include?('/temp/') || p.include?('/library/') }
sdk_sources = source_files.select { |path| File.basename(path).match?(sdk_names) || File.read(path).match?(sdk_names) }
sdk_packages = manifest_files.flat_map do |path|
  json = JSON.parse(File.read(path))
  %w[dependencies optionalDependencies peerDependencies].flat_map do |group|
    (json[group] || {}).keys.grep(sdk_names).map { |name| { manifest: path.delete_prefix(client.to_s + '/'), group: group, name: name } }
  end
end

compat = assets.join('Common/Code/Runtime/CompatibilityApp/platform')
runtime = assets.join('Common/Code/Runtime/platform/LegacyPlatformRuntime.ts')
required = %w[LegacyAnalyticsService.ts LegacyVoiceService.ts].map { |name| compat.join(name) }
duplicate = %w[LegacyAnalyticsService.ts LegacyVoiceService.ts].map { |name| assets.join('Common/Code/Runtime/platform', name) }.select(&:exist?)
runtime_text = runtime.exist? ? runtime.read : ''
initialized = required.to_h { |path| [path.basename.to_s, path.exist? && runtime_text.include?(path.basename('.ts').to_s)] }

errors = []
errors << "third-party SDK sources remain: #{sdk_sources.join(', ')}" unless sdk_sources.empty?
errors << "production SDK packages remain: #{sdk_packages.map { |x| x[:name] }.join(', ')}" unless sdk_packages.empty?
errors << "duplicate platform services remain: #{duplicate.join(', ')}" unless duplicate.empty?
errors << "retained compatibility service lacks runtime entry: #{initialized.select { |_k, v| !v }.keys.join(', ')}" unless initialized.values.all?

report = {
  task: 'UNUSED20', status: errors.empty? ? 'passed' : 'failed',
  manifests: manifest_files.map { |p| p.delete_prefix(client.to_s + '/') }.sort,
  thirdPartySdkSources: sdk_sources.map { |p| p.delete_prefix(client.to_s + '/') }.sort,
  productionSdkPackages: sdk_packages, retainedPlatformAdapters: initialized,
  removedDuplicateAdapters: %w[assets/Common/Code/Runtime/platform/LegacyAnalyticsService.ts assets/Common/Code/Runtime/platform/LegacyVoiceService.ts],
  policy: 'No uninitialized third-party SDK or duplicate adapter; required 2.22 analytics and voice capabilities remain behind one initialized compatibility boundary.',
  errors: errors
}
out = root.join('work/audit/unused20-frontend-sdks.json')
out.dirname.mkpath
out.write(JSON.pretty_generate(report) + "\n")
abort(errors.join("\n")) unless errors.empty?
puts 'UNUSED20 passed: no third-party SDK package/source; retained analytics and voice adapters have one runtime entry'
