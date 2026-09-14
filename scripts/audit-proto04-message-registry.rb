require 'json'
require 'fileutils'

root = File.expand_path('..', __dir__)
source = JSON.parse(File.read(File.join(root, 'protocol/aoo-protocol-v2.json')))
messages = source.fetch('messages')
ids = messages.map { |message| message.fetch('msgId') }
reserved = source.fetch('reservedMessageIds')
generated = File.read(File.join(root, 'server/GameSPI/src/main/java/com/aoo/bcg/gamespi/protocol/GeneratedProtocolIds.java'))
registry = File.read(File.join(root, 'server/LegacyCommon/src/com/ddm/server/protocol/v2/ProtocolRegistry.java'))
checks = {
  registry_versioned: source.fetch('messageRegistryVersion').is_a?(Integer) && source.fetch('messageRegistryVersion') > 0,
  active_unique: ids.uniq.length == ids.length,
  active_reserved_disjoint: (ids & reserved).empty?,
  all_ids_generated: ids.all? { |id| generated.include?(%Q{"#{id}"}) },
  runtime_rejects_undeclared: registry.include?('GeneratedProtocolIds.DEFINITIONS.containsKey(msgId)'),
  deletion_reservation_gate: File.read(File.join(root, 'tools/protocol/check.mjs')).include?('removed message must remain reserved')
}
checks[:passed] = checks.values.all?
out = File.join(root, 'docs/generated/proto04-message-registry.json')
FileUtils.mkdir_p(File.dirname(out))
File.write(out, JSON.pretty_generate(checks.merge(active_count: ids.length, reserved_count: reserved.length)) + "\n")
abort('PROTO04 message registry audit failed') unless checks[:passed]
