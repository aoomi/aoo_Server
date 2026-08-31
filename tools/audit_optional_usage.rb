#!/usr/bin/env ruby
require 'json'
require 'open3'
root = File.expand_path('..', __dir__)
source_files = Dir.glob(File.join(root, 'server/**/*.java')).reject { |file| file.include?('/build/') || file.include?('/target/') }
unsafe = []
optional_fields = []
source_files.each do |file|
  source = File.read(file, encoding: 'UTF-8', invalid: :replace, undef: :replace)
  variables = source.scan(/Optional\s*<[^;=]+>\s+(\w+)/).flatten
  variables.each { |name| unsafe << file.sub(root + '/', '') if source.match?(/\b#{Regexp.escape(name)}\s*\.\s*get\s*\(/) }
  source.each_line.with_index(1) do |line, number|
    optional_fields << "#{file.sub(root + '/', '')}:#{number}" if line.match?(/^\s*(private|protected|public)\s+(?:final\s+)?Optional\s*<[^;]+>\s+\w+\s*(?:=[^;]*)?;/)
  end
end
unsafe.uniq!
java_home = File.expand_path('../.toolchains/jdk-26.0.2.1.jdk/Contents/Home', root)
env = {'JAVA_HOME' => java_home, 'PATH' => "#{java_home}/bin:#{ENV['PATH']}"}
stdout, stderr, status = Open3.capture3(env, './mvnw', '-pl', 'server/gameServer,server/XCPDK', '-am', '-DskipTests', 'package', '-q', chdir: root)
checks = {'no_optional_get' => unsafe.empty?, 'no_optional_entity_fields' => optional_fields.empty?, 'impacted_build_passed' => status.success?}
evidence = {'task' => 'TYPE03', 'passed' => checks.values.all?, 'checks' => checks,
            'unsafeOptionalGetFiles' => unsafe, 'optionalEntityFields' => optional_fields,
            'buildStdout' => stdout, 'buildStderr' => stderr}
audit = File.join(root, 'work/audit'); Dir.mkdir(File.join(root, 'work')) unless Dir.exist?(File.join(root, 'work')); Dir.mkdir(audit) unless Dir.exist?(audit)
File.write(File.join(audit, 'optional-usage.json'), JSON.pretty_generate(evidence))
puts JSON.generate(evidence); exit(evidence['passed'] ? 0 : 1)
