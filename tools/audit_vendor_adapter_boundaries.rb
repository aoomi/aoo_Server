#!/usr/bin/env ruby
# frozen_string_literal: true

require 'json'
require 'time'
require 'fileutils'

root = File.expand_path('..', __dir__)
client = File.expand_path('../Client/assets', root)
output = File.join(root, 'work/audit/vendor-adapter-boundaries.json')
vendors = {
  rocketmq: /org\.apache\.rocketmq/,
  redis: /redis\.clients|Jedis/,
  mongodb: /com\.mongodb|MongoClient/,
  wechat: /(?:weixin|wechat|wx\.)/i,
  alipay: /(?:alipay|alipay-sdk)/i,
  cloudVendor: /(?:aliyun|tencentcloud|aws\.|google\.cloud)/i,
  platformSdk: /(?:jsb\.|native\.ref|window\.wx|ApplePay|GooglePlay)/
}.freeze
allowed = %r{/(?:adapter|adapters|platform|infrastructure|gateway|network|persistence|repository|config)/}i
rows = []
[[File.join(root, 'server'), %w[.java .kt]], [client, %w[.ts .js]]].each do |base, extensions|
  Dir.glob(File.join(base, '**/*')).each do |path|
    next unless File.file?(path) && extensions.include?(File.extname(path))
    next if path.include?('/target/') || path.include?('/build/') || path.include?('/CompatibilityApp/')
    text = File.read(path, encoding: 'UTF-8', invalid: :replace, undef: :replace)
               .encode('UTF-8', invalid: :replace, undef: :replace, replace: '')
    relative = path.delete_prefix(root + '/').delete_prefix(client + '/')
    vendors.each do |vendor, pattern|
      next unless text.match?(pattern)
      rows << {vendor: vendor, path: relative, adapterBoundary: relative.match?(allowed)}
    end
  end
end
violations = rows.reject { |row| row[:adapterBoundary] }
summary = {vendorReferences: rows.size, adapterReferences: rows.size - violations.size,
           directBusinessReferences: violations.size, affectedFiles: violations.map { |row| row[:path] }.uniq.size,
           passed: rows.any? && violations.empty?}
report = {schemaVersion: 1, generatedAt: Time.now.utc.iso8601,
          invariant: 'Business and game code depends on project ports; only adapter/infrastructure boundaries import vendor or framework APIs.',
          summary: summary, references: rows, violations: violations}
FileUtils.mkdir_p(File.dirname(output))
File.write(output, JSON.pretty_generate(report) + "\n", mode: 'w:UTF-8')
puts JSON.generate(summary)
exit(summary[:passed] ? 0 : 2) if ENV['AOO_ENFORCE_VENDOR_ADAPTER_GATE'] == '1'
