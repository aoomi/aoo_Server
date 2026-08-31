#!/usr/bin/env ruby
require 'json'
root = File.expand_path('..', __dir__)
checks = {
  'command_payload_typed' => File.read(File.join(root, 'server/GameSPI/src/main/java/com/aoo/bcg/gamespi/CommandPayload.java')).include?('extends TypedDocument'),
  'rule_snapshot_typed' => File.read(File.join(root, 'server/GameCommon/src/main/java/com/aoo/bcg/common/config/RoomRuleSnapshot.java')).include?('RulePayload immutableRules'),
  'state_snapshot_typed' => File.read(File.join(root, 'server/GameCommon/src/main/java/com/aoo/bcg/common/recovery/RoomSnapshot.java')).include?('StatePayload authoritativeState'),
  'creation_rules_typed' => File.read(File.join(root, 'server/GameSPI/src/main/java/com/aoo/bcg/gamespi/RoomCreationContext.java')).include?('RulePayload immutableRules'),
  'typed_keys_available' => File.exist?(File.join(root, 'server/GameSPI/src/main/java/com/aoo/bcg/gamespi/FieldKey.java'))
}
out = {'task'=>'TYPE09','passed'=>checks.values.all?,'checks'=>checks}
path = File.join(root, 'work/audit/typed-document-boundaries.json')
Dir.mkdir(File.dirname(path)) unless Dir.exist?(File.dirname(path))
File.write(path, JSON.pretty_generate(out) + "\n")
abort JSON.generate(out) unless out['passed']
puts JSON.generate(out)
