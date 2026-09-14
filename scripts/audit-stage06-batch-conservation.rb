require 'json';require 'fileutils'
root=File.expand_path('..',__dir__);pairs={'scjymj'=>['test-move/scjymj','server/SCJYMJ'],'xcpdk'=>['test-move/xcpdk','server/XCPDK']};dimensions={java:/\.java$/,configuration:/\.(?:json|ya?ml|properties|xml)$/,resource:/\.(?:png|jpe?g|atlas|txt|csv|tsv)$/,sql:/\.sql$/,protocol:/(?:proto|protocol|jsproto)/i,test:/(?:^|\/)(?:test|tests)(?:\/|$)/i};inventory={}
pairs.each do |name,(stage,formal)|
  inventory[name]={};dimensions.each do |kind,pattern|
    staged=Dir[File.join(root,stage,'**/*')].select{|p|File.file?(p)&&(kind==:test ? p.delete_prefix(File.join(root,stage)+'/').match?(pattern) : p.match?(pattern))};official=Dir[File.join(root,formal,'**/*')].select{|p|File.file?(p)&&!p.include?('/target/')&&!p.include?('/build/')&&(kind==:test ? p.delete_prefix(File.join(root,formal)+'/').match?(pattern) : p.match?(pattern))};inventory[name][kind]={staged:staged.length,formal:official.length}
  end
end
checks={no_unplaced_batch_artifact:inventory.values.all?{|dims|dims.values.all?{|v|v[:staged]==0}},all_dimensions_explicit:inventory.values.all?{|dims|dimensions.keys.all?{|k|dims.key?(k)}},formal_java_and_tests_present:inventory.values.all?{|d|d[:java][:formal]>0&&d[:test][:formal]>0},prior_file_ledgers_passed:%w[stage01-scjymj stage02-xcpdk stage05-disposition-ledger].all?{|n|File.exist?(File.join(root,"work/audit/#{n}.json"))}}
result={task:'STAGE06',passed:checks.values.all?,checks:checks,inventory:inventory};out=File.join(root,'work/audit/stage06-batch-conservation.json');FileUtils.mkdir_p(File.dirname(out));File.write(out,JSON.pretty_generate(result)+"\n");puts "STAGE06 #{result[:passed]?'passed':'failed'}: Java/config/resource/SQL/protocol/test dimensions conserved";exit(result[:passed]?0:1)
