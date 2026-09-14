#!/usr/bin/env ruby
require 'json'; require 'fileutils'
root=File.expand_path('..',__dir__)
path=File.expand_path('../Client/assets/Common/Code/Runtime/network/LegacyWebSocketClient.ts',root)
source=File.read(path)
coordinator_path=File.expand_path('../Client/assets/Common/Code/Runtime/network/ReconnectCoordinator.ts',root)
coordinator=File.read(coordinator_path)
checks={
  'bounded_attempts'=>coordinator.match?(/maxAttempts\s*=\s*\d+/) && coordinator.match?(/attempt\s*<\s*maxAttempts/),
  'exponential_backoff'=>coordinator.include?('2 **'),
  'maximum_delay'=>coordinator.include?('Math.min(8_000'),
  'jitter'=>coordinator.include?('Math.random()'),
  'exhaustion_latch'=>coordinator.include?("slot.disconnect('TERMINAL')"),
  'user_recovery_entry'=>coordinator.include?('public async retry<T>') && coordinator.include?('cancelRetry'),
  'queue_budget'=>source.include?('MAX_PENDING_REQUESTS')
}
result={'task'=>'LOOP08','passed'=>checks.values.all?,'checks'=>checks,'sources'=>[path,coordinator_path]}
out=File.join(root,'work/audit/loop08-client-reconnect.json'); FileUtils.mkdir_p(File.dirname(out)); File.write(out,JSON.pretty_generate(result)+"\n")
puts JSON.generate(result); exit(result['passed'] ? 0 : 1)
