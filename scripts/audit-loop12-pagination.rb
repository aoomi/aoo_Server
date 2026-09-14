#!/usr/bin/env ruby
require 'json';require 'fileutils'
root=File.expand_path('..',__dir__); files=Dir.glob(File.join(root,'server/**/*.{java,sql}')).reject{|f|f.include?('/build/')||f.include?('/target/')}
offset=files.select{|f|File.read(f).match?(/\bOFFSET\s+(?:\?|\d+)/i)}
guard=File.read(File.join(root,'server/GameCommon/src/main/java/com/aoo/bcg/common/paging/CursorTraversalGuard.java'))
club=File.read(File.join(root,'server/GameCommon/src/main/java/com/aoo/bcg/common/club/ClubNotificationService.java'))
checks={'no_offset_pagination'=>offset.empty?,'page_budget'=>guard.include?('maximumPages'),'duplicate_cursor'=>guard.include?('seen.add(nextCursor)'),
 'empty_page_termination'=>guard.include?('itemCount==0'),'production_integration'=>club.include?('CursorTraversalGuard')}
r={'task'=>'LOOP12','passed'=>checks.values.all?,'checks'=>checks,'offsetFiles'=>offset};out=File.join(root,'work/audit/loop12-pagination.json');FileUtils.mkdir_p(File.dirname(out));File.write(out,JSON.pretty_generate(r)+"\n");puts JSON.generate(r);exit(r['passed'] ? 0 : 1)
