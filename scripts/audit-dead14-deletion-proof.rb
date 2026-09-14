require 'json';require 'fileutils'
root=File.expand_path('..',__dir__);manifest=JSON.parse(File.read(File.join(root,'config/deletion-proofs.json')));required=%w[staticReachability dynamicRuntime dataReferences buildArtifacts regressionSuite owner reviewedAt artifactSha256];failures=[]
manifest.fetch('approvedDeletions').each do |entry|
 missing=required.reject{|key|entry.key?(key)&&entry[key]!=false&&entry[key]!=nil&&entry[key]!=''};failures<<{path:entry['path'],missing:missing} unless missing.empty?
end
ledgers=%w[docs/未引用代码删除登记.md docs/数据库对象删除复核登记.md docs/前端资源删除复核登记.md];ledgers.each do |relative|
 path=File.join(root,relative);next unless File.exist?(path);File.foreach(path).with_index(1){|line,index|failures<<{path:relative,line:index+1,reason:'ledger claims deletion without proof manifest'} if line.match?(/\|\s*(?:可删除|已批准删除|已删除)\s*\|/)}
end
out=File.join(root,'work/audit/dead14-deletion-proof.json');FileUtils.mkdir_p(File.dirname(out));File.write(out,JSON.pretty_generate({task:'DEAD14',status:failures.empty? ? 'passed':'failed',approvedDeletionCount:manifest['approvedDeletions'].size,requiredEvidence:required,checkedLedgers:ledgers,findings:failures})+"\n");abort "DEAD14 deletion proof failed: #{failures}" unless failures.empty?
