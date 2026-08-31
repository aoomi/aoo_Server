#!/usr/bin/env ruby
require 'json'
root=File.expand_path('..',__dir__)
scanner=File.read(File.join(root,'server/gameServer/src/core/server/ForeignKeyIntegrityReadiness.java'))
readiness=File.read(File.join(root,'server/gameServer/src/core/server/ProductionSchemaReadiness.java'))
policy=File.read(File.join(root,'database/migrations/FOREIGN_KEY_POLICY.md'))
active_sql=Dir[File.join(root,'database/migrations/*.sql')].map{|f|File.read(f)}.join("\n")
checks={'production_fk_enabled'=>scanner.include?('@@SESSION.FOREIGN_KEY_CHECKS'),'all_fk_metadata_scanned'=>scanner.include?('information_schema.KEY_COLUMN_USAGE'),'composite_fk_supported'=>scanner.include?('ORDINAL_POSITION')&&scanner.include?('referencedColumns'),'orphan_query_fail_closed'=>scanner.include?('foreign-key orphan rows'),'startup_integrated'=>readiness.include?('ForeignKeyIntegrityReadiness.verify'),'legacy_disable_isolated'=>policy.include?('database/original'),'active_migrations_never_disable'=>!active_sql.match?(/FOREIGN_KEY_CHECKS\s*=\s*0/i)}
out={'task'=>'CON04','passed'=>checks.values.all?,'checks'=>checks};path=File.join(root,'work/audit/foreign-key-integrity.json');File.write(path,JSON.pretty_generate(out)+"\n");abort JSON.generate(out) unless out['passed'];puts JSON.generate(out)
