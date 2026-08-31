#!/usr/bin/env ruby
require 'json'
root=File.expand_path('..',__dir__)
ddl=File.read(File.join(root,'database/migrations/V20260823_15__canonical_column_types.sql'))
gate=File.read(File.join(root,'server/gameServer/src/core/server/SchemaTypeReadiness.java'))
jdbc=File.read(File.join(root,'server/Gateway/src/main/java/com/aoo/bcg/gateway/JdbcConnectionGenerationStore.java'))
readiness=File.read(File.join(root,'server/gameServer/src/core/server/ProductionSchemaReadiness.java'))
checks={'canonical_numeric_identity'=>ddl.scan(/room_id BIGINT UNSIGNED/).size>=5,'canonical_charset'=>ddl.scan(/utf8mb4_0900_ai_ci/).size>=10,'jdbc_numeric_binding'=>jdbc.include?('setLong(2, numericRoomId)'),'metadata_type_gate'=>gate.include?('DATA_TYPE,COLUMN_TYPE'),'metadata_collation_gate'=>gate.include?('utf8mb4_0900_ai_ci'),'startup_integrated'=>readiness.include?('SchemaTypeReadiness.verify')}
out={'task'=>'CON06','passed'=>checks.values.all?,'checks'=>checks};path=File.join(root,'work/audit/canonical-column-types.json');File.write(path,JSON.pretty_generate(out)+"\n");abort JSON.generate(out) unless out['passed'];puts JSON.generate(out)
