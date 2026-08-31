#!/usr/bin/env ruby
# frozen_string_literal: true

require 'json'
require 'open3'
require 'time'

root = File.expand_path('..', __dir__)
request = File.read(File.join(root, 'server/LegacyCommon/src/com/ddm/server/websocket/handler/requset/RequestDispatcher.java'))
blocklist = File.read(File.join(root, 'server/GameSPI/src/main/java/com/aoo/bcg/gamespi/api/DeprecatedEntryPointBlocklist.java'))
rollback = File.read(File.join(root, 'tools/protocol-v1-record-rollback-drill.sh'))
client_files = Dir.glob(File.expand_path('../Client/assets/**/*.ts', root))
client_refs = client_files.select { |path| File.read(path, encoding: 'UTF-8', invalid: :replace, undef: :replace).match?(/protocol\.v1|v1-compatibility/) }
errors = []
errors << 'legacy dispatcher remains reachable' unless request.include?('legacy protocol is disabled') && !request.include?('transitional.handleMessage')
errors << 'retired HTTP/message/listener/config deny-list is incomplete' unless %w[/api/v1/ legacy. 904 9998 legacy.protocol.enabled].all? { |token| blocklist.include?(token) }
errors << 'rollback recorder still mutates runtime state' if rollback.match?(/redis-cli|SET|CONFIRM=YES/)
errors << 'rollback tombstone is not fail-closed' unless rollback.include?('exit 2')
errors << "client V1 compatibility references remain: #{client_refs.size}" unless client_refs.empty?
command = ['./mvnw', '-Dexec.skip=true', '-pl', 'server/GameSPI', '-am', '-Dtest=DeprecatedEntryPointBlocklistTest', '-Dsurefire.failIfNoSpecifiedTests=false', 'test']
stdout, stderr, status = Open3.capture3(*command, chdir: root)
errors << 'compiled V1 deny-list test failed' unless status.success?
report = {
  schemaVersion: 1, generatedAt: Time.now.utc.iso8601, task: 'TOOL11', passed: errors.empty?,
  productionRollbackAvailable: false, clientCompatibilityReferences: client_refs.map { |path| path.delete_prefix(File.expand_path('..', root) + '/') },
  command: command.join(' '), exitCode: status.exitstatus, errors: errors,
  outputTail: (stdout + stderr).lines.last(30).join
}
File.write(File.join(root, 'docs/generated/tool11-protocol-v1-retirement.json'), JSON.pretty_generate(report) + "\n")
puts "protocol-v1-retirement: #{errors.empty? ? 'passed' : 'failed'}"
exit(errors.empty? ? 0 : 1)
