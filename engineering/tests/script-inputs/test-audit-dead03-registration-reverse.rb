require 'json';require 'open3'
root=File.expand_path('../../..',__dir__);audit=File.join(root,'engineering/audit/script-inputs/audit-dead03-registration-reverse.rb')
stdout,stderr,status=Open3.capture3('ruby',audit)
abort "audit failed:\n#{stdout}#{stderr}" unless status.success?
report=JSON.parse(File.read(File.join(root,'build-only/audit/dead03-registration-reverse.json')))
abort 'expected passed audit' unless report['status']=='passed'
rows=report.fetch('implementations').to_h{|row|[row['className'],row]}
{'com.aoo.bcg.poker.nn.NnGameProvider'=>'quanguo2998','com.aoo.bcg.poker.cd299.Cd299GameProvider'=>'chengdu2999'}.each do |provider,code|
 row=rows.fetch(provider);proof=row.fetch('reverseProof');abort "missing reverse proof for #{provider}" unless row['registered']&&row['registration'].include?('service-loader-factory-provider-catalog-reverse-proof')&&proof['catalogCode']==code
end
load audit
matrix=Dead03::DatabaseRegistration.load(root)
abort 'unknown provider must remain unproved' unless Dead03RegistrationReverse.reverse_proof(root,'com.example.UnknownGameProvider',matrix).nil?
puts 'PASS two non-matching provider identities and unknown-provider fail-closed rejection'
