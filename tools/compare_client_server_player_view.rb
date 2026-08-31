#!/usr/bin/env ruby
# frozen_string_literal: true

require 'json'
require 'digest'
require 'time'
require 'fileutils'

root = File.expand_path('..', __dir__)
output = File.join(root, 'work/audit/client-server-player-view-diff.json')
client_path = ENV['AOO_CLIENT_PUBLIC_VIEW']
server_path = ENV['AOO_SERVER_PLAYER_VIEW']

canonical = lambda do |value|
  case value
  when Hash then value.keys.sort.to_h { |key| [key, canonical.call(value[key])] }
  when Array then value.map { |item| canonical.call(item) }
  else value
  end
end

client_view = client_path && File.file?(client_path) ? canonical.call(JSON.parse(File.read(client_path, encoding: 'UTF-8'))) : nil
server_view = server_path && File.file?(server_path) ? canonical.call(JSON.parse(File.read(server_path, encoding: 'UTF-8'))) : nil
client_json = client_view && JSON.generate(client_view)
server_json = server_view && JSON.generate(server_view)
summary = {
  clientCapturePresent: !client_view.nil?, serverCapturePresent: !server_view.nil?,
  clientDigest: client_json && Digest::SHA256.hexdigest(client_json),
  serverDigest: server_json && Digest::SHA256.hexdigest(server_json),
  equal: !client_json.nil? && client_json == server_json,
  passed: !client_json.nil? && client_json == server_json
}
report = {schemaVersion: 1, generatedAt: Time.now.utc.iso8601,
          invariant: 'At each checkpoint the client public mirror equals the server DTO for that authenticated player.',
          summary: summary, clientCapture: client_path, serverCapture: server_path,
          captureContract: %w[roomId playVersion eventSeq authenticatedViewerId checkpointId publicView]}
FileUtils.mkdir_p(File.dirname(output))
File.write(output, JSON.pretty_generate(report) + "\n", mode: 'w:UTF-8')
puts JSON.generate(summary)
exit(summary[:passed] ? 0 : 2) if ENV['AOO_ENFORCE_PLAYER_VIEW_DIFF_GATE'] == '1'
