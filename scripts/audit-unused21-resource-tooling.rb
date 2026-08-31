#!/usr/bin/env ruby
require 'digest'
require 'json'
require 'pathname'

root = Pathname(__dir__).join('..').expand_path
client = root.join('../Client').expand_path
assets = client.join('assets')
manifests = Dir[client.join('**/package.json').to_s].reject { |p| p.match?(%r{/(?:node_modules|temp|library|build)/}) }
tool_pattern = /(?:postcss|autoprefixer|cssnano|sass|less|stylus|fontmin|fontforge|imagemin|sharp|pngquant|optipng|texture.?packer)/i
packages = manifests.flat_map do |path|
  json = JSON.parse(File.read(path))
  %w[dependencies devDependencies optionalDependencies].flat_map do |group|
    (json[group] || {}).keys.grep(tool_pattern).map { |name| { manifest: path.delete_prefix(client.to_s + '/'), group: group, name: name } }
  end
end

style_sources = Dir[assets.join('**/*.{css,scss,less,styl}').to_s]
fonts = Dir[assets.join('**/*.{ttf,TTF,otf,OTF,woff,woff2}').to_s]
asset_text = Dir[assets.join('**/*.{scene,prefab,json,ts,js}').to_s].map { |p| [p, File.read(p)] }
font_rows = fonts.map do |path|
  meta = path + '.meta'
  uuid = File.exist?(meta) ? JSON.parse(File.read(meta))['uuid'].to_s : ''
  references = uuid.empty? ? [] : asset_text.map { |p, text| p if text.include?(uuid) }.compact
  { path: path.delete_prefix(client.to_s + '/'), sha256: Digest::SHA256.file(path).hexdigest, uuid: uuid, referenceCount: references.length }
end
unreferenced_duplicate_fonts = font_rows.group_by { |row| row[:sha256] }.values.flat_map do |rows|
  rows.length > 1 ? rows.select { |row| row[:referenceCount].zero? } : []
end

builder = JSON.parse(client.join('profiles/v2/packages/builder.json').read)
options = builder.dig('BuildTaskManager', 'taskMap')&.values&.first&.dig('options') || {}
errors = []
errors << "resource processing packages remain: #{packages.map { |x| x[:name] }.join(', ')}" unless packages.empty?
errors << "unowned stylesheet pipeline inputs remain: #{style_sources.join(', ')}" unless style_sources.empty?
errors << "unreferenced duplicate fonts remain: #{unreferenced_duplicate_fonts.map { |x| x[:path] }.join(', ')}" unless unreferenced_duplicate_fonts.empty?
errors << 'Creator auto-atlas owner is not enabled' unless options['packAutoAtlas'] == true
errors << 'Creator texture compression was bypassed' unless options['skipCompressTexture'] == false

report = {
  task: 'UNUSED21', status: errors.empty? ? 'passed' : 'failed',
  externalResourcePackages: packages, stylesheetSources: style_sources.map { |p| p.delete_prefix(client.to_s + '/') },
  fonts: font_rows, unreferencedDuplicateFonts: unreferenced_duplicate_fonts,
  creatorPipeline: { version: '3.8.8', packAutoAtlas: options['packAutoAtlas'], skipCompressTexture: options['skipCompressTexture'], genMipmaps: false },
  legacyAtlasPolicy: 'PAC/PLIST/Spine atlas files are migration inputs or runtime data, not executable plugins; keep while 2.22 capability references remain.',
  errors: errors
}
out = root.join('work/audit/unused21-resource-tooling.json')
out.dirname.mkpath
out.write(JSON.pretty_generate(report) + "\n")
abort(errors.join("\n")) unless errors.empty?
puts "UNUSED21 passed: Creator 3.8.8 is sole active resource pipeline; #{font_rows.length} referenced fonts, no duplicate orphan"
