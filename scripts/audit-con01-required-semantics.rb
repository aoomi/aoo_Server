#!/usr/bin/env ruby
require 'json'
root=File.expand_path('..',__dir__)
gate=File.read(File.join(root,'server/gameServer/src/core/server/ProductionSchemaReadiness.java'))
migration=File.read(File.join(root,'database/migrations/V20260823_11__required_business_semantics.sql'))
checks={'metadata_nullability_gate'=>gate.include?('(nullable)'),'placeholder_default_gate'=>gate.include?('(placeholder default)'),'critical_tables_hardened'=>%w[aoo_business_idempotency aoo_room_snapshot aoo_room_lease aoo_settlement aoo_ledger aoo_currency_balance aoo_club_member].all?{|t|migration.include?(t)},'no_empty_default_introduced'=>!migration.match?(/DEFAULT\s+(''|0)\b/i)}
out={'task'=>'CON01','passed'=>checks.values.all?,'checks'=>checks};path=File.join(root,'work/audit/required-column-semantics.json');File.write(path,JSON.pretty_generate(out)+"\n");abort JSON.generate(out) unless out['passed'];puts JSON.generate(out)
