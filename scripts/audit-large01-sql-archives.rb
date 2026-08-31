#!/usr/bin/env ruby
# frozen_string_literal: true
require 'digest';require 'json';require 'time'
root=File.expand_path('..',__dir__);directory=File.join(root,'database/original')
files=Dir.glob(File.join(directory,'*.sql')).sort.map do|path|
 {path:path.delete_prefix(root+'/'),bytes:File.size(path),sha256:Digest::SHA256.file(path).hexdigest,readOnly:(File.stat(path).mode&0o222).zero?}
end
active=[File.join(root,'pom.xml'),File.join(root,'tools/apply_migrations.sh'),File.join(root,'tools/verify_fresh_migrations.sh')]+Dir.glob(File.join(root,'server/**/pom.xml'))+Dir.glob(File.join(root,'server/**/src/main/**/*')).select{|path|File.file?(path)}
reachable=active.select{|path|File.read(path,encoding:'UTF-8',invalid: :replace,undef: :replace).match?(%r{database/original|(?:clark_(?:log|game)_qh|db_zle(?:_qh)?)\.sql})}.map{|path|path.delete_prefix(root+'/')}
release=File.read(File.join(root,'.releaseignore'));migrations=Dir.glob(File.join(root,'database/migrations/*.sql')).sort
checks={allReadOnly:files.all?{|file|file[:readOnly]},releaseExcluded:release.include?('database/original/'),runtimeAndBuildUnreachable:reachable.empty?,incrementalAuthority:migrations.length>0&&File.read(File.join(root,'tools/apply_migrations.sh')).include?('database/migrations'),archiveManifestComplete:files.length==4}
report={schemaVersion:1,generatedAt:Time.now.utc.iso8601,task:'LARGE01',passed:checks.values.all?,policy:{authority:'database/migrations',archive:'database/original immutable offline reference',import:'isolated staging schema only',release:'excluded'},files:files,totalBytes:files.sum{|file|file[:bytes]},migrationCount:migrations.length,reachableFromActiveRuntime:reachable,checks:checks}
File.write(File.join(root,'docs/generated/large01-sql-archives.json'),JSON.pretty_generate(report)+"\n");puts "LARGE01 #{report[:passed] ? 'PASS':'FAIL'} archives=#{files.length} bytes=#{report[:totalBytes]}";exit(report[:passed] ? 0:1)
