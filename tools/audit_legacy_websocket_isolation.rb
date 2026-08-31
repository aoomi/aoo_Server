#!/usr/bin/env ruby
# frozen_string_literal: true

require 'json'
require 'time'
require 'fileutils'

root = File.expand_path('..', __dir__)
output = File.join(root, 'work/audit/legacy-websocket-isolation.json')
extensions = %w[.java .kt .xml .yml .yaml .properties].freeze
excluded = %w[/target/ /build/ /.git/].freeze

files = Dir.glob(File.join(root, 'server/**/*'), File::FNM_DOTMATCH).select do |path|
  File.file?(path) && extensions.include?(File.extname(path)) && excluded.none? { |part| path.include?(part) }
end

categories = {
  legacyHandler: /(?:extends\s+(?:BaseHandler|PlayerHandler|RequestHandler)|registerRequestHandlers)/,
  legacyTransport: /(?:BaseIoHandler|ServerMessageDispatcher|ClientHandlerDispatcher|TWebSocketServerProtocolHandler)/,
  legacyAlias: /(?:legacyEvent|legacyHandlers|compatibility requests|ProtocolV2Bridge)/,
  numericMessage: /\bC\d{3,}[A-Za-z0-9_]*\b/
}.freeze

findings = []
files.each do |path|
  relative = path.delete_prefix(root + '/')
  File.foreach(path, encoding: 'UTF-8', invalid: :replace, undef: :replace).with_index(1) do |line, number|
    line = line.encode('UTF-8', invalid: :replace, undef: :replace, replace: '')
    categories.each do |kind, pattern|
      next unless line.match?(pattern)
      findings << {kind: kind, path: relative, line: number, text: line.strip[0, 240]}
    end
  end
end

counts = categories.keys.to_h { |kind| [kind, findings.count { |item| item[:kind] == kind }] }
report = {
  schemaVersion: 1,
  generatedAt: Time.now.utc.iso8601,
  invariant: 'Production WebSocket accepts only the unified protocol; legacy handlers, numeric message ids and compatibility aliases are unreachable.',
  summary: counts.merge(scannedFiles: files.size, violations: findings.size, passed: findings.empty?),
  findings: findings
}
FileUtils.mkdir_p(File.dirname(output)) unless Dir.exist?(File.dirname(output))
File.write(output, JSON.pretty_generate(report) + "\n", mode: 'w:UTF-8')
puts JSON.generate(report[:summary])
exit(findings.empty? ? 0 : 2) if ENV['AOO_ENFORCE_LEGACY_WS_GATE'] == '1'
