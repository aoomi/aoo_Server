#!/usr/bin/env ruby
# frozen_string_literal: true

require 'json'
require 'fileutils'

root = File.expand_path('..', __dir__)
sources = Dir.glob(File.join(root, 'server/**/src/main/**/*.java'))
             .reject { |path| path.match?(%r{/(?:Provider|provider)/}) }
read = ->(path) { File.read(path, encoding: 'UTF-8', invalid: :replace, undef: :replace) }
relative = ->(path) { path.delete_prefix(root + '/') }

patterns = {
  'unbounded_loop' => /\bwhile\s*\(\s*true\s*\)|\bfor\s*\(\s*;\s*;\s*\)/,
  'cached_thread_pool' => /Executors\.newCachedThreadPool\s*\(/,
  'unbounded_jdk_queue' => /new\s+(?:LinkedBlockingQueue|LinkedTransferQueue|ConcurrentLinkedQueue|ConcurrentLinkedDeque)(?:\s*<[^;()]*>)?\s*\(\s*\)/
}
findings = patterns.transform_values do |pattern|
  sources.map { |path| relative.call(path) if read.call(path).match?(pattern) }.compact
end

okhttp = read.call(File.join(root, 'server/LegacyAccountServer/common/src/main/java/com/ddm/server/http/configuration/OKHttpConfig.java'))
mongo = read.call(File.join(root, 'server/LegacyAccountServer/dao/src/main/java/server/aoo/dao/config/MongoSettingsProperties.java'))
invariants = {
  'okhttp_timeouts_use_milliseconds' => okhttp.scan(/TimeUnit\.MILLISECONDS/).length >= 3,
  'okhttp_keepalive_uses_seconds' => okhttp.include?('getKeepAliveDurationSec(), TimeUnit.SECONDS'),
  'mongo_read_timeout_is_finite' => mongo.match?(/socketTimeout\s*=\s*[1-9][0-9]*/),
  'mongo_idle_lifetime_is_finite' => mongo.match?(/maxConnectionIdleTime\s*=\s*[1-9][0-9]*/) && mongo.match?(/maxConnectionLifeTime\s*=\s*[1-9][0-9]*/),
  'mongo_client_has_destroy_method' => read.call(File.join(root, 'server/LegacyAccountServer/dao/src/main/java/server/aoo/dao/config/MongoGameDataSourceConfiguration.java')).include?('destroyMethod = "close"')
}

largest = Dir.glob(File.join(root, '{server,database,tools}/**/*'), File::FNM_EXTGLOB)
             .select { |path| File.file?(path) }.map { |path| [relative.call(path), File.size(path)] }
             .sort_by { |(_, size)| -size }.first(20).to_h
failures = findings.values.flatten + invariants.map { |name, passed| name unless passed }.compact
result = {
  'schemaVersion' => 1,
  'task' => 'R11',
  'passed' => failures.empty?,
  'scannedProductionJavaFiles' => sources.length,
  'findings' => findings,
  'invariants' => invariants,
  'largestFilesBytes' => largest,
  'failureCount' => failures.length
}
output = File.join(root, 'work/audit/r11-performance-stability.json')
FileUtils.mkdir_p(File.dirname(output))
File.write(output, JSON.pretty_generate(result) + "\n")
puts JSON.generate('passed' => result['passed'], 'scanned' => sources.length, 'failureCount' => failures.length)
exit(result['passed'] ? 0 : 1)
