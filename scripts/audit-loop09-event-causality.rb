#!/usr/bin/env ruby
require 'json'; require 'fileutils'
root=File.expand_path('..',__dir__); client=File.expand_path('../Client',root)
j=File.read(File.join(root,'server/GameCommon/src/main/java/com/aoo/bcg/common/event/CausalDispatchGuard.java'))
t=File.read(File.join(client,'assets/Common/Code/Runtime/CompatibilityApp/core/CausalEventGuard.ts'))
p=File.read(File.join(client,'assets/Common/Code/Runtime/CompatibilityApp/platform/LegacyPlatformEvents.ts'))
n_path=File.join(client,'assets/Common/Code/Runtime/network/ProtocolClient.ts')
n=File.read(n_path)
if (m=n.match(/from\s+['"]([^'"]*network\/ProtocolClient)['"]/))
  target=File.expand_path("#{m[1]}.ts",File.dirname(n_path))
  n << "\n" << File.read(target) if File.file?(target)
end
checks={'backend_cycle_guard'=>j.include?('causal cycle detected'),'backend_depth_budget'=>j.include?('maxDepth'),
 'client_cycle_guard'=>t.include?('active.has(key)'),'store_ui_platform_boundary'=>p.include?("dispatch('platform'"),
 'network_boundary'=>n.include?("dispatch('network'"),'trace_identity'=>p.include?('traceId')&&n.include?('traceId')}
r={'task'=>'LOOP09','passed'=>checks.values.all?,'checks'=>checks}; out=File.join(root,'work/audit/loop09-event-causality.json'); FileUtils.mkdir_p(File.dirname(out)); File.write(out,JSON.pretty_generate(r)+"\n"); puts JSON.generate(r); exit(r['passed'] ? 0 : 1)
