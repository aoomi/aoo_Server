#!/usr/bin/env ruby
# frozen_string_literal: true

require 'json'
require 'socket'
require 'time'
require 'fileutils'

root = File.expand_path('..', __dir__)
output = File.join(root, 'work/audit/legacy-endpoint-negative-probe.json')
host = ENV.fetch('AOO_PROBE_HOST', '127.0.0.1')
ports = ENV.fetch('AOO_LEGACY_PORTS', '904,8802,9998').split(',').map(&:to_i).uniq
production = ENV['AOO_PROBE_ENV'] == 'production'

probes = ports.map do |port|
  reachable = false
  error = nil
  begin
    Socket.tcp(host, port, connect_timeout: 0.5) { |socket| reachable = !socket.closed? }
  rescue StandardError => e
    error = e.class.name
  end
  {target: "#{host}:#{port}", reachable: reachable, errorClass: error}
end

summary = {
  targets: probes.size,
  reachable: probes.count { |row| row[:reachable] },
  unreachable: probes.count { |row| !row[:reachable] },
  productionEnvironment: production,
  passed: production && probes.none? { |row| row[:reachable] }
}
report = {
  schemaVersion: 1,
  generatedAt: Time.now.utc.iso8601,
  environment: production ? 'production' : 'local-non-authoritative',
  invariant: 'Every retired HTTP, WebSocket and TCP target rejects connections and legacy message shapes.',
  summary: summary,
  tcpProbes: probes,
  pendingProductionProbes: ['/JavaServerPack', 'legacy WebSocket handshake', 'legacy numeric message id', 'legacyEvent alias']
}
FileUtils.mkdir_p(File.dirname(output))
File.write(output, JSON.pretty_generate(report) + "\n", mode: 'w:UTF-8')
puts JSON.generate(summary)
exit(summary[:passed] ? 0 : 2) if ENV['AOO_ENFORCE_LEGACY_ENDPOINT_GATE'] == '1'
