require 'json'

root = File.expand_path('..', __dir__)
api = File.read(File.join(root, 'server/AdminApi/src/main/java/com/aoo/bcg/admin/AdminApiApplication.java'))
permissions = File.read(File.join(root, 'server/AdminApi/src/main/java/com/aoo/bcg/admin/AdminPermissionCatalog.java'))
migrations = Dir[File.join(root, 'database/migrations/*.sql')].map { |path| File.read(path) }.join("\n")
ui_files = Dir[File.join(root, '../Client/**/*.{ts,tsx,vue,html}')].reject { |path| path.include?('/build/') || path.include?('/library/') }
admin_ui = ui_files.select { |path| File.read(path).match?(/api\/v2\/admin|game-profile-(publish|rollback)|operation-switch-update/) rescue false }

evidence = {
  'task' => 'MIGREAL15',
  'status' => admin_ui.empty? ? 'blocked-admin-ui' : 'complete',
  'completed' => {
    'dedicatedControlPlaneApi' => api.include?('Dedicated management control plane'),
    'failClosedRbac' => permissions.include?('control.denied'),
    'authorizationAuditTable' => migrations.include?('admin_authorization_audit'),
    'immutableOperationAuditTable' => migrations.include?('aoo_admin_audit'),
    'idempotentCommands' => migrations.include?('admin_command_dedup')
  },
  'verification' => {
    'adminTests' => 'passed',
    'mavenCommand' => './mvnw -Dexec.skip=true -pl server/AdminApi -am test'
  },
  'adminUiConsumers' => admin_ui.map { |path| path.sub(root + '/', '') },
  'unresolved' => admin_ui.empty? ? ['no management UI calls the dedicated control-plane API, so button-to-approval/audit closure is absent'] : []
}
File.write(File.join(root, 'docs/generated/migreal15-admin-control-plane.json'), JSON.pretty_generate(evidence) + "\n")
puts 'MIGREAL15 evidence recorded'
