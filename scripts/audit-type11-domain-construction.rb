#!/usr/bin/env ruby
require 'json'
root=File.expand_path('..',__dir__)
room=File.read(File.join(root,'server/GameCommon/src/main/java/com/aoo/bcg/common/room/AuthoritativeRoom.java'))
profile=File.read(File.join(root,'server/ConfigCenter/src/main/java/com/aoo/bcg/config/PublishedGameProfile.java'))
provider=File.read(File.join(root,'server/GameSPI/src/main/java/com/aoo/bcg/gamespi/GameProvider.java'))
checks={'legacy_room_uses_domain_definition'=>room.include?('AuthoritativeRoom(RoomDefinition definition)'),'catalog_scope_invariants'=>profile.include?('province profiles require provinces'),'catalog_snapshot_identity'=>profile.include?('profile identity must match'),'typed_restore_boundary'=>provider.include?('restoreAuthoritativeSession(StatePayload state)'),'typed_default_rules'=>provider.include?('defaultConfigurationPayload()')}
out={'task'=>'TYPE11','passed'=>checks.values.all?,'checks'=>checks};path=File.join(root,'work/audit/domain-construction-invariants.json');File.write(path,JSON.pretty_generate(out)+"\n");abort JSON.generate(out) unless out['passed'];puts JSON.generate(out)
