require 'json'; require 'fileutils'
root = File.expand_path('..', __dir__); pom = File.read(File.join(root, 'pom.xml'))
account_pom = File.read(File.join(root, 'server/Account/pom.xml'))
account = File.read(File.join(root, 'server/Account/src/main/java/com/aoo/bcg/account/AccountService.java'))
sessions = File.read(File.join(root, 'server/Account/src/main/java/com/aoo/bcg/account/JdbcAccountSessionService.java'))
assets = File.read(File.join(root, 'server/Account/src/main/java/com/aoo/bcg/account/AccountAssetView.java'))
modern_poms = Dir[File.join(root, 'server/{Account,Hall,Gateway,Bootstrap,GameCommon,GameSPI,Billing}/pom.xml')].map { |p| File.read(p) }.join("\n")
checks = {
  canonical_account_in_reactor: pom.include?('<module>server/Account</module>'),
  legacy_account_outside_reactor: !pom.include?('<module>server/LegacyAccountServer</module>') && !pom.include?('<module>server/LegacyAccountServer</module>'),
  identity_session_profile_owned: account.include?('profile') && %w[login authorize revokeSessions].all? { |x| sessions.include?(x) },
  device_bound_session_contract: sessions.include?('deviceId') && sessions.include?('device mismatch') && sessions.include?('aoo_account_device'),
  asset_mutation_owned_by_billing: account_pom.include?('<artifactId>game-billing</artifactId>') && assets.include?('Read-only') && !assets.match?(/debit|credit|transfer/),
  no_modern_legacy_dependency: !modern_poms.match?(/account_server|LegacyAccountServer/)
}
result={task:'REAL05',passed:checks.values.all?,checks:checks}; out=File.join(root,'work/audit/real05-account-ownership.json'); FileUtils.mkdir_p(File.dirname(out)); File.write(out,JSON.pretty_generate(result)+"\n"); puts "REAL05 #{result[:passed] ? 'passed':'failed'}: account/session/profile and asset ownership are unique"; exit(result[:passed] ? 0:1)
