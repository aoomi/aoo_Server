#!/usr/bin/env ruby
require 'fileutils'
require 'json'

root = File.expand_path('..', __dir__)
java_files = Dir.glob(File.join(root, 'server/**/*.java')).reject { |path| path.include?('/target/') || path.include?('/build/') }
native_sites = java_files.select { |path| File.read(path).match?(/ObjectInputStream|\.readObject\s*\(/) }
external_sites = native_sites.select do |path|
  relative = path.delete_prefix(root + '/')
  relative.end_with?('/JavaSerializeUtil.java') || relative.end_with?('/ObjectUtil.java') || relative.end_with?('/Lists.java')
end
redis = File.read(File.join(root, 'server/LegacyCommon/src/com/ddm/server/common/redis/RedisUtil.java'))
domain_decoder = File.join(root, 'server/GameCommon/src/main/java/com/aoo/bcg/common/serialization/DomainJsonDecoder.java')
compat_test = File.join(root, 'server/GameCommon/src/test/java/com/aoo/bcg/common/serialization/HistoricalFormatCompatibilityTest.java')
checks = {
  json_compatibility_boundary_exists: File.file?(domain_decoder),
  historical_json_regression_exists: File.file?(compat_test),
  redis_uses_native_java_deserialization: redis.include?('JavaSerializeUtil.unSerialize'),
  no_unrestricted_external_native_deserialization: external_sites.empty?
}
result = {
  task: 'THIRD09',
  passed: checks.values.all?,
  status: checks.values.all? ? 'passed' : 'failed-skipped',
  checks: checks,
  nativeDeserializationFiles: native_sites.map { |path| path.delete_prefix(root + '/') }.sort,
  externallyReachableUtilityFiles: external_sites.map { |path| path.delete_prefix(root + '/') }.sort,
  requiredRemediation: 'replace Redis Java object decoding with a size/depth/class allowlisted compatibility adapter, then confine remaining native serialization to trusted in-memory clone operations'
}
out = File.join(root, 'work/audit/third09-deserialization-boundary.json')
FileUtils.mkdir_p(File.dirname(out))
File.write(out, JSON.pretty_generate(result) + "\n")
puts "THIRD09 #{result[:status]}: #{external_sites.length} externally reachable native-deserialization utility files"
exit(result[:passed] ? 0 : 1)
