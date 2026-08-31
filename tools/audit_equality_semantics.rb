#!/usr/bin/env ruby
require 'json'
require 'open3'
root = File.expand_path('..', __dir__)
files = Dir.glob(File.join(root, 'server/**/src/**/*.java')).reject { |file| file.include?('/build/') || file.include?('/target/') || file.include?('/test/') }
index = {}
files.each do |file|
  source = File.read(file, encoding: 'UTF-8', invalid: :replace, undef: :replace)
  source.scan(/\b(record|enum|class)\s+(\w+)/) { |kind, name| index[name] = [kind, file, source] }
end
key_types = []
files.each do |file|
  source = File.read(file, encoding: 'UTF-8', invalid: :replace, undef: :replace)
  source.scan(/(?:Map|ConcurrentHashMap|Set|HashSet|LinkedHashSet)\s*<\s*((?:[A-Z]\w*\.)*[A-Z]\w*)/) do |match|
    key_types << match[0].split('.').last
  end
end
ignored = %w[String Integer Long Short Byte Character Boolean Object Class Map Tuple TypedTuple T K E M S Void Trigger TileRank ZSetOperations]
violations = key_types.uniq.reject { |name| ignored.include?(name) }.map do |name|
  declaration = index[name]
  next "#{name}: declaration missing" unless declaration
  kind, file, source = declaration
  next if kind == 'record' || kind == 'enum' || source.include?('@IdentitySemantics')
  generated = source.match?(/@Data|@Value/)
  implemented = source.include?('equals(Object') && source.include?('hashCode()')
  "#{name}: #{file.sub(root + '/', '')}" unless generated || implemented
end.compact
java_home = File.expand_path('../.toolchains/jdk-26.0.2.1.jdk/Contents/Home', root)
env = {'JAVA_HOME' => java_home, 'PATH' => "#{java_home}/bin:#{ENV['PATH']}"}
stdout, stderr, status = Open3.capture3(env, './mvnw', '-pl', 'server/gameServer,server/NJPDK,server/GameCommon', '-am', 'test', '-q', chdir: root)
checks = {'all_collection_keys_have_declared_semantics' => violations.empty?, 'value_contract_tests_passed' => status.success?}
evidence = {'task' => 'TYPE06', 'passed' => checks.values.all?, 'checks' => checks,
            'violations' => violations, 'buildStdout' => stdout, 'buildStderr' => stderr}
audit = File.join(root, 'work/audit'); Dir.mkdir(File.join(root, 'work')) unless Dir.exist?(File.join(root, 'work')); Dir.mkdir(audit) unless Dir.exist?(audit)
File.write(File.join(audit, 'equality-semantics.json'), JSON.pretty_generate(evidence))
puts JSON.generate(evidence); exit(evidence['passed'] ? 0 : 1)
