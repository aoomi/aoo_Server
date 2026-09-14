#!/usr/bin/env ruby
require 'json'; require 'pathname'
root=Pathname(__dir__).join('..').expand_path; ledger=root.join('docs/2.22必要依赖保护清单.md'); text=ledger.read
entries=['Disruptor','OkHttp','Apache MINA','Gson','Fastjson2 compatibility','Joda-Time','Commons Codec/IO','RocketMQ client']
columns=['支撑功能','调用方式/边界','数据格式','插件/产物','缺失影响','等价替代判定']
checks={ledgerExists:ledger.exist?,allCapabilitiesListed:entries.all?{|v|text.include?(v)},allRequiredColumns:columns.all?{|v|text.include?(v)},deletionForbiddenUntilProof:text.include?('未完成输入/输出、异常、并发、平台和历史格式回归前禁止删除'),historicalGateLinked:text.include?('`UNUSED26`')&&text.include?('`UNUSED28`')}
errors=checks.reject{|_,v|v}.keys; report={task:'UNUSED31',status:errors.empty? ? 'passed':'failed',checks:checks,protectedCapabilities:entries,ledger:'docs/2.22必要依赖保护清单.md',errors:errors}; out=root.join('work/audit/unused31-legacy-necessary-ledger.json');out.write(JSON.pretty_generate(report)+"\n");abort("UNUSED31 failed: #{errors.join(', ')}")unless errors.empty?;puts "UNUSED31 passed: #{entries.length} necessary 2.22 capability groups protected"
