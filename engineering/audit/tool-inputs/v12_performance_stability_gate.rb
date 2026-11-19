#!/usr/bin/env ruby
# frozen_string_literal: true

require 'json'
require 'fileutils'

module AooV12PerformanceStabilityGate
  module_function

  def constructor_arguments(source, type)
    marker = /new\s+#{Regexp.escape(type)}\s*\(/
    arguments = []
    source.scan(marker) do
      opening = Regexp.last_match.end(0) - 1
      depth = 0
      quote = nil
      escaped = false
      closing = nil
      source.each_char.with_index do |character, offset|
        next if offset < opening
        if quote
          if escaped
            escaped = false
          elsif character == '\\'
            escaped = true
          elsif character == quote
            quote = nil
          end
        elsif character == '"' || character == "'"
          quote = character
        elsif character == '('
          depth += 1
        elsif character == ')'
          depth -= 1
          if depth.zero?
            closing = offset
            break
          end
        end
      end
      arguments << source[(opening + 1)...closing] if closing
    end
    arguments
  end

  def bounded_gateway_queue?(source)
    executable = source.gsub(%r{/\*.*?\*/}m, ' ').gsub(%r{//.*$}, ' ')
    array_capacity = executable.match(
      /new\s+ArrayBlockingQueue(?:\s*<[^>]*>)?\s*\(\s*([+-]?\d+)/
    )
    return true if array_capacity && array_capacity[1].to_i.positive?

    constructor_arguments(executable, 'DefaultEventExecutorGroup').any? do |arguments|
      capacity = arguments.match(
        /,\s*([+-]?\d+)\s*,\s*RejectedExecutionHandlers\.reject\s*\(\s*\)\s*\z/m
      )
      !capacity.nil? && capacity[1].to_i.positive?
    end
  end
end

if __FILE__ == $PROGRAM_NAME
root = File.expand_path('../../..', __dir__)
client = File.expand_path('../Client', root)
admin = File.expand_path('../Admin', root)

production = lambda do |base, extensions|
  extensions.flat_map { |extension| Dir.glob(File.join(base, "**/*.#{extension}")) }
    .reject { |path| path.match?(%r{/(?:test|tests|target|build|node_modules|library|temp)/}i) }
end
java_files = production.call(File.join(root, 'modules'), %w[java])
client_files = production.call(File.join(client, 'assets'), %w[ts])
admin_files = production.call(File.join(admin, 'src'), %w[ts vue])
all_files = java_files + client_files + admin_files

body = lambda do |path|
  File.read(path, encoding: 'UTF-8', invalid: :replace, undef: :replace)
      .gsub(%r{/\*.*?\*/}m, ' ').gsub(%r{//.*$}, ' ')
end
relative = lambda { |path| path.sub(%r{^#{Regexp.escape(File.dirname(root))}/?}, '') }

rules = {
  'unbounded_loops' => /\bwhile\s*\(\s*true\s*\)|\bfor\s*\(\s*;\s*;\s*\)/,
  'unbounded_jdk_queues' => /new\s+(?:LinkedBlockingQueue|LinkedTransferQueue|ConcurrentLinkedQueue|ConcurrentLinkedDeque)(?:\s*<[^;()]*>)?\s*\(\s*\)/,
  'cached_thread_pools' => /Executors\.newCachedThreadPool\s*\(/
}
findings = rules.transform_values do |pattern|
  all_files.map { |path| relative.call(path) if body.call(path).match?(pattern) }.compact
end

scheduled = java_files.select { |path| body.call(path).match?(/newSingleThreadScheduledExecutor|newScheduledThreadPool/) }
findings['scheduler_without_shutdown'] = scheduled.map do |path|
  source = body.call(path)
  relative.call(path) unless source.match?(/shutdown(?:Now)?\s*\(|::shutdownNow|awaitTermination\s*\(/)
end.compact

intervals = (client_files + admin_files).select { |path| body.call(path).include?('setInterval(') }
findings['interval_without_clear'] = intervals.map do |path|
  relative.call(path) unless body.call(path).include?('clearInterval(')
end.compact

findings['global_listener_without_remove'] = admin_files.map do |path|
  source = body.call(path)
  added = source.scan(/(?:window|document(?:\.body)?)\.addEventListener\(\s*['"]([^'"]+)/).flatten.uniq
  removed = source.scan(/(?:window|document(?:\.body)?)\.removeEventListener\(\s*['"]([^'"]+)/).flatten.uniq
  missing = added - removed
  "#{relative.call(path)}: #{missing.join(',')}" unless missing.empty?
end.compact

event_handlers = java_files.select { |path| body.call(path).match?(/ChannelInboundHandler|SimpleChannelInboundHandler|ChannelHandlerContext|EventLoop/) }
blocking = /(?:getConnection|prepareStatement|executeQuery|executeUpdate)\s*\(|Files\.(?:read|write|lines|newInputStream|newOutputStream)|Thread\.sleep\s*\(|\.await\s*\(|\.join\s*\(\s*\)/
findings['event_loop_blocking'] = event_handlers.map { |path| relative.call(path) if body.call(path).match?(blocking) }.compact

retention_sources = java_files.map { |path| body.call(path) }.join("\n")
invariants = {
  'terminal_event_retention' => retention_sources.include?('purgeTerminalBefore'),
  'bounded_outbox_batch' => retention_sources.include?('batchSize<1||batchSize>500'),
  'bounded_media_cleanup' => retention_sources.include?('cleanupExpired(500)'),
  'bounded_gateway_queue' => AooV12PerformanceStabilityGate.bounded_gateway_queue?(
    body.call(File.join(root, 'modules/infrastructure/Gateway/src/main/java/com/aoo/bcg/gateway/GatewayApplication.java'))
  ),
  'log_rotation_policy' => !Dir.glob(File.join(root, '**/logback*.xml')).empty? && Dir.glob(File.join(root, '**/logback*.xml')).any? { |path| body.call(path).match?(/maxHistory|totalSizeCap/) }
}

failures = findings.values.flatten + invariants.map { |name, passed| name unless passed }.compact
result = {
  'schemaVersion' => 1,
  'gate' => 'V12 performance and stability static gate',
  'passed' => failures.empty?,
  'scannedFiles' => all_files.length,
  'findings' => findings,
  'invariants' => invariants,
  'failureCount' => failures.length
}
output = File.join(root, 'build-only/audit/v12-performance-stability-static.json')
FileUtils.mkdir_p(File.dirname(output))
File.write(output, JSON.pretty_generate(result) + "\n")
puts JSON.generate('passed' => result['passed'], 'scannedFiles' => result['scannedFiles'], 'failureCount' => result['failureCount'])
exit(result['passed'] ? 0 : 1)
end
