#!/usr/bin/env ruby
require 'json'
require 'open3'
root = File.expand_path('..', __dir__)
files = Dir.glob(File.join(root, 'server/**/src/**/*.java')).reject { |file| file.include?('/build/') || file.include?('/target/') }
violations = []
files.each do |file|
  source = File.read(file, encoding: 'UTF-8', invalid: :replace, undef: :replace)
  position = 0
  while (match = /record\s+(\w+)\s*\(/m.match(source, position))
    name = match[1]
    cursor = match.end(0) - 1
    depth = 0
    finish = cursor
    loop do
      char = source[finish]
      depth += 1 if char == '('
      depth -= 1 if char == ')'
      finish += 1
      break if depth == 0 || finish >= source.size
    end
    header = source[(cursor + 1)...(finish - 1)]
    components = header.scan(/(?:List|Map|Set|Collection)\s*<[^;,)]+(?:>[^;,)]+)*>\s+(\w+)/).flatten
    unless components.empty?
      body = source[finish, [source.size - finish, 2400].min]
      components.each do |component|
        copied = body.match?(/#{Regexp.escape(component)}\s*=\s*(?:List|Map|Set)\.copyOf/) ||
                 body.match?(/#{Regexp.escape(component)}\s*=.*Collectors\.toUnmodifiableMap/m)
        violations << "#{file.sub(root + '/', '')}:#{name}.#{component}" unless copied
      end
    end
    position = finish
  end
end
java_home = File.expand_path('../.toolchains/jdk-26.0.2.1.jdk/Contents/Home', root)
env = {'JAVA_HOME' => java_home, 'PATH' => "#{java_home}/bin:#{ENV['PATH']}"}
stdout, stderr, status = Open3.capture3(env, './mvnw', '-pl', 'server/ZYPK,server/gameServer', '-am', '-DskipTests', 'package', '-q', chdir: root)
checks = {'all_record_collections_defensively_copied' => violations.empty?, 'impacted_build_passed' => status.success?}
evidence = {'task' => 'TYPE04', 'passed' => checks.values.all?, 'checks' => checks,
            'violations' => violations, 'buildStdout' => stdout, 'buildStderr' => stderr}
audit = File.join(root, 'work/audit'); Dir.mkdir(File.join(root, 'work')) unless Dir.exist?(File.join(root, 'work')); Dir.mkdir(audit) unless Dir.exist?(audit)
File.write(File.join(audit, 'immutable-collection-boundaries.json'), JSON.pretty_generate(evidence))
puts JSON.generate(evidence); exit(evidence['passed'] ? 0 : 1)
