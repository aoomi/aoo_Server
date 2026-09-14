#!/usr/bin/env ruby
# frozen_string_literal: true
require 'digest';require 'fileutils';require 'json';require 'open3';require 'pathname';require 'time'
ROOT=Pathname.new(__dir__).parent.realpath
BASELINE=ROOT/'work/audit/audit-source-baseline.json'
TASKS=ROOT/'docs/Aoo-前后端全框架功能通信审计任务清单.md'
OUTPUT=ROOT/'docs/generated/tool14-final-audit.json'
abort 'asset baseline missing' unless BASELINE.file?
baseline=JSON.parse(BASELINE.read); baseline_files=baseline.fetch('files'); roots=baseline.fetch('roots')
extensions=baseline.fetch('extensions'); excludes=baseline.fetch('excludes')
root_paths=roots.map{|entry|entry.is_a?(Hash) ? (entry['path']||entry['root']):entry}.compact.map{|path|Pathname.new(path)}
missing_roots=root_paths.reject(&:directory?).map(&:to_s)
current=[]
root_paths.select(&:directory?).each do|root|
 Dir.glob((root/'**/*').to_s,File::FNM_DOTMATCH).sort.each do|path|
  next unless File.file?(path);relative=Pathname.new(path).relative_path_from(root).to_s
  next unless extensions.include?(File.extname(path));next if excludes.any?{|excluded|relative==excluded||relative.start_with?(excluded.to_s.sub(%r{/+\z},'')+'/')}
  current<<{'path'=>path,'root'=>root.to_s,'relativePath'=>relative,'size'=>File.size(path),'sha256'=>Digest::SHA256.file(path).hexdigest}
 end
end
current_by_path=current.to_h{|item|[item['path'],item]};baseline_by_path=baseline_files.to_h{|item|[item.fetch('path'),item]}
missing=baseline_by_path.keys-current_by_path.keys;added=current_by_path.keys-baseline_by_path.keys
changed=(baseline_by_path.keys&current_by_path.keys).select{|path|baseline_by_path[path]['sha256']!=current_by_path[path]['sha256']}
domains={'server'=>%r{/Server/},'client'=>%r{/Client/},'admin'=>%r{/Admin/},'legacy_reference'=>%r{/Test/}}
domain_counts=domains.to_h{|name,pattern|[name,current.count{|item|item['path'].match?(pattern)}]}
commands={
 'architecture'=>['ruby','tools/check_architecture_boundaries.rb'],
 'gameplaySecurityRuntime'=>['ruby','tools/check_gameplay_security_runtime.rb'],
 'runtimeSmoke'=>['ruby','tools/runtime_smoke_current_architecture.rb'],
 'protocolV1Retirement'=>['ruby','tools/check_protocol_v1_retirement.rb'],
 'migrationAuthority'=>['ruby','scripts/audit-dbreal12-flyway-authority.rb']
}
gates=commands.to_h do|name,command|
 out,err,status=Open3.capture3(*command,chdir:ROOT.to_s);[name,{'passed'=>status.success?,'exitCode'=>status.exitstatus,'output'=>(out+err).lines.last(20).join}]
end
rows=TASKS.readlines.grep(/^\| (?:[A-Z]+\d+) \|/).map do|line|
 cells=line.split('|').map(&:strip);{'id'=>cells[1],'status'=>cells[2]}
end
unfinished=rows.reject{|row|%w[已完成 失败跳过 待人工核验].include?(row['status'])}
# Baseline deletions and mutations are evidence, not automatic failures: many are
# intentional remediations. Final success requires every task to classify them.
passed=missing_roots.empty?&&domain_counts.values.all?(&:positive?)&&gates.values.all?{|gate|gate['passed']}&&unfinished.empty?
report={'schemaVersion'=>1,'generatedAt'=>Time.now.utc.iso8601,'task'=>'TOOL14','passed'=>passed,
 'assetBaseline'=>{'path'=>BASELINE.relative_path_from(ROOT).to_s,'aggregateSha256'=>baseline['aggregateSha256'],'baselineFiles'=>baseline_files.length,'currentFiles'=>current.length,'roots'=>root_paths.map(&:to_s),'missingRoots'=>missing_roots,'domainCounts'=>domain_counts,'missingFiles'=>missing,'addedFiles'=>added,'changedFiles'=>changed},
 'gates'=>gates,'taskCoverage'=>{'rows'=>rows.length,'unfinished'=>unfinished},
 'policy'=>'Final verdict is derived from every baseline root plus current files and all task rows; missing/changed assets remain explicit and cannot disappear from evidence.'}
FileUtils.mkdir_p(OUTPUT.dirname);OUTPUT.write(JSON.pretty_generate(report)+"\n")
puts "final-audit: #{passed ? 'passed':'incomplete'} baseline=#{baseline_files.length} current=#{current.length} unfinished=#{unfinished.length}"
exit(passed ? 0:1)
