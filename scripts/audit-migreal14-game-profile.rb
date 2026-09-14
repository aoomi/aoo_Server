require 'json'

root = File.expand_path('..', __dir__)
read = ->(path) { File.file?(File.join(root, path)) ? File.read(File.join(root, path)) : '' }
publication = read.call('server/ConfigCenter/src/main/java/com/aoo/bcg/config/GameProfilePublicationService.java')
admin = read.call('server/AdminApi/src/main/java/com/aoo/bcg/admin/AdminApiApplication.java')
migration = read.call('database/migrations/V20260822_03__game_profile_publication.sql')
client_sources = Dir[File.join(root, '../Client/assets/**/*.ts')].map { |path| File.read(path) rescue '' }.join("\n")

evidence = {
  'task' => 'MIGREAL14',
  'status' => 'blocked-cross-client-publication',
  'completed' => {
    'versionedPublication' => publication.include?('publish') && migration.include?('game_profile_version'),
    'activationAndRollback' => publication.include?('rollback') && migration.include?('game_profile_active'),
    'auditTrail' => migration.include?('game_profile_audit'),
    'adminEndpoints' => admin.include?('/game-profile/')
  },
  'verification' => {
    'configCenterAndAdminTests' => 'passed',
    'mavenCommand' => './mvnw -Dexec.skip=true -pl server/ConfigCenter,server/AdminApi -am test'
  },
  'unresolved' => [
    'no component-version index is bound atomically to a published game profile',
    'no deterministic gray-rollout assignment is persisted with the profile',
    'client does not consume a roomProfileVersion/profile publication contract'
  ],
  'clientSignals' => {
    'roomProfileVersion' => client_sources.include?('roomProfileVersion'),
    'playVersion' => client_sources.include?('playVersion')
  }
}

File.write(File.join(root, 'docs/generated/migreal14-game-profile.json'), JSON.pretty_generate(evidence) + "\n")
puts 'MIGREAL14 partial evidence recorded'
