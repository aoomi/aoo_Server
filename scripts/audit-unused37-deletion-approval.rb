#!/usr/bin/env ruby
require 'json';require 'pathname';require 'digest';require 'time'
root=Pathname(__dir__).join('..').expand_path
manifest=JSON.parse(root.join('docs/legacy-2.22-artifact-manifest.json').read);approval_doc=JSON.parse(root.join('docs/legacy-2.22-deletion-approvals.json').read)
required=%w[path sha256 usageScan replacementMapping artifactComparison historicalDataTest ownerConclusion approvedBy approvedAt]
current=Dir[root.join('reference/legacy-2.22/**/*.jar').to_s].to_h{|path|[Pathname(path).relative_path_from(root).to_s,Digest::SHA256.file(path).hexdigest]}
baseline=manifest['artifacts'].to_h{|item|[item['path'],item['sha256']]};approvals=approval_doc['approvals']||[]
valid_approval=lambda do |path,sha|
  approvals.any? do |approval|
    required.all?{|field|approval[field].is_a?(String)&&!approval[field].strip.empty?} && approval['path']==path && approval['sha256']==sha && Time.iso8601(approval['approvedAt'])
  rescue ArgumentError
    false
  end
end
removed=baseline.keys-current.keys;modified=baseline.keys.select{|path|current.key?(path)&&current[path]!=baseline[path]};unapproved=(removed+modified).reject{|path|valid_approval.call(path,baseline[path])}
checks={requiredFieldsFixed:approval_doc['requiredFields']==required,manifestHasProtectedArtifacts:baseline.length>=100,noUnapprovedDeletionOrMutation:unapproved.empty?,approvalPathsUnique:approvals.map{|a|a['path']}.uniq.length==approvals.length,prerequisiteEvidencePresent:%w[unused26-historical-formats.json unused27-artifact-comparison.json unused33-historical-read-protection.json unused34-equivalent-replacement.json].all?{|f|root.join('work/audit',f).exist?}}
errors=checks.reject{|_,v|v}.keys;report={task:'UNUSED37',status:errors.empty? ? 'passed':'failed',checks:checks,protectedArtifacts:baseline.length,removed:removed,modified:modified,unapproved:unapproved,approvalCount:approvals.length,errors:errors};root.join('work/audit/unused37-deletion-approval.json').write(JSON.pretty_generate(report)+"\n");abort("UNUSED37 failed: #{errors.join(', ')} #{unapproved.join(', ')}")unless errors.empty?;puts "UNUSED37 passed: #{baseline.length} protected artifacts; every deletion requires evidence and owner approval"
