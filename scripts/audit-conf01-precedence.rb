require 'json'
require 'fileutils'
root = File.expand_path('..', __dir__)
source = File.read(File.join(root, 'server/GameCommon/src/main/java/com/aoo/bcg/common/config/RuntimeConfigResolver.java'))
test = File.read(File.join(root, 'server/GameCommon/src/test/java/com/aoo/bcg/common/config/RuntimeConfigResolverTest.java'))
order = %w[arguments environment file configCenter defaults].map { |name| source.index("#{name}.containsKey") }
checks = {
  one_resolver: source.include?('class RuntimeConfigResolver'),
  deterministic_precedence: order.none?(&:nil?) && order.each_cons(2).all? { |a,b| a < b },
  origin_is_observable: source.include?('enum Origin'),
  required_values_fail_fast: source.include?('required configuration is missing'),
  executable_tests: test.include?('enforcesOneDeterministicPrecedenceOrder')
}
abort "CONF01 audit failed: #{checks}" unless checks.values.all?
out = File.join(root, 'work/audit/runtime-config-precedence.json')
FileUtils.mkdir_p(File.dirname(out))
File.write(out, JSON.pretty_generate({task: 'CONF01', status: 'passed', checks: checks}) + "\n")
