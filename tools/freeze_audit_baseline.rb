#!/usr/bin/env ruby
require 'digest';require 'json';require 'pathname';require 'time'
server_root=File.expand_path('..',__dir__);aoo_root=File.expand_path('..',server_root)
ROOTS=[File.join(server_root,'modules'),File.join(server_root,'server'),File.join(server_root,'tools'),File.join(aoo_root,'Client/assets'),File.join(aoo_root,'Admin/src')].freeze
EXCLUDE=%w[.git node_modules target build library temp work].freeze
EXT=%w[.java .ts .vue .js .mjs .json .xml .sql .rb .sh .properties .yml .yaml .conf .prefab .scene .meta].freeze
entries=[]
ROOTS.each do |root|
 next unless File.directory?(root)
 base=Pathname(root); stack=[base]
 until stack.empty?
  dir=stack.pop
  dir.children.sort_by(&:to_s).each do |path|
   if path.directory?
    stack << path unless EXCLUDE.include?(path.basename.to_s)
   elsif path.file? && EXT.include?(path.extname.downcase)
    stat=path.stat
    entries << {'path'=>path.to_s,'root'=>root,'relativePath'=>path.relative_path_from(base).to_s,'size'=>stat.size,'sha256'=>Digest::SHA256.file(path).hexdigest}
   end
  rescue Errno::ENOENT,Errno::EACCES
   next
  end
 end
end
entries.sort_by!{|e|e['path']}
aggregate=Digest::SHA256.hexdigest(entries.map{|e|"#{e['path']}\0#{e['size']}\0#{e['sha256']}"}.join("\n"))
report={'schemaVersion'=>1,'generatedAt'=>Time.now.utc.iso8601,'roots'=>ROOTS,'excludes'=>EXCLUDE,'extensions'=>EXT,'fileCount'=>entries.length,'aggregateSha256'=>aggregate,'files'=>entries}
out=ARGV.fetch(0,'work/audit/audit-source-baseline.json');File.write(out,JSON.generate(report)+"\n")
