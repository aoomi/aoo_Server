require 'json'
require 'fileutils'

root = File.expand_path('..', __dir__)
source = JSON.parse(File.read(File.join(root, 'protocol/aoo-protocol-v2.json')))
ids = source.fetch('messages').map { |message| message.fetch('msgId') }
java_path = File.join(root, 'server/GameSPI/src/main/java/com/aoo/bcg/gamespi/protocol/GeneratedProtocolIds.java')
ts_path = File.expand_path('../Client/assets/Common/Code/Runtime/network/GeneratedProtocolIds.ts', root)
java = File.read(java_path)
ts = File.read(ts_path)
envelopes = {
  gateway: File.read(File.join(root, 'server/Gateway/src/main/java/com/aoo/bcg/gateway/WebSocketFrame.java')),
  legacy: File.read(File.join(root, 'server/LegacyCommon/src/com/ddm/server/protocol/v2/ProtocolEnvelope.java')),
  client_http: File.read(File.expand_path('../Client/assets/Common/Code/Runtime/network/ProtocolHttpClient.ts', root))
}
fields = %w[protocolVersion msgId kind requestId seq traceId timestamp body]
checks = {
  java_shapes_generated: java.include?('record Shape') && java.include?('Shape request, Shape response'),
  typescript_bodies_generated: ts.include?('interface ProtocolRequestBodies') && ts.include?('interface ProtocolResponseBodies'),
  all_java_ids_generated: ids.all? { |id| java.include?(%Q{"#{id}"}) },
  all_typescript_ids_generated: ids.all? { |id| ts.include?(%Q{'#{id}'}) },
  canonical_envelope_fields: envelopes.transform_values { |text| fields.all? { |field| text.include?(field) } },
  single_active_java_definition: Dir.glob(File.join(root, 'server/**/GeneratedProtocolIds.java')).map { |path| path.delete_prefix(root + '/') }
}
checks[:passed] = checks[:java_shapes_generated] && checks[:typescript_bodies_generated] &&
  checks[:all_java_ids_generated] && checks[:all_typescript_ids_generated] &&
  checks[:canonical_envelope_fields].values.all? && checks[:single_active_java_definition] == ['server/GameSPI/src/main/java/com/aoo/bcg/gamespi/protocol/GeneratedProtocolIds.java']
out = File.join(root, 'docs/generated/proto03-dto-drift.json')
FileUtils.mkdir_p(File.dirname(out))
File.write(out, JSON.pretty_generate(checks.merge(message_count: ids.length)) + "\n")
abort('PROTO03 DTO drift audit failed') unless checks[:passed]
