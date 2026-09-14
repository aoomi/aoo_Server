#!/usr/bin/env ruby
require 'json'
root=File.expand_path('..',__dir__)
ddl=File.read(File.join(root,'database/migrations/V20260823_16__utc_millisecond_time.sql'))
gate=File.read(File.join(root,'server/gameServer/src/core/server/TemporalSchemaReadiness.java'))
source=File.read(File.join(root,'server/GameCommon/src/main/java/com/aoo/bcg/common/persistence/DriverManagerDataSource.java'))
readiness=File.read(File.join(root,'server/gameServer/src/core/server/ProductionSchemaReadiness.java'))
checks={'timestamp_normalized'=>ddl.include?('MODIFY updated_at DATETIME(3)'),'utc_connection_session'=>source.include?("SET time_zone='+00:00'"),'full_temporal_metadata_scan'=>gate.include?("DATA_TYPE IN ('datetime','timestamp')"),'millisecond_precision_gate'=>gate.include?('rows.getInt(4)!=3'),'ownership_split'=>gate.include?('DATABASE_OWNED')&&gate.include?('APPLICATION_OWNED'),'startup_integrated'=>readiness.include?('TemporalSchemaReadiness.verify')}
out={'task'=>'CON07','passed'=>checks.values.all?,'checks'=>checks};path=File.join(root,'work/audit/temporal-authority.json');File.write(path,JSON.pretty_generate(out)+"\n");abort JSON.generate(out) unless out['passed'];puts JSON.generate(out)
