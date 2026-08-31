require 'json';require 'fileutils';require 'digest'
root=File.expand_path('..',__dir__);roots=%w[test-move/scjymj test-move/xcpdk];entries=[]
roots.each do |rel|
  base=File.join(root,rel);Dir[File.join(base,'**/*')].select{|p|File.file?(p)}.each do |path|
    entries<<{stagingPath:path.delete_prefix(root+'/'),sha256:Digest::SHA256.file(path).hexdigest,disposition:'UNCLASSIFIED',formalTarget:nil,evidence:nil}
  end
end
allowed=%w[MIGRATED IDENTICAL REWRITE_REQUIRED APPROVED_OBSOLETE BLOCKED]
checks={every_staged_file_recorded:entries.length==roots.sum{|r|Dir[File.join(root,r,'**/*')].count{|p|File.file?(p)}},no_unclassified_disposition:entries.all?{|e|allowed.include?(e[:disposition])},every_nonobsolete_has_target:entries.all?{|e|%w[APPROVED_OBSOLETE BLOCKED].include?(e[:disposition])||e[:formalTarget]},every_entry_has_evidence:entries.all?{|e|e[:evidence]}}
ledger={schemaVersion:1,task:'STAGE05',allowedDispositions:allowed,entries:entries};ledger_path=File.join(root,'docs/generated/staging-file-disposition.json');FileUtils.mkdir_p(File.dirname(ledger_path));File.write(ledger_path,JSON.pretty_generate(ledger)+"\n");result={task:'STAGE05',passed:checks.values.all?,checks:checks,entryCount:entries.length,ledger:'docs/generated/staging-file-disposition.json'};out=File.join(root,'work/audit/stage05-disposition-ledger.json');FileUtils.mkdir_p(File.dirname(out));File.write(out,JSON.pretty_generate(result)+"\n");puts "STAGE05 #{result[:passed]?'passed':'failed'}: #{entries.length} staged files classified";exit(result[:passed]?0:1)
