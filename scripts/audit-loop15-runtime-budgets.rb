#!/usr/bin/env ruby
require 'json';require 'fileutils';root=File.expand_path('..',__dir__)
w=File.read(File.join(root,'server/GameSPI/src/main/java/com/aoo/bcg/gamespi/RuntimeWorkBudget.java'));a=File.read(File.join(root,'server/GameSPI/src/main/java/com/aoo/bcg/gamespi/AlgorithmBudget.java'))
checks={'algorithm_input_steps_depth_time'=>%w[maximumInput maximumSteps maximumDepth maximumElapsed].all?{|x|a.include?(x)},'runtime_steps'=>w.include?('maxSteps'),
 'runtime_depth'=>w.include?('maxDepth'),'runtime_queue'=>w.include?('maxQueueItems'),'runtime_memory'=>w.include?('maxBytes'),'runtime_time'=>w.include?('deadlineNanos')}
r={'task'=>'LOOP15','passed'=>checks.values.all?,'checks'=>checks};out=File.join(root,'work/audit/loop15-runtime-budgets.json');FileUtils.mkdir_p(File.dirname(out));File.write(out,JSON.pretty_generate(r)+"\n");puts JSON.generate(r);exit(r['passed'] ? 0 : 1)
