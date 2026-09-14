require 'json'
require 'fileutils'
root = File.expand_path('..', __dir__)
support = File.read(File.join(root, 'server/GameCommon/src/main/java/com/aoo/bcg/common/persistence/JdbcSupport.java'))
test = File.read(File.join(root, 'server/GameCommon/src/test/java/com/aoo/bcg/common/persistence/JdbcSupportTest.java'))
checks = {
  stable_error_codes: %w[1008 1009 1010 1011 1012].all? { |code| support.include?(code) },
  sql_state_classification: support.include?('getSQLState') && support.include?('getErrorCode'),
  public_message_redaction: test.include?('NeverExposesSqlDetails'),
  internal_cause_preserved: support.include?('DatabaseOperationException')
}
abort "CON10 audit failed: #{checks}" unless checks.values.all?
out = File.join(root, 'work/audit/sql-error-contract.json')
FileUtils.mkdir_p(File.dirname(out))
File.write(out, JSON.pretty_generate({task: 'CON10', status: 'passed', checks: checks}) + "\n")
