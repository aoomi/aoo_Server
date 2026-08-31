#!/usr/bin/env ruby
require 'json';require 'fileutils';root=File.expand_path('..',__dir__);client=File.expand_path('../Client',root)
files=Dir.glob(File.join(client,'assets/Common/Code/Runtime/CompatibilityApp/**/*.ts'))
intervals=files.select{|f|File.read(f).include?('setInterval')};uncleared=intervals.reject{|f|File.read(f).include?('clearInterval')}
forever=files.select{|f|File.read(f).match?(/repeatForever|repeat\s*\(\s*Number\.MAX/i)}
forms=File.read(File.join(client,'assets/Common/Code/Runtime/ui/LegacyFormManager.ts'))
scope=File.read(File.join(client,'assets/Common/Code/Runtime/CompatibilityApp/ui/AnimationLifecycleScope.ts'))
checks={'interval_lifecycle'=>uncleared.empty?,'no_unowned_infinite_animation'=>forever.empty?,'form_close_stops_tree'=>forms.include?('stopAnimationTree(this.node)'),
 'scope_idempotent_close'=>scope.include?('if (this.closed) return'),'tween_stop'=>scope.include?('Tween.stopAllByTarget')}
r={'task'=>'LOOP14','passed'=>checks.values.all?,'checks'=>checks,'intervalFiles'=>intervals.size,'unownedIntervals'=>uncleared,'infiniteAnimationFiles'=>forever}
out=File.join(root,'work/audit/loop14-ui-animation.json');FileUtils.mkdir_p(File.dirname(out));File.write(out,JSON.pretty_generate(r)+"\n");puts JSON.generate(r);exit(r['passed'] ? 0 : 1)
