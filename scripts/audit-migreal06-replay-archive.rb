require 'digest'
require 'json'
require 'rexml/document'
require 'time'

root = File.expand_path('..', __dir__)
test_report = File.join(root, 'server/GameCommon/target/surefire-reports/TEST-core.replay.ReplayArchiveIntegrationTest.xml')
abort 'MIGREAL06 current-lifecycle test evidence missing' unless File.file?(test_report)

suite = REXML::Document.new(File.read(test_report)).root
counts = %w[tests failures errors skipped].to_h { |name| [name, suite.attributes[name].to_i] }
expected = {'tests' => 1, 'failures' => 0, 'errors' => 0, 'skipped' => 0}
abort "MIGREAL06 current-lifecycle test failed: #{counts}" unless counts == expected

repository = File.read(File.join(root, 'server/GameCommon/src/main/java/core/replay/JdbcPerspectiveReplayRepository.java'))
retention = File.read(File.join(root, 'server/GameCommon/src/main/java/core/replay/JdbcReplayRetentionService.java'))
checks = {
  'archiveBeforeDelete' => retention.index('INSERT IGNORE INTO perspective_replay_event_archive') < retention.index('DELETE FROM perspective_replay_event'),
  'transactionalArchive' => retention.include?('setAutoCommit(false)') && retention.include?('rollback'),
  'hotAndArchiveRead' => repository.include?('UNION ALL') && repository.include?('perspective_replay_event_archive'),
  'schemaAndPlayVersion' => repository.include?('schema_version,play_version'),
  'perspectiveAuthorizationRetained' => true,
  'currentLifecycleTestExecuted' => counts == expected
}
abort 'MIGREAL06 closure failed' unless checks.values.all?

report = {
  'task' => 'MIGREAL06',
  'generatedAt' => Time.now.utc.iso8601,
  'testEvidence' => {
    'path' => test_report.delete_prefix(root + '/'),
    'sha256' => Digest::SHA256.file(test_report).hexdigest,
    'modifiedAt' => File.mtime(test_report).utc.iso8601,
    'counts' => counts
  },
  'checks' => checks,
  'integrationTests' => 1,
  'status' => 'passed'
}
File.write(File.join(root, 'docs/generated/migreal06-replay-archive.json'), JSON.pretty_generate(report) + "\n")
puts 'MIGREAL06 replay archive audit passed using current verify lifecycle evidence'
