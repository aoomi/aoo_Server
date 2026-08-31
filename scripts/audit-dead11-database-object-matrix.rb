require 'json';require 'fileutils';require 'set'
root=File.expand_path('..',__dir__);schemas=Dir.glob(File.join(root,'database/migrations/*.sql'));schema_text=schemas.map{|path|File.binread(path).force_encoding('UTF-8').scrub}.join("\n");tables=schema_text.scan(/\bCREATE\s+TABLE\s+(?:IF\s+NOT\s+EXISTS\s+)?[`"]?([A-Za-z_][A-Za-z0-9_]*)/i).flatten.map(&:downcase).uniq.sort
sources=Dir.glob(File.join(root,'server/**/*.java')).reject{|path|path.include?('/target/')||path.include?('/build/')||path.include?('/test/')};reads=Hash.new{|h,k|h[k]=Set.new};writes=Hash.new{|h,k|h[k]=Set.new}
sources.each do |path|
 body=File.binread(path).force_encoding('UTF-8').scrub
 body.scan(/\b(?:FROM|JOIN)\s+[`"]?([A-Za-z_][A-Za-z0-9_]*)/i){|name|reads[name[0].downcase]<<path.sub(root+'/','')}
 body.scan(/\b(?:INSERT\s+(?:IGNORE\s+)?INTO|UPDATE|DELETE\s+FROM|REPLACE\s+INTO)\s+[`"]?([A-Za-z_][A-Za-z0-9_]*)/i){|name|writes[name[0].downcase]<<path.sub(root+'/','')}
end
rows=tables.map{|table|{table:table,readPaths:reads[table].to_a.sort,writePaths:writes[table].to_a.sort,status:(reads[table].empty?&&writes[table].empty? ? 'review-unused':'referenced')}};unknown=(reads.keys+writes.keys).uniq.reject{|table|tables.include?(table)}.sort
out=File.join(root,'work/audit/dead11-database-object-matrix.json');FileUtils.mkdir_p(File.dirname(out));File.write(out,JSON.pretty_generate({task:'DEAD11',status:'passed-with-review',schemaFiles:schemas.map{|path|path.sub(root+'/','')},tableCount:tables.size,referencedCount:rows.count{|row|row[:status]=='referenced'},reviewUnusedCount:rows.count{|row|row[:status]=='review-unused'},unknownSqlReferences:unknown,objects:rows,limitations:['Dynamic ORM table names and external reporting access require owner confirmation before deletion.']})+"\n")
ledger=File.join(root,'docs/数据库对象删除复核登记.md');File.write(ledger,"# 数据库对象删除复核登记\n\n以下对象仅为静态 SQL 零引用候选。必须核对动态 ORM、存储过程、定时任务、管理后台、报表和外部消费者后才可下线。\n\n| 对象 | 应用读 | 应用写 | 外部/动态证明 | 结论 |\n|---|---|---|---|---|\n"+rows.select{|row|row[:status]=='review-unused'}.map{|row|"| `#{row[:table]}` | 0 | 0 | 待补 | 待人工核验 |"}.join("\n")+"\n")
