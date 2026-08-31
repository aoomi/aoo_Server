#!/usr/bin/env ruby
require 'json'
require 'fileutils'
root = File.expand_path('..', __dir__)
config = File.read(File.join(root, 'server/LegacyCommon/conf/logback_game.xml'))
client = File.read(File.join(root, '../Client/assets/Common/Code/Runtime/observability/RuntimeLogGovernor.ts'))
bootstrap = File.read(File.join(root, '../Client/assets/Common/Code/Runtime/bootstrap/LoginScreenBootstrap.ts'))
checks = {
  production_root_info: config.include?('<level value="INFO"/>') && !config.include?('<level value="DEBUG"/>'),
  heartbeat_and_polling_suppressed: %w[aoo.heartbeat aoo.polling].all? { |name| config.include?(name) },
  duplicate_rate_limit: config.include?('DuplicateMessageFilter') && config.include?('<AllowedRepetitions>3</AllowedRepetitions>'),
  client_heartbeat_sampling: client.match?(/heartbeat\|heart\.\?beat\|poll/),
  client_signature_rate_limit: client.include?('maxPerMinute') && client.include?('window.count >= maxPerMinute'),
  installed_at_bootstrap: bootstrap.include?('RuntimeLogGovernor.install()')
}
passed = checks.values.all?
out = File.join(root, 'docs/generated/logreal11-noise-policy.json')
FileUtils.mkdir_p(File.dirname(out)); File.write(out, JSON.pretty_generate({task: 'LOGREAL11', status: passed ? 'passed' : 'failed', checks: checks}) + "\n")
abort "LOGREAL11 failed: #{checks}" unless passed
puts 'LOGREAL11 PASS: runtime noise policy enforced'
