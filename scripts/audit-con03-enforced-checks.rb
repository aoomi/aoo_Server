#!/usr/bin/env ruby
require 'json'
root=File.expand_path('..',__dir__)
ddl=File.read(File.join(root,'database/migrations/V20260823_13__enforced_domain_checks.sql'))
gate=File.read(File.join(root,'server/gameServer/src/core/server/ProductionSchemaReadiness.java'))
domains=%w[idempotency room_event snapshot lease settlement ledger club_member play_variant room_template]
checks={'all_domain_checks'=>domains.all?{|name|ddl.include?("chk_#{name}")},'range_checks'=>ddl.include?('round_no >= 0')&&ddl.include?('template_version > 0'),'status_checks'=>ddl.scan(/status IN \(/).size>=4,'visibility_consistency'=>ddl.include?("visibility='PLAYER_PRIVATE' AND owner_player_id>0"),'mysql_enforcement_version_gate'=>gate.include?('patch<16'),'metadata_constraint_gate'=>gate.include?("CONSTRAINT_TYPE='CHECK'")}
out={'task'=>'CON03','passed'=>checks.values.all?,'checks'=>checks};path=File.join(root,'work/audit/enforced-check-constraints.json');File.write(path,JSON.pretty_generate(out)+"\n");abort JSON.generate(out) unless out['passed'];puts JSON.generate(out)
