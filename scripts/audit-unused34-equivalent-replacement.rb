#!/usr/bin/env ruby
require 'json';require 'pathname'
root=Pathname(__dir__).join('..').expand_path;doc=root.join('docs/依赖等价替代证明矩阵.md').read
dimensions=['输入等价','输出等价','异常等价','并发/时序','平台','历史格式','当前结论'];pairs=['Gson/Fastjson','MINA WebSocket','Joda-Time','Commons Codec/IO','Disruptor 3.x','OkHttp 3/4','RocketMQ 4.x client']
checks={allDimensionsRequired:dimensions.all?{|v|doc.include?(v)},allReplacementPairsClassified:pairs.all?{|v|doc.include?(v)},partialReplacementRetained:doc.scan('并存隔离').length>=4,noDeletionWithoutEvidence:doc.include?('任一列没有可执行证据时')&&doc.include?('不得删除旧能力'),historicalTestsPassed:JSON.parse(root.join('work/audit/unused33-historical-read-protection.json').read)['status']=='passed'}
errors=checks.reject{|_,v|v}.keys;report={task:'UNUSED34',status:errors.empty? ? 'passed':'failed',checks:checks,replacementPairs:pairs,dimensions:dimensions,errors:errors};root.join('work/audit/unused34-equivalent-replacement.json').write(JSON.pretty_generate(report)+"\n");abort("UNUSED34 failed: #{errors.join(', ')}")unless errors.empty?;puts "UNUSED34 passed: #{pairs.length} replacement paths classified across #{dimensions.length} dimensions"
