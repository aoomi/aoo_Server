require 'json'

root = File.expand_path('..', __dir__)
tables = %w[aoo_session aoo_business_idempotency aoo_ledger aoo_settlement aoo_outbox aoo_game_profile_version aoo_appeal aoo_admin_audit aoo_data_lifecycle_policy]
sources = Dir[File.join(root, 'server/**/src/main/**/*.java')]
usage = tables.to_h do |table|
  matches = sources.select { |path| File.read(path).include?(table) rescue false }
  [table, matches.map { |path| path.sub(root + '/', '') }]
end
missing = usage.select { |_table, files| files.empty? }.keys
evidence = {
  'task' => 'MIGREAL16',
  'status' => missing.empty? ? 'complete' : 'blocked-unused-production-tables',
  'tableRuntimeUsage' => usage,
  'completedClosures' => %w[aoo_business_idempotency aoo_ledger aoo_settlement aoo_outbox].select { |table| !usage.fetch(table).empty? },
  'unresolvedTables' => missing,
  'unresolved' => missing.empty? ? [] : ['login, appeal/admin and lifecycle tables are not all connected to production repositories', 'no automated login-hall-club-room-game-billing end-to-end test proves the complete business transaction']
}
File.write(File.join(root, 'docs/generated/migreal16-business-closure.json'), JSON.pretty_generate(evidence) + "\n")
puts 'MIGREAL16 evidence recorded'
