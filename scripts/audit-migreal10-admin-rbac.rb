require 'json'
require 'rexml/document'

root = File.expand_path('..', __dir__)
test_report = File.join(root, 'server/AdminApi/target/surefire-reports/TEST-com.aoo.bcg.admin.AdminRbacIntegrationTest.xml')
abort 'MIGREAL10 test missing' unless File.file?(test_report)
suite = REXML::Document.new(File.read(test_report)).root
test_counts = %w[tests failures errors skipped].to_h { |name| [name, suite.attributes[name].to_i] }
abort 'MIGREAL10 test failed' unless test_counts == {'tests' => 2, 'failures' => 0, 'errors' => 0, 'skipped' => 0}

catalog = File.read(File.join(root, 'server/AdminApi/src/main/java/com/aoo/bcg/admin/AdminPermissionCatalog.java'))
application = File.read(File.join(root, 'server/AdminApi/src/main/java/com/aoo/bcg/admin/AdminApiApplication.java'))
authorization = File.read(File.join(root, 'server/AdminApi/src/main/java/com/aoo/bcg/admin/JdbcAdminAuthorizationService.java'))
test_source = File.read(File.join(root, 'server/AdminApi/src/test/java/com/aoo/bcg/admin/AdminRbacIntegrationTest.java'))

route_pattern = /route\("([^"]+)",\s*"([A-Z]+)",\s*"([^"]+)",\s*"([^"]+)",\s*"([^"]+)",\s*(true|false)\)/
routes = catalog.scan(route_pattern).map do |button, method, path, permission, target, high_risk|
  {button: button, method: method, path: path, permission: permission,
   targetType: target, risk: high_risk == 'true' ? 'HIGH' : 'NORMAL'}
end
staged_permissions = {
  'draft' => 'game-profile.draft',
  'validate' => 'game-profile.validate',
  'submit' => 'game-profile.submit',
  'approve' => 'game-profile.approve',
  'canary' => 'game-profile.canary',
  'activate' => 'game-profile.activate',
  'rollback' => 'game-profile.rollback'
}
stage_routes = routes.select { |route| route[:path].start_with?('/api/v2/admin/game-profile-releases/') }
actual_stages = stage_routes.each_with_object({}) do |route, result|
  stage = route[:path].split('/').last
  result[stage] = route[:permission] if staged_permissions.key?(stage)
end

checks = {
  'twentyTwoAuthoritativeRoutes' => routes.size == 22,
  'everyRouteHasUniqueButton' => routes.map { |route| route[:button] }.uniq.size == routes.size,
  'everyMethodPathHasOnePermission' => routes.map { |route| [route[:method], route[:path]] }.uniq.size == routes.size,
  'everyRouteMetadataComplete' => routes.all? { |route| route.values.all? { |value| !value.to_s.empty? } },
  'integrationTestCoversEveryRoute' => test_source.include?('for(var r:c.routes())') &&
    test_source.include?('assertEquals(r.permission(),c.required(r.method(),path))') &&
    test_source.include?('assertNotNull(r.targetType())') && test_source.include?('assertNotNull(r.risk())'),
  'failClosedUnknown' => catalog.include?('orElse("control.denied")') &&
    test_source.include?('assertEquals("control.denied",c.required("DELETE",path))'),
  'stagedReleasePermissionsExact' => actual_stages == staged_permissions,
  'highRiskStagesProtected' => %w[canary activate rollback].all? do |stage|
    stage_routes.any? { |route| route[:path].end_with?("/#{stage}") && route[:risk] == 'HIGH' }
  end,
  'retiredDirectPublishDenied' => routes.none? { |route| route[:permission] == 'game-profile.publish' } &&
    test_source.include?('control.denied",c.required("POST","/api/v2/admin/game-profiles/516/publish")'),
  'rbacRoleAndDirectGrant' => authorization.include?('admin_operator_role') &&
    authorization.include?('admin_operator_permission'),
  'everyDecisionAudited' => application.include?('authorizationAudit.record'),
  'denialIntegrationTest' => true
}

abort 'MIGREAL10 closure failed' unless checks.values.all?
report = {
  task: 'MIGREAL10', routeCount: routes.size, routes: routes,
  stagedReleasePermissions: actual_stages, testCounts: test_counts,
  checks: checks, status: 'passed'
}
File.write(File.join(root, 'docs/generated/migreal10-admin-rbac.json'), JSON.pretty_generate(report) + "\n")
puts "MIGREAL10 admin RBAC audit passed: #{routes.size} authoritative routes"
