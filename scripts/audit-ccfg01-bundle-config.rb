#!/usr/bin/env ruby
require 'json'
require 'digest'
require 'fileutils'

root = File.expand_path('..', __dir__)
client = File.expand_path('../Client', root)
config_root = File.join(client, 'development/configs')
files = Dir.glob(File.join(config_root, '**/*')).select { |path| File.file?(path) && !path.end_with?('.meta') && File.basename(path) != '.DS_Store' }
bundle_files = files.select { |path| path.include?('/bundles/') || File.basename(path).include?('bundle') }
entries = bundle_files.map do |path|
  parsed = JSON.parse(File.read(path)) rescue nil
  {
    path: path.delete_prefix(client + '/'),
    bytes: File.size(path),
    sha256: Digest::SHA256.file(path).hexdigest,
    example: File.basename(path).include?('example'),
    parseable: !parsed.nil?
  }
end

checks = {
  hasProductionBundleConfig: entries.any? { |entry| !entry[:example] },
  noExampleOnlyConfig: entries.none? { |entry| entry[:example] },
  hasBackendGameCodeClosure: false,
  hasDependencyAndVersionClosure: false
}
report = {schemaVersion: 1, files: entries, checks: checks, passed: checks.values.all?}
out = File.join(root, 'docs/generated/ccfg01-bundle-config.json')
FileUtils.mkdir_p(File.dirname(out))
File.write(out, JSON.pretty_generate(report) + "\n")
abort "CCFG01 blocked: development bundle configuration is example-only and has no backend game/dependency/version closure" unless report[:passed]
