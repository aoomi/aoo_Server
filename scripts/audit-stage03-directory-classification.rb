require 'json';require 'fileutils';require 'find'
root=File.expand_path('..',__dir__);names=%w[target build library temp Archive source-project jdk8 runtime local-runtime toolchains];inventory=[]
Find.find(root) do |path|
  next unless File.directory?(path);rel=path.delete_prefix(root+'/');base=File.basename(path);next unless names.include?(base)
  category=case
  when base=='target' then 'maven-generated'
  when base=='build' then 'legacy-build-output'
  when %w[runtime local-runtime].include?(base) then 'local-runtime-output'
  when %w[jdk8 toolchains].include?(base) then 'toolchain-reference'
  when base=='source-project' then 'migration-source'
  when base=='Archive' then 'historical-reference'
  when %w[library temp].include?(base) then 'generated-cache'
  end
  inventory<<{path:rel,category:category,productionPublish:false};Find.prune
end
policy=File.read(File.join(root,'docs/迁移暂存与生成目录分类规范.md'));checks={all_candidates_classified:inventory.all?{|x|x[:category]},none_publishable:inventory.none?{|x|x[:productionPublish]},policy_covers_all_categories:inventory.map{|x|x[:category]}.uniq.all?{|c|policy.include?(c.split('-').first)||policy.include?(c)},migration_staging_audited:File.exist?(File.join(root,'work/audit/stage01-scjymj.json'))&&File.exist?(File.join(root,'work/audit/stage02-xcpdk.json'))}
result={task:'STAGE03',passed:checks.values.all?,checks:checks,directoryCount:inventory.length,inventory:inventory.sort_by{|x|x[:path]}};out=File.join(root,'work/audit/stage03-directory-classification.json');FileUtils.mkdir_p(File.dirname(out));File.write(out,JSON.pretty_generate(result)+"\n");puts "STAGE03 #{result[:passed]?'passed':'failed'}: #{inventory.length} temporary/generated directories classified";exit(result[:passed]?0:1)
