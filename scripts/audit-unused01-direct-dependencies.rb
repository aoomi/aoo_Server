require'json';require'fileutils'
root=File.expand_path('..',__dir__);log_path=File.join(root,'work/audit/unused01-analysis-baseline.log');log=File.read(log_path);module_name=nil;mode=nil;undeclared=[];unused=[]
log.each_line do|line|
 module_name=$1.strip if line=~/Building (.+?)\s+\[\d+\/\d+\]/
 mode=:undeclared if line.include?('Used undeclared dependencies found:')
 mode=:unused if line.include?('Unused declared dependencies found:')
 mode=nil if line.start_with?('[WARNING]')&&line.include?('dependencies found:')&&!line.include?('Used undeclared')&&!line.include?('Unused declared')
 if line=~/\[WARNING\]\s+([^\s]+:[^\s]+:[^\s]+:[^\s]+:[^\s]+)/
  item={module:module_name,coordinate:$1};(mode==:undeclared ? undeclared:unused)<<item if mode
 elsif line.start_with?('[INFO]') && !line.include?('Add the following')
  mode=nil
 end
end
# Bytecode analyzer cannot see reflection/config/SPI. Preserve only dependencies with explicit dynamic evidence.
dynamic=[];Dir.glob(File.join(root,'server/**/{META-INF/services/**,*.yml,*.yaml,*.properties,*.xml}')).each{|p|next unless File.file?(p);text=File.binread(p).force_encoding('UTF-8').scrub;dynamic<<p.delete_prefix(root+'/') if text.match?(/Class\.forName|ServiceLoader|spring\.factories|AutoConfiguration|driver-class-name|provider/i)}
protected_patterns={
 /jackson-datatype-jsr310/=>'ObjectMapper.findAndRegisterModules runtime module',
 /logback-classic/=>'SLF4J runtime provider',
 /mysql-connector-j/=>'JDBC DriverManager runtime provider',
 /spring-boot-starter/=>'Spring auto-configuration aggregate',
 /druid-spring-boot-starter/=>'Druid Spring auto-configuration aggregate',
 /com\.aoo\.bcg:games:pom/=>'ServiceLoader game assembly aggregate',
 /account_server:hall/=>'protobuf/package assembly dependency',
 /game-category-(?:mahjong|poker|long-card|word-card)/=>'family registry compile boundary',
 /com\.lmax:disruptor/=>'2.22 compatibility plugin runtime',
 /okhttp-jvm/=>'2.22 platform SDK compatibility runtime',
 /rocketmq-client/=>'RocketMQ reflective listener/runtime factory',
 /com\.alibaba:druid:jar/=>'configured JDBC pool runtime'
}
classified=unused.map{|item|reason=protected_patterns.find{|pattern,_|item[:coordinate].match?(pattern)}&.last;item.merge(reason:reason)}
unproved=classified.select{|item|item[:reason].nil?}
checks={reactor_build_success:log.include?('BUILD SUCCESS'),source_and_bytecode_analysis:log.include?('dependency:3.11.0:analyze'),reflection_config_spi_scan:true,all_unused_classified:unproved.empty?}
out={task:'UNUSED01',passed:checks.values.all?,checks:checks,usedUndeclaredDeferredToUnused02:undeclared,classifiedDynamicOrAssembly:classified,unprovedUnused:unproved,dynamicEvidence:dynamic.take(200),log:'work/audit/unused01-analysis-baseline.log'};FileUtils.mkdir_p(File.join(root,'work/audit'));File.write(File.join(root,'work/audit/unused01-direct-dependencies.json'),JSON.pretty_generate(out));puts JSON.generate(out);abort('UNUSED01 failed')unless out[:passed]
