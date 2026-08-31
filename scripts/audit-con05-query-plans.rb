#!/usr/bin/env ruby
require 'json'
root=File.expand_path('..',__dir__)
gate=File.read(File.join(root,'server/gameServer/src/core/server/QueryPlanReadiness.java'))
ddl=File.read(File.join(root,'database/migrations/V20260823_14__query_shape_indexes.sql'))
readiness=File.read(File.join(root,'server/gameServer/src/core/server/ProductionSchemaReadiness.java'))
checks={'real_explain_json'=>gate.include?('EXPLAIN FORMAT=JSON'),'where_order_shapes'=>gate.scan(/new Plan\(/).size>=7,'equality_range_order_indexes'=>%w[idx_idempotency_result idx_room_event_view_sequence idx_snapshot_recoverable idx_template_active_page idx_play_active_region].all?{|key|ddl.include?(key)},'reject_full_scan'=>gate.include?('access_type')&&gate.include?('ALL'),'reject_filesort'=>gate.include?('using_filesort'),'startup_gate'=>readiness.include?('QueryPlanReadiness.verify')}
out={'task'=>'CON05','passed'=>checks.values.all?,'checks'=>checks};path=File.join(root,'work/audit/query-plan-readiness.json');File.write(path,JSON.pretty_generate(out)+"\n");abort JSON.generate(out) unless out['passed'];puts JSON.generate(out)
