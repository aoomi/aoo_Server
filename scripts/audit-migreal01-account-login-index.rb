require 'json'
root=File.expand_path('..',__dir__); path=File.join(root,'docs/generated/migreal01-account-login-index.json')
abort 'MIGREAL01 evidence missing' unless File.file?(path); e=JSON.parse(File.read(path))
abort 'MIGREAL01 capacity failed' unless e['rows']>=10_000_000
abort 'MIGREAL01 uniqueness failed' unless e['rows']==e['distinctAccountIds']
abort 'MIGREAL01 identity semantics missing' unless e['accountIdentityType']=='numeric-case-neutral'
abort 'MIGREAL01 plan failed' unless e['index']=='uk_db_player_account'
abort 'MIGREAL01 latency failed' unless e['p95Ms']<=e['p95BudgetMs']&&e['p99Ms']<=e['p99BudgetMs']
abort 'MIGREAL01 migration missing' unless Dir[File.join(root,'database/migrations/*account_login_unique_index.sql')].one?
puts 'MIGREAL01 account login index audit passed'
