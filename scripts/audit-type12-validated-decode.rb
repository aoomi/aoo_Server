#!/usr/bin/env ruby
require 'json'
root=File.expand_path('..',__dir__)
snapshot=File.read(File.join(root,'server/GameCommon/src/main/java/com/aoo/bcg/common/recovery/JdbcRoomSnapshotStore.java'))
idem=File.read(File.join(root,'server/GameCommon/src/main/java/com/aoo/bcg/common/idempotency/JdbcIdempotencyStore.java'))
cache=File.read(File.join(root,'server/GameCommon/src/main/java/com/aoo/bcg/common/cache/VersionedCacheCodec.java'))
checks={'central_decode_boundary'=>File.exist?(File.join(root,'server/GameCommon/src/main/java/com/aoo/bcg/common/serialization/DomainJsonDecoder.java')),'snapshot_schema_validation'=>snapshot.include?('DomainJsonDecoder::requireDocument'),'idempotency_domain_validation'=>idem.include?('decoder.decode'),'cache_schema_version_gate'=>cache.include?('unsupported cache schemaVersion'),'cache_null_rejected'=>cache.include?('cache decoder returned null')}
out={'task'=>'TYPE12','passed'=>checks.values.all?,'checks'=>checks};path=File.join(root,'work/audit/validated-json-decode.json');File.write(path,JSON.pretty_generate(out)+"\n");abort JSON.generate(out) unless out['passed'];puts JSON.generate(out)
