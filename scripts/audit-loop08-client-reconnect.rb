#!/usr/bin/env ruby
require 'json'; require 'fileutils'
root=File.expand_path('..',__dir__)
path=File.expand_path('../Client/assets/Common/Code/Runtime/CompatibilityApp/network/LegacyWebSocketClient.ts',root)
source=File.read(path)
checks={
  'bounded_attempts'=>source.include?('MAX_RECONNECT_ATTEMPTS') && source.match?(/attempt\s*<\s*LegacyWebSocketClient\.MAX_RECONNECT_ATTEMPTS/),
  'exponential_backoff'=>source.include?('2 **'),
  'maximum_delay'=>source.include?('RECONNECT_MAX_DELAY_MS'),
  'jitter'=>source.include?('Math.random()'),
  'exhaustion_latch'=>source.include?('reconnectExhausted = true'),
  'user_recovery_entry'=>source.include?('onRecoveryRequired') && source.include?('forceReconnect()'),
  'queue_budget'=>source.include?('MAX_PENDING_REQUESTS')
}
result={'task'=>'LOOP08','passed'=>checks.values.all?,'checks'=>checks,'source'=>path}
out=File.join(root,'work/audit/loop08-client-reconnect.json'); FileUtils.mkdir_p(File.dirname(out)); File.write(out,JSON.pretty_generate(result)+"\n")
puts JSON.generate(result); exit(result['passed'] ? 0 : 1)
