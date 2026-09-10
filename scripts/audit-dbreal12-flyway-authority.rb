#!/usr/bin/env ruby
# frozen_string_literal: true
require 'json'
require 'pathname'

root = Pathname.new(__dir__).parent
parent = (root / 'pom.xml').read
module_pom = (root / 'server/gameServer/pom.xml').read
startup = (root / 'server/gameServer/src/core/server/GameServer.java').read
migrator = (root / 'server/gameServer/src/core/server/FlywaySchemaMigrator.java').read
runner = (root / 'tools/apply_migrations.sh').read
checks = {
  'official_current_flyway_version_pinned' => parent.include?('<flyway.version>13.3.0</flyway.version>'),
  'mysql_database_module_present' => module_pom.include?('<artifactId>flyway-mysql</artifactId>'),
  'migrations_packaged_as_classpath_resources' => module_pom.include?('<targetPath>db/migration</targetPath>'),
  'application_startup_migrates_before_runtime' => startup.index('FlywaySchemaMigrator.migrate') && startup.index('GameServerUnifiedRuntime.install') && startup.index('FlywaySchemaMigrator.migrate') < startup.index('GameServerUnifiedRuntime.install'),
  'legacy_schema_mutators_disconnected' => !startup.include?('checkAndUpdateVersion') && !startup.include?('runAutoVersionUpdate') && !startup.include?('updateDB('),
  'baseline_and_clean_fail_closed' => migrator.include?('baselineOnMigrate(false)') && migrator.include?('cleanDisabled(true)'),
  'corrective_out_of_order_migrations_enabled' => migrator.include?('outOfOrder(true)') && runner.include?('FLYWAY_OUT_OF_ORDER="true"'),
  'single_history_authority' => runner.include?('flyway_schema_history') && !runner.include?('aoo_schema_migration'),
  'release_and_local_use_same_engine' => runner.include?('flyway-maven-plugin') && migrator.include?('org.flywaydb.core.Flyway')
}
report = {
  'task' => 'DBREAL12', 'passed' => checks.values.all?,
  'engine' => 'Flyway Open Source 13.3.0',
  'officialVersionEvidence' => 'Redgate Flyway Java API documentation, updated 2026-08-13',
  'schemaHistoryTable' => 'flyway_schema_history',
  'checks' => checks
}
(root / 'work/audit').mkpath
(root / 'work/audit/dbreal12-flyway-authority.json').write(JSON.pretty_generate(report) + "\n")
abort JSON.generate(report) unless report['passed']
puts 'DBREAL12 passed: Flyway is the sole release and startup migration authority'
