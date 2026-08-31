#!/usr/bin/env ruby
require 'json'
require 'open3'
root = File.expand_path('..', __dir__)
targets = %w[
 server/GameSPI/src/main/java/com/aoo/bcg/gamespi/CommandPayload.java
 server/GameCommon/src/main/java/com/aoo/bcg/common/recovery/RoomSnapshot.java
 server/GameCommon/src/main/java/com/aoo/bcg/common/replay/ReplayFrame.java
 server/GameCommon/src/main/java/com/aoo/bcg/common/perspective/PerspectiveMessage.java
 server/GameCommon/src/main/java/com/aoo/bcg/common/event/InMemoryRoomEventJournal.java
]
uses = targets.to_h { |path| [path, File.read(File.join(root, path)).include?('ImmutableValue.freeze')] }
java_home = File.expand_path('../.toolchains/jdk-26.0.2.1.jdk/Contents/Home', root)
env = {'JAVA_HOME' => java_home, 'PATH' => "#{java_home}/bin:#{ENV['PATH']}"}
stdout, stderr, status = Open3.capture3(env, './mvnw', '-pl', 'server/ZYPK,server/GameCommon', '-am', 'test', '-q', chdir: root)
checks = {'all_state_boundaries_deep_freeze' => uses.values.all?, 'deep_mutation_tests_passed' => status.success?}
evidence = {'task' => 'TYPE05', 'passed' => checks.values.all?, 'checks' => checks,
            'boundaryCoverage' => uses, 'buildStdout' => stdout, 'buildStderr' => stderr}
audit = File.join(root, 'work/audit'); Dir.mkdir(File.join(root, 'work')) unless Dir.exist?(File.join(root, 'work')); Dir.mkdir(audit) unless Dir.exist?(audit)
File.write(File.join(audit, 'deep-snapshot-isolation.json'), JSON.pretty_generate(evidence))
puts JSON.generate(evidence); exit(evidence['passed'] ? 0 : 1)
