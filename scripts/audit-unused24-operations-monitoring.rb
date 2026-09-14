#!/usr/bin/env ruby
require 'json'
require 'pathname'
require 'rexml/document'

root = Pathname(__dir__).join('..').expand_path
ignored = %r{/(?:\.git|target|work|node_modules|temp|library|build)/}
pom_files = Dir[root.join('**/pom.xml').to_s].reject { |p| p.match?(ignored) }
monitor_pattern = /(?:opentelemetry|prometheus|micrometer|actuator|zipkin|jaeger|skywalking|newrelic|datadog|jolokia|sentry|grafana|logstash|fluentd|filebeat)/i
dependencies = pom_files.flat_map do |path|
  doc = REXML::Document.new(File.read(path))
  REXML::XPath.match(doc, '//*[local-name()="dependency"]').map do |node|
    group = REXML::XPath.first(node, './*[local-name()="groupId"]')&.text.to_s
    artifact = REXML::XPath.first(node, './*[local-name()="artifactId"]')&.text.to_s
    { pom: path.delete_prefix(root.to_s + '/'), coordinate: "#{group}:#{artifact}" } if "#{group}:#{artifact}".match?(monitor_pattern)
  end.compact
end

config_files = Dir[root.join('{deploy,config,server}/**/*.{yml,yaml,json,properties,conf,xml}').to_s].reject { |p| p.match?(ignored) }
sidecars = config_files.select do |path|
  text = File.binread(path).force_encoding(Encoding::UTF_8).scrub
  File.basename(path).match?(monitor_pattern) || text.match?(/\b(?:sidecar|javaagent|otel\.exporter|prometheus\.io\/scrape)\b/i)
end
startup_files = Dir[root.join('{tools,deploy,scripts}/**/*.{sh,bash,zsh}').to_s].reject { |p| p.match?(ignored) }
agents = startup_files.flat_map do |path|
  File.readlines(path).each_with_index.map { |line, index| { file: path.delete_prefix(root.to_s + '/'), line: index + 1, text: line.strip } if line.match?(/-(?:javaagent|agentlib|agentpath)\b/) }.compact
end

monitors = %w[
  server/GameCommon/src/main/java/com/aoo/bcg/common/metrics/DynamicMetricRegistry.java
  server/GameCommon/src/main/java/com/aoo/bcg/common/concurrency/ConcurrencyStallMonitor.java
  server/GameCommon/src/main/java/com/aoo/bcg/common/time/SchedulerDriftMonitor.java
  server/GameSPI/src/main/java/com/aoo/bcg/gamespi/time/ClockRollbackMonitor.java
]
missing_monitors = monitors.reject { |p| root.join(p).exist? }
errors = []
errors << "monitoring/exporter dependencies remain: #{dependencies.map { |x| x[:coordinate] }.join(', ')}" unless dependencies.empty?
approved_configs = sidecars.select do |path|
  rel = path.delete_prefix(root.to_s + '/')
  text = File.read(path)
  rel.start_with?('deploy/ops/') &&
    (text.include?('aoo.io/production-observability') ||
     (text.include?('name: metrics') && text.include?('resources:') && text.include?('allowPrivilegeEscalation: false')))
end
unowned_configs = sidecars - approved_configs
errors << "monitoring sidecar/exporter configs lack production ownership and resource/security bounds: #{unowned_configs.join(', ')}" unless unowned_configs.empty?
errors << "monitoring agents remain: #{agents.map { |x| x[:file] }.join(', ')}" unless agents.empty?
errors << "required lightweight monitor missing: #{missing_monitors.join(', ')}" unless missing_monitors.empty?
report = {
  task: 'UNUSED24', status: errors.empty? ? 'passed' : 'failed', monitoringDependencies: dependencies,
  exporterOrSidecarConfigs: sidecars.map { |p| p.delete_prefix(root.to_s + '/') }, javaAgents: agents,
  approvedOperationsConfigs: approved_configs.map { |p| p.delete_prefix(root.to_s + '/') },
  retainedInProcessMonitors: monitors, policy: 'Retain bounded JDK/in-process diagnostics; external exporter, agent or sidecar requires a named consumer, alert owner and unique collection responsibility.',
  errors: errors
}
out = root.join('work/audit/unused24-operations-monitoring.json')
out.dirname.mkpath
out.write(JSON.pretty_generate(report) + "\n")
abort(errors.join("\n")) unless errors.empty?
puts "UNUSED24 passed: #{approved_configs.length} owned operations config(s); no dependency or agent; bounded in-process monitors remain"
