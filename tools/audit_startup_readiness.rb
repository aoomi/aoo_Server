#!/usr/bin/env ruby
require 'json'
require 'open3'
root = File.expand_path('..', __dir__)
gate = File.join(root, 'server/GameCommon/src/main/java/com/aoo/bcg/common/readiness/ServiceReadinessGate.java')
bootstrap = File.read(File.join(root, 'server/Bootstrap/src/main/java/com/aoo/bcg/bootstrap/BootstrapAPP.java'))
assembly = File.read(File.join(root, 'server/gameServer/src/core/server/ProductionPersistenceAssembly.java'))
java_home = File.expand_path('../.toolchains/jdk-26.0.2.1.jdk/Contents/Home', root)
env = {'JAVA_HOME' => java_home, 'PATH' => "#{java_home}/bin:#{ENV['PATH']}"}
stdout, stderr, status = Open3.capture3(env, './mvnw', '-pl', 'server/Bootstrap,server/gameServer', '-am', 'test', '-q', chdir: root)
checks = {
  'closed_by_default_gate' => File.exist?(gate) && File.read(gate).include?('State.NOT_READY'),
  'traffic_rejection' => File.exist?(gate) && File.read(gate).include?('requireAcceptingTraffic'),
  'catalog_is_gated' => bootstrap.include?('gameCatalogIndex') && bootstrap.include?('verifyAndOpen'),
  'database_is_gated' => assembly.include?('databaseConnectionAndSchema') && assembly.include?('verifyAndOpen'),
  'tests_passed' => status.success?
}
result = {'task' => 'JAVA12', 'passed' => checks.values.all?, 'checks' => checks,
          'buildStdout' => stdout, 'buildStderr' => stderr}
path = File.join(root, 'work/audit/startup-readiness.json')
FileUtils.mkdir_p(File.dirname(path)) if defined?(FileUtils)
Dir.mkdir(File.join(root, 'work')) unless Dir.exist?(File.join(root, 'work'))
Dir.mkdir(File.join(root, 'work/audit')) unless Dir.exist?(File.join(root, 'work/audit'))
File.write(path, JSON.pretty_generate(result))
puts JSON.generate(result)
exit(result['passed'] ? 0 : 1)
