#!/usr/bin/env ruby
require 'json'
require 'pathname'

root = Pathname(__dir__).join('..').expand_path
runtime = root.join('work/audit/unused27-runtime')
class_log = runtime.join('class-load.log').read
catalog = runtime.join('catalog.txt').read
run_log = runtime.join('runtime.txt').read
failure_patterns = [
  /Exception in thread/, /Caused by: .*ClassNotFoundException/,
  /Caused by: .*NoClassDefFoundError/, /ServiceConfigurationError:/,
  /Could not initialize class/, /BootstrapMethodError:/
]
combined = catalog + "\n" + run_log
failures = failure_patterns.flat_map { |pattern| combined.scan(pattern).map(&:to_s) }
checks = {
  classLoadLogPresent: class_log.each_line.count > 500,
  applicationClassesLoaded: class_log.include?('com.aoo.bcg.bootstrap.BootstrapAPP'),
  spiLoaded: catalog.each_line.count { |line| line.start_with?('GameDescriptor') } == 533,
  noRuntimeClassResolutionFailure: failures.empty?,
  fullChainGatePassed: JSON.parse(root.join('work/audit/unused28-full-chain.json').read)['status'] == 'passed'
}
errors = checks.reject { |_key, value| value }.keys
report = { task: 'UNUSED29', status: errors.empty? ? 'passed' : 'failed', checks: checks,
  loadedClassLogEntries: class_log.each_line.count, gameProviderCount: catalog.each_line.count { |line| line.start_with?('GameDescriptor') },
  note: 'Names of JVM exception classes in -Xlog:class+load are normal class definitions, not thrown failures; only exception/causal runtime records are rejected.', failures: failures, errors: errors }
out = root.join('work/audit/unused29-runtime-classloading.json'); out.write(JSON.pretty_generate(report) + "\n")
abort("UNUSED29 failed: #{errors.join(', ')}") unless errors.empty?
puts "UNUSED29 passed: #{report[:loadedClassLogEntries]} class-load records and 533 providers without runtime class-resolution failure"
